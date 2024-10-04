package com.verifiablecredentials.javaaadvcapiidtokenhint.model;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Pin{
    public String value;
    public int length;
    public String salt;
    public String alg;
    public Integer iterations;
}
