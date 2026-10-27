import unittest
from unittest.mock import MagicMock, patch

from app.main import operations_overview
from app.queries import OrderFilters, fetch_operations_overview


class OperationsOverviewTests(unittest.TestCase):
    def test_execution_prices_and_pagination_metadata(self):
        rows = [
            {
                "orderid": f"order-{index}", "userid": "user-123", "accounttype": "ISA",
                "ticker": "AAPL", "action": "BUY", "quantity": quantity,
                "executedvalue": value, "status": status, "latest_event": None,
            }
            for index, (status, quantity, value) in enumerate([
                ("SETTLED", 4, 500), ("FILLED", 2, 300), ("REJECTED", 4, 500),
                ("ACCEPTED", 4, None), ("SETTLED", 0, 500),
            ])
        ]
        payload = {
            "summary": {"total_requests": 100}, "rows": rows,
            "total_matching_orders": 45, "available_instruments": ["AAPL", "MSFT"],
        }
        with patch("app.main.get_conn"), patch("app.main.fetch_trade_bounds", return_value={}), patch(
            "app.main.fetch_operations_overview", return_value=payload
        ) as fetch:
            result = operations_overview(
                period_from=None, period_to=None, instrument="AAPL", account_type=None,
                search="user", execution_status=None, settlement_status=None,
                exception_filter=None, limit=20, offset=20,
            )

        self.assertEqual([row.execution_price_usd for row in result.orders], [125, 150, None, None, None])
        self.assertEqual(result.total_matching_orders, 45)
        self.assertEqual(result.available_instruments, ["AAPL", "MSFT"])
        self.assertEqual(fetch.call_args.args[2:], (20, 20))

    def test_count_and_rows_share_filters_without_paginating_count_or_options(self):
        conn = MagicMock()
        cursor = conn.cursor.return_value.__enter__.return_value
        cursor.fetchone.side_effect = [{"total_requests": 100}, {"total_matching_orders": 45}]
        cursor.fetchall.side_effect = [[], [{"ticker": "AAPL"}, {"ticker": "MSFT"}]]
        filters = OrderFilters(
            instrument="AAPL", search="user", execution_status="SETTLED",
            settlement_status="SETTLED", exception_filter="NO_EXCEPTIONS",
        )

        result = fetch_operations_overview(conn, filters, 20, 20)

        summary_call, count_call, rows_call, options_call = cursor.execute.call_args_list
        self.assertNotIn("ILIKE", summary_call.args[0])
        self.assertEqual(count_call.args[1], rows_call.args[1][:-2])
        self.assertEqual(rows_call.args[1][-2:], [20, 20])
        self.assertEqual(
            count_call.args[0].split("WHERE")[1].strip(),
            rows_call.args[0].split("WHERE")[1].split("ORDER BY")[0].strip(),
        )
        self.assertNotIn("LIMIT", count_call.args[0])
        self.assertIn("o.orderid DESC", rows_call.args[0])
        self.assertNotIn("WHERE", options_call.args[0])
        self.assertNotIn("LIMIT", options_call.args[0])
        self.assertEqual(result["total_matching_orders"], 45)
        self.assertEqual(result["available_instruments"], ["AAPL", "MSFT"])


if __name__ == "__main__":
    unittest.main()