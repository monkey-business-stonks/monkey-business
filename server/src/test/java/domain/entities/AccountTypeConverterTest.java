package domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccountTypeConverterTest {
    private final AccountTypeConverter converter = new AccountTypeConverter();

    @Test
    void persistsEveryRetirementTypeUsingItsCanonicalDatabaseValue() {
        assertEquals("TRADITIONAL 401K", converter.convertToDatabaseColumn(Account.AccountType.TRADITIONAL_401K));
        assertEquals("ROTH 401K", converter.convertToDatabaseColumn(Account.AccountType.ROTH_401K));
        assertEquals("TRADITIONAL IRA", converter.convertToDatabaseColumn(Account.AccountType.TRADITIONAL_IRA));
        assertEquals("ROTH IRA", converter.convertToDatabaseColumn(Account.AccountType.ROTH_IRA));
    }

    @Test
    void readsLegacy401kRowsAsTraditional401k() {
        assertEquals(Account.AccountType.TRADITIONAL_401K, converter.convertToEntityAttribute("401K"));
        assertEquals(Account.AccountType.TRADITIONAL_401K, converter.convertToEntityAttribute("_401K"));
    }

    @Test
    void readsNewRetirementValues() {
        assertEquals(Account.AccountType.TRADITIONAL_401K, converter.convertToEntityAttribute("TRADITIONAL 401K"));
        assertEquals(Account.AccountType.ROTH_401K, converter.convertToEntityAttribute("ROTH 401K"));
        assertEquals(Account.AccountType.TRADITIONAL_IRA, converter.convertToEntityAttribute("TRADITIONAL IRA"));
        assertEquals(Account.AccountType.ROTH_IRA, converter.convertToEntityAttribute("ROTH IRA"));
    }
}
