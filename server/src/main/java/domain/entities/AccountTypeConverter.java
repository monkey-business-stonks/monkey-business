package domain.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccountTypeConverter implements AttributeConverter<Account.AccountType, String> {
    @Override
    public String convertToDatabaseColumn(Account.AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case TRADITIONAL_401K -> "TRADITIONAL 401K";
            case ROTH_401K -> "ROTH 401K";
            case TRADITIONAL_IRA -> "TRADITIONAL IRA";
            case ROTH_IRA -> "ROTH IRA";
            default -> accountType.name();
        };
    }

    @Override
    public Account.AccountType convertToEntityAttribute(String databaseValue) {
        if (databaseValue == null) {
            return null;
        }

        return switch (databaseValue) {
            case "401K", "_401K", "TRADITIONAL 401K", "TRADITIONAL_401K" -> Account.AccountType.TRADITIONAL_401K;
            case "ROTH 401K", "ROTH_401K" -> Account.AccountType.ROTH_401K;
            case "TRADITIONAL IRA", "TRADITIONAL_IRA" -> Account.AccountType.TRADITIONAL_IRA;
            case "ROTH IRA", "ROTH_IRA" -> Account.AccountType.ROTH_IRA;
            default -> Account.AccountType.valueOf(databaseValue);
        };
    }
}