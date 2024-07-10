package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
//import java.util.List; 
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
IssuanceRequest issuanceRequest = om.readValue(myJsonString, IssuanceRequest.class); */

public class IssuanceRequest{
    public boolean includeQRCode;
    public boolean includeReceipt;
    public Callback callback;
    public String authority;
    public Registration registration;
    public String type;
    public String manifest;
    public Pin pin;
    public Map<String, String> claims;
    public Date expirationDate;
}
