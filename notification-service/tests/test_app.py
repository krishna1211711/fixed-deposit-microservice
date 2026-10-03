import os
import unittest
from unittest.mock import Mock, call, patch

os.environ["NOTIFICATION_DISABLE_CONSUMER"] = "true"

import app


class NotificationProcessingTest(unittest.TestCase):
    def setUp(self):
        self.event = {
            "eventId": "f52b3f6d-5d8c-4de4-aa82-16782bd915db",
            "eventType": "FD_OPENED",
            "aggregateId": "0010000001",
            "customerId": "CUST001",
            "subject": "FD opened",
            "messageBody": "Your FD was opened.",
        }
        app.state["deadLettered"] = 0

    @patch("app.record_attempt")
    @patch("app.send_email")
    @patch("app.already_consumed", return_value=False)
    def test_success_records_sent_and_inbox(self, _consumed, send_email, record_attempt):
        app.process_event(self.event, Mock(), "fd.lifecycle.v1")

        send_email.assert_called_once_with(self.event)
        record_attempt.assert_called_once_with(
            self.event, "SENT", 1, mark_consumed=True
        )

    @patch("app.record_attempt")
    @patch("app.send_email")
    @patch("app.already_consumed", return_value=True)
    def test_inbox_deduplicates_replayed_event(self, _consumed, send_email, record_attempt):
        app.process_event(self.event, Mock(), "fd.lifecycle.v1")

        send_email.assert_not_called()
        record_attempt.assert_not_called()

    @patch("app.time.sleep")
    @patch("app.publish_dead_letter")
    @patch("app.record_attempt")
    @patch("app.send_email", side_effect=RuntimeError("SMTP unavailable"))
    @patch("app.already_consumed", return_value=False)
    def test_exhausted_retries_publish_dlq_then_mark_consumed(
        self, _consumed, send_email, record_attempt, publish_dlq, _sleep
    ):
        producer = Mock()
        with patch.dict(os.environ, {"NOTIFICATION_MAX_ATTEMPTS": "3"}):
            app.process_event(self.event, producer, "fd.lifecycle.v1")

        self.assertEqual(3, send_email.call_count)
        self.assertEqual("RETRYING", record_attempt.call_args_list[0].args[1])
        self.assertEqual("RETRYING", record_attempt.call_args_list[1].args[1])
        self.assertEqual("FAILED", record_attempt.call_args_list[2].args[1])
        publish_dlq.assert_called_once()
        self.assertEqual(
            call(self.event, "DEAD_LETTER", 3, unittest.mock.ANY, mark_consumed=True),
            record_attempt.call_args_list[3],
        )
        self.assertEqual(1, app.state["deadLettered"])

    def test_invalid_event_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Missing required event fields"):
            app.process_event({"eventId": "only-an-id"}, Mock(), "fd.lifecycle.v1")


if __name__ == "__main__":
    unittest.main()
