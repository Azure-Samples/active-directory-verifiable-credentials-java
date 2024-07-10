package com.verifiablecredentials.javaaadvcapiidtokenhint.model; 
//import java.util.List; 
import java.util.ArrayList;

public class RequestedCredential{
    public String type;
    public String purpose;
    public ArrayList<String> acceptedIssuers;
    public Configuration configuration;
}
