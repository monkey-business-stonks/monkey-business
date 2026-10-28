import os
import unittest
from unittest.mock import patch

from app.db import _db_config, require_env


class SecretConfigurationTests(unittest.TestCase):
    def test_missing_or_blank_values_are_rejected(self):
        for value in (None, "", "   "):
            environment = {} if value is None else {"JWT_SIGNING_KEY": value}
            with self.subTest(value=value), patch.dict(os.environ, environment, clear=True):
                with self.assertRaisesRegex(RuntimeError, "JWT_SIGNING_KEY must be set"):
                    require_env("JWT_SIGNING_KEY")

    def test_database_credentials_have_no_fallback(self):
        for missing_name in ("ANALYTICS_DB_USER", "ANALYTICS_DB_PASSWORD"):
            environment = {
                "ANALYTICS_DB_USER": "validation_reader",
                "ANALYTICS_DB_PASSWORD": "validation_only",
            }
            del environment[missing_name]
            with self.subTest(missing_name=missing_name), patch.dict(os.environ, environment, clear=True):
                with self.assertRaisesRegex(RuntimeError, f"{missing_name} must be set"):
                    _db_config()

    def test_supplied_credentials_are_preserved(self):
        environment = {
            "ANALYTICS_DB_USER": "validation_reader",
            "ANALYTICS_DB_PASSWORD": "validation_'$\\_only",
        }
        with patch.dict(os.environ, environment, clear=True):
            config = _db_config()
        self.assertEqual(config["user"], environment["ANALYTICS_DB_USER"])
        self.assertEqual(config["password"], environment["ANALYTICS_DB_PASSWORD"])


if __name__ == "__main__":
    unittest.main()