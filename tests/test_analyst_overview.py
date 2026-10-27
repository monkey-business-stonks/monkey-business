import unittest
from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

from app.queries import OrderFilters, fetch_analyst_overview


class AnalystOverviewTests(unittest.TestCase):
    def fetch_options(self, filters, rows=None):
        conn = MagicMock()
        cursor = conn.cursor.return_value.__enter__.return_value
        option_rows = rows if rows is not None else [
            {"ticker": "AAPL", "accounttype": "ISA"},
            {"ticker": "AAPL", "accounttype": "GIA"},
            {"ticker": "MSFT", "accounttype": "ISA"},
            {"ticker": "TSLA", "accounttype": "SIPP"},
        ]
        cursor.fetchall.side_effect = [option_rows, []]
        with patch("app.queries._fetch_kpis", return_value={}), patch(
            "app.queries._fetch_account_breakdown", return_value=[]
        ):
            result = fetch_analyst_overview(conn, filters)
        return result, cursor.execute.call_args_list[0]

    def test_client_segment_limits_instruments(self):
        result, _ = self.fetch_options(OrderFilters(account_type="ISA"))
        self.assertEqual(result["available_instruments"], ["AAPL", "MSFT"])
        self.assertEqual(result["available_account_types"], ["GIA", "ISA", "SIPP"])

    def test_instrument_limits_client_segments(self):
        result, _ = self.fetch_options(OrderFilters(instrument="AAPL"))
        self.assertEqual(result["available_account_types"], ["GIA", "ISA"])
        self.assertEqual(result["available_instruments"], ["AAPL", "MSFT", "TSLA"])

    def test_both_filters_keep_compatible_alternatives_available(self):
        result, _ = self.fetch_options(OrderFilters(instrument="AAPL", account_type="ISA"))
        self.assertEqual(result["available_instruments"], ["AAPL", "MSFT"])
        self.assertEqual(result["available_account_types"], ["GIA", "ISA"])

    def test_clearing_filters_restores_all_options(self):
        result, _ = self.fetch_options(OrderFilters())
        self.assertEqual(result["available_instruments"], ["AAPL", "MSFT", "TSLA"])
        self.assertEqual(result["available_account_types"], ["GIA", "ISA", "SIPP"])

    def test_options_respect_reporting_period_without_filtering_out_alternatives(self):
        start = datetime(2026, 10, 1, tzinfo=timezone.utc)
        end = datetime(2026, 10, 27, tzinfo=timezone.utc)
        _, options_call = self.fetch_options(OrderFilters(
            from_ts=start, to_ts=end, instrument="AAPL", account_type="ISA"
        ))
        self.assertEqual(options_call.args[1], [start, end])
        self.assertNotIn("o.ticker = %s", options_call.args[0])
        self.assertNotIn("a.accounttype = %s", options_call.args[0])

    def test_unknown_selection_and_empty_period_have_no_matching_options(self):
        result, _ = self.fetch_options(OrderFilters(instrument="UNKNOWN", account_type="UNKNOWN"))
        self.assertEqual(result["available_instruments"], [])
        self.assertEqual(result["available_account_types"], [])
        result, _ = self.fetch_options(OrderFilters(), rows=[])
        self.assertEqual(result["available_instruments"], [])
        self.assertEqual(result["available_account_types"], [])


if __name__ == "__main__":
    unittest.main()