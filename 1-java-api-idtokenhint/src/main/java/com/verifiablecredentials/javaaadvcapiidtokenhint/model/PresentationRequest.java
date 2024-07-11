package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
//import java.util.List; 
import java.util.ArrayList;

// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
Root root = om.readValue(myJsonString, Root.class); */

public class PresentationRequest{
    public boolean includeQRCode;
    public boolean includeReceipt;
    public String authority;
    public Registration registration;
    public Callback callback;
    public ArrayList<RequestedCredential> requestedCredentials;
}
