package com.verifiablecredentials.javaaadvcapiidtokenhint.controller;

import java.util.*;
import java.util.logging.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.*;
import com.github.benmanes.caffeine.cache.*;
import com.verifiablecredentials.javaaadvcapiidtokenhint.helpers.*;
import com.verifiablecredentials.javaaadvcapiidtokenhint.model.*;

@RestController
@EnableCaching
public class VerifierController {
    private static final Logger lgr = Logger.getLogger(VerifierController.class.getName());

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

    private String callVCClientAPI( String payload ) {
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
        String endpoint = apiEndpoint.replace("http://", "https://" ) + "verifiableCredentials/createPresentationRequest";
        lgr.info( "callVCClientAPI: " + endpoint + "\n" + payload );
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

    private PresentationRequest createPresentationRequest( HttpServletRequest httpRequest ) {

        PresentationRequest request = new PresentationRequest();

        request.registration = new Registration();
        request.authority = AppConfig.getDidAuthority();
        request.includeReceipt = true;
        request.registration.clientName = AppConfig.getClientName();

        request.callback = new Callback();
        request.callback.url = getBasePath(httpRequest) + "api/verifier/presentation-request-callback";
        request.callback.state = java.util.UUID.randomUUID().toString();;
        request.callback.headers = new Headers();
        request.callback.headers.apiKey = AppConfig.getApiKey();

        request.requestedCredentials = new ArrayList<RequestedCredential>();
        RequestedCredential requestedCredential = new RequestedCredential();
        requestedCredential.type = AppConfig.getCredentialType();
        requestedCredential.purpose = AppConfig.getPurpose();
        requestedCredential.acceptedIssuers = new ArrayList<String>();
        requestedCredential.acceptedIssuers.add( AppConfig.getDidAuthority() );
        requestedCredential.configuration = new Configuration();    
        requestedCredential.configuration.validation = new Validation();
        requestedCredential.configuration.validation.allowRevoked = false;
        requestedCredential.configuration.validation.validateLinkedDomain = true;
        request.requestedCredentials.add( requestedCredential );
    
        return request;
    }

    /**
     * This method is called from the UI to initiate the presentation of the verifiable credential
     * @param request
     * @param headers
     * @return JSON object with the address to the presentation request and optionally a QR code and a state value which can be used to check on the response status
     */
    @CrossOrigin(origins = "*") // needed for B2C
    @GetMapping("/api/verifier/presentation-request")
    public ResponseEntity<String> presentationRequest( HttpServletRequest request
                                                    , @RequestHeader HttpHeaders headers ) {
        traceHttpRequest( request );
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        String responseBody = "";
        try {
            PresentationRequest presentationRequest = createPresentationRequest( request );
            String correlationId = presentationRequest.callback.state;
            String faceCheck = request.getParameter("faceCheck");
            if ( !faceCheck.isEmpty() && faceCheck.equals("1") ) {
                String photoClaimName = request.getParameter("photoClaimName");
                if ( photoClaimName.isEmpty() || photoClaimName.isBlank() ) {
                    photoClaimName = AppConfig.getPhotoClaimName();
                }
                FaceCheck fc = new FaceCheck();
                fc.sourcePhotoClaimName = photoClaimName;
                fc.matchConfidenceThreshold = 70;
                presentationRequest.requestedCredentials.get(0).configuration.validation.faceCheck = fc;
            }
            String payload = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(presentationRequest);
            lgr.info( payload );

            ObjectNode data = objectMapper.createObjectNode();
            data.put("status", "request_created" );
            data.put("message", "Waiting for QR code to be scanned" );
            cache.put( correlationId, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data) );

            responseBody = callVCClientAPI( payload );
            RequestAPIResponse presentationResponse = objectMapper.readValue(responseBody, RequestAPIResponse.class);
            presentationResponse.id = correlationId;
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(presentationResponse);
            lgr.info( responseBody );
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
        }
        // the response from the VC Request API call is returned to the caller (the UI). It contains the URI to the request which Authenticator can download after
        // it has scanned the QR code. If the payload requested the VC Request service to create the QR code that is returned as well
        // the javascript in the UI will use that QR code to display it on the screen to the user.            
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");    
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( responseBody );
    }

    @GetMapping("/api/verifier/get-presentation-details")
    public ResponseEntity<String> getPresentationDetals( HttpServletRequest request
                                            , @RequestHeader HttpHeaders headers ) {
        traceHttpRequest( request );
        String responseBody = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            PresentationRequest presentationRequest = createPresentationRequest( request );
            ObjectNode data = objectMapper.createObjectNode();
            data.put("clientName", presentationRequest.registration.clientName );
            data.put("purpose", presentationRequest.requestedCredentials.get(0).purpose );
            data.put("didAuthority", presentationRequest.authority ); 
            data.put("type", presentationRequest.requestedCredentials.get(0).type );
            data.put("acceptedIssuers", presentationRequest.requestedCredentials.get(0).acceptedIssuers.get(0) );
            data.put("photoClaimName", AppConfig.getPhotoClaimName() ); 
            data.put("useFaceCheck", AppConfig.getUseFaceCheck() ); 
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
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
} // cls