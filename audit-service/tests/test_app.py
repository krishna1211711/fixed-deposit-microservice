import os
import unittest
from unittest.mock import patch, Mock

os.environ["AUDIT_DISABLE_CONSUMER"] = "true"
import app


class AuditContractTest(unittest.TestCase):
    def test_health_route_exists(self):
        self.assertIn("/health", {rule.rule for rule in app.app.url_map.iter_rules()})

    def test_events_route_exists(self):
        self.assertIn("/events", {rule.rule for rule in app.app.url_map.iter_rules()})

    @patch("app.db_connection")
    def test_legacy_event_is_normalized_instead_of_poisoning_partition(self, db_connection):
        connection = Mock()
        cursor = Mock()
        cursor.fetchone.return_value = None
        connection.cursor.return_value = cursor
        db_connection.return_value = connection

        event = {"eventId": "legacy-id", "eventType": "FD_OPENED", "fdAccountNo": "0010000001"}
        app.process_event(event)

        self.assertEqual("legacy-0", event["schemaVersion"])
        self.assertEqual("FD_ACCOUNT", event["aggregateType"])
        connection.commit.assert_called_once()


if __name__ == "__main__":
    unittest.main()
