package main.dto;

public class UpdateAccountRequest {
    private String accountType;

    public UpdateAccountRequest() {
    }

    public UpdateAccountRequest(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
