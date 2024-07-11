package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CallbackEvent {
    public String requestId;
    public String requestStatus;
    public String state;
    public String subject;
    public ArrayList<VerifiedCredentialsData> verifiedCredentialsData;
    public Receipt receipt;
    @JsonProperty("error")
    public CallbackError error;
}
