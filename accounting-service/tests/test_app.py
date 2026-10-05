import os
import unittest

os.environ["ACCOUNTING_DISABLE_CONSUMER"] = "true"
import app


class AccountingContractTest(unittest.TestCase):
    def test_non_financial_events_are_ignored_without_database_access(self):
        self.assertIsNone(app.process_event({"eventType": "INTEREST_ACCRUED"}))

    def test_health_route_exists(self):
        self.assertIn("/health", {rule.rule for rule in app.app.url_map.iter_rules()})

    def test_entries_route_exists(self):
        self.assertIn("/entries", {rule.rule for rule in app.app.url_map.iter_rules()})


if __name__ == "__main__":
    unittest.main()
