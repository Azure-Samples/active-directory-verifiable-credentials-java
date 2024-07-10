package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
//import java.util.List; 
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
RequestAPIResponse requestAPIResponse = om.readValue(myJsonString, RequestAPIResponse.class); */

public class RequestAPIResponse{
    public String requestId;
    public String url;
    public int expiry;
    public String qrCode;
    public String id;
    public String pin;
}
