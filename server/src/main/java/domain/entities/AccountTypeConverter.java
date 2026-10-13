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
            case _401K -> "401K";
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
            case "401K", "_401K" -> Account.AccountType._401K;
            case "ROTH IRA", "ROTH_IRA" -> Account.AccountType.ROTH_IRA;
            default -> Account.AccountType.valueOf(databaseValue);
        };
    }
}