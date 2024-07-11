package com.verifiablecredentials.javaaadvcapiidtokenhint.controller;

import java.util.*;
import java.util.logging.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.*;
import com.github.benmanes.caffeine.cache.*;
import com.verifiablecredentials.javaaadvcapiidtokenhint.helpers.AppConfig;
import com.verifiablecredentials.javaaadvcapiidtokenhint.helpers.CacheHelper;
import com.verifiablecredentials.javaaadvcapiidtokenhint.helpers.MSALHelper;
import com.verifiablecredentials.javaaadvcapiidtokenhint.model.*;

@RestController
@EnableCaching
public class IssuerController {
    private static final Logger lgr = Logger.getLogger(IssuerController.class.getName());

    private static Cache<String, String> cache = CacheHelper.getCache(); 

    // *********************************************************************************
    // helpers
    // *********************************************************************************
    public static String getBasePath(HttpServletRequest request) {
        String basePath = "https://" + request.getServerName() + "/";
        return basePath;
    }

    public static void traceHttpRequest( HttpServletRequest request ) {
        String method = request.getMethod();
        String requestURL = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestURL += "?" + queryString;
        }
        
        lgr.info( method + " " + requestURL );
    }

    private String base64Decode( String base64String ) {
        if ( (base64String.length()%4) > 0  ) {
            base64String += "====".substring((base64String.length()%4));
        }
        return new String(Base64.getUrlDecoder().decode(base64String), StandardCharsets.UTF_8);
    } 

    private String callRequestServiceAPI( String payload ) {
        String accessToken = "";
        try {
            accessToken = cache.getIfPresent( "MSALAccessToken" );
            if ( accessToken == null || accessToken.isEmpty() ) {
                accessToken = MSALHelper.getAccessToken();
                lgr.info( accessToken );
                cache.put( "MSALAccessToken", accessToken );
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
            return null;
        }
        String apiEndpoint = AppConfig.getApiEndpoint();
        String endpoint = apiEndpoint.replace("http://", "https://" ) + "verifiableCredentials/createIssuanceRequest";
        lgr.info( "callRequestServiceAPI: " + endpoint + "\n" + payload );
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client.post()
                                                    .uri( endpoint )
                                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                    .header("Authorization", "Bearer " + accessToken)
                                                    .accept(MediaType.APPLICATION_JSON)
                                                    .body(BodyInserters.fromObject(payload))
                                                    .retrieve();
        String responseBody = responseSpec.bodyToMono(String.class).block();
        lgr.info( responseBody );
        return responseBody;
    }

    private String downloadManifest( String manifestURL ) {
        lgr.info( "manifestURL: " + manifestURL );
        WebClient client = WebClient.create();
        WebClient.ResponseSpec responseSpec = client.get()
                                                    .uri( manifestURL )
                                                    .accept(MediaType.APPLICATION_JSON)
                                                    .retrieve();
        String responseBody = responseSpec.bodyToMono(String.class).block();
        lgr.info( responseBody );
        return responseBody;
    }

    public String generatePinCode( Integer length ) {
        int min = 0;
        int max = (int)(Integer.parseInt( "999999999999999999999".substring(0, length) ));
        Integer pin = (Integer)(int)((Math.random() * (max - min)) + min);
        return String.format( String.format("%%0%dd", length), pin );
    }

    private boolean fromMobile(HttpServletRequest request) {
        String userAgent = Optional.ofNullable(request.getHeader(HttpHeaders.USER_AGENT)).orElse("").toLowerCase(Locale.ROOT);
        return  userAgent.contains("android") || userAgent.contains("iphone");
    }

    private IssuanceRequest createIssuanceRequest(HttpServletRequest httpRequest, Map<String, String> claims) {

        IssuanceRequest request = new IssuanceRequest();

        request.registration = new Registration();
        request.authority = AppConfig.getDidAuthority();
        request.includeReceipt = true;
        request.registration.clientName = AppConfig.getClientName();

        request.callback = new Callback();
        request.callback.url = getBasePath( httpRequest ) + "api/issuer/issue-request-callback";
        request.callback.state = java.util.UUID.randomUUID().toString();;
        request.callback.headers = new Headers();
        request.callback.headers.apiKey = AppConfig.getApiKey();

        request.type = AppConfig.getCredentialType();
        request.manifest = AppConfig.getManifestURL();

        request.claims = claims;

        if ( !fromMobile(httpRequest) ) {
            int pinCodeLength = AppConfig.getPinCodeLength();
            if ( pinCodeLength > 0 ) {
                request.pin = new Pin();
                request.pin.length = pinCodeLength;
                request.pin.value = generatePinCode( pinCodeLength );
            }
        }

        return request;
    }


    /**
     * This method is called from the UI to initiate the issuance of the verifiable credential
     * @param request
     * @param headers
     * @return JSON object with the address to the presentation request and optionally a QR code and a state value which can be used to check on the response status
     */
    @GetMapping("/api/issuer/issuance-request")
    public ResponseEntity<String> issueRequest( HttpServletRequest request, @RequestHeader HttpHeaders headers ) {
        traceHttpRequest( request );
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        String responseBody = "";
        try {
            // Modify this to add the claims needed by your solution
            Map<String, String> claims = new HashMap<String, String>();
            claims.put("given_name", "Megan");
            claims.put("family_name", "Bowen");

            IssuanceRequest issuanceRequest = createIssuanceRequest( request, claims );
            String correlationId = issuanceRequest.callback.state;
            String payload = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(issuanceRequest);

            responseBody = callRequestServiceAPI( payload );
            RequestAPIResponse issuanceResponse = objectMapper.readValue(responseBody, RequestAPIResponse.class);
            issuanceResponse.id = correlationId;
            if ( issuanceRequest.pin != null  ) {
                issuanceResponse.pin = issuanceRequest.pin.value;
            }
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(issuanceResponse);
            lgr.info( responseBody );

            ObjectNode data = objectMapper.createObjectNode();
            data.put("status", "request_created" );
            data.put("message", "Waiting for QR code to be scanned" );
            String cachedData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
            lgr.info( cachedData );
            cache.put( correlationId, cachedData );
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( responseBody );
    }

    @GetMapping("/api/issuer/get-manifest")
    public ResponseEntity<String> getManifest( HttpServletRequest request
                                            , @RequestHeader HttpHeaders headers ) {
        traceHttpRequest( request );
        String manifest = cache.getIfPresent( "manifest" );
        if ( manifest == null ) {
            String responseBody = downloadManifest( AppConfig.getManifestURL() );
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode resp = objectMapper.readTree( responseBody );
                manifest = base64Decode( resp.path("token").asText().split("\\.")[1] );
                cache.put( "manifest", manifest );
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
            }
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( manifest );
    }

} // cls
