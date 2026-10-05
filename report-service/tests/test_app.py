import os
import unittest

os.environ["REPORT_DISABLE_CONSUMER"] = "true"

import app


class ReportReadModelTest(unittest.TestCase):
    def test_lifecycle_event_mapping(self):
        self.assertEqual("ACTIVE", app.lifecycle_status("FD_OPENED"))
        self.assertEqual("CLOSED", app.lifecycle_status("FD_MATURED"))
        self.assertEqual("PREMATURE_CLOSED", app.lifecycle_status("FD_PREMATURELY_CLOSED"))
        self.assertEqual("RENEWED", app.lifecycle_status("FD_RENEWED"))

    def test_unknown_non_terminal_event_keeps_account_active(self):
        self.assertEqual("ACTIVE", app.lifecycle_status("INTEREST_ACCRUED"))


if __name__ == "__main__":
    unittest.main()
