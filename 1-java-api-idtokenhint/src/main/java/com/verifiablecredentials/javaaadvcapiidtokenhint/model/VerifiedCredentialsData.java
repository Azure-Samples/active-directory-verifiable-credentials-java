package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
import java.util.ArrayList;
import java.util.Map;

public class VerifiedCredentialsData {
    public String issuer;
    public ArrayList<String> type;
    public Map<String, String> claims;
    public CredentialState credentialState;
    public DomainValidation domainValidation;
    public String issuanceDate;
    public String expirationDate;
}
