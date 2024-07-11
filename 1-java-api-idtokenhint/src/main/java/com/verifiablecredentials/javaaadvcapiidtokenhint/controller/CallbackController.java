package com.verifiablecredentials.javaaadvcapiidtokenhint.controller;

import java.util.*;
import java.util.logging.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.*;
import com.github.benmanes.caffeine.cache.*;
import com.verifiablecredentials.javaaadvcapiidtokenhint.helpers.*;
import com.verifiablecredentials.javaaadvcapiidtokenhint.model.*;

@RestController
@EnableCaching
public class CallbackController {
    private static final Logger lgr = Logger.getLogger(CallbackController.class.getName());

    private static Cache<String, String> cache = CacheHelper.getCache(); /*Caffeine.newBuilder()
                                            .expireAfterWrite(15, TimeUnit.MINUTES)
                                            .maximumSize(100)
                                            .build();*/

    // *********************************************************************************
    // helpers
    // *********************************************************************************
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
    

    public ResponseEntity<String> handleRequestCallback( HttpServletRequest request
                                                             , @RequestHeader HttpHeaders headers
                                                             , @RequestBody String body
                                                             , String requestType ) {
        traceHttpRequest( request );
        lgr.info( body );
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        try {
            // we need to get back our api-key in the header to make sure we don't accept unsolicited calls
            if ( !request.getHeader("api-key").equals( AppConfig.getApiKey() ) ) {
                lgr.info( "api-key wrong or missing" );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body( "api-key wrong or missing" );
            }
            CallbackEvent callbackEvent = null;
            try {
                callbackEvent = objectMapper.readValue(body, CallbackEvent.class);
            } catch(java.io.IOException exc) {
                // there is something wrong with the callback event. Try and grab the error atleast
                JsonNode cb = objectMapper.readTree( body  );
                callbackEvent = new CallbackEvent();
                callbackEvent.requestStatus = cb.path("requestStatus").asText();
                callbackEvent.state = cb.path("state").asText();
            }

            ArrayList<String> presentationStatus = new ArrayList<String>() { {  
                add("request_retrieved"); add("presentation_verified"); add("presentation_error");
             } };
            ArrayList<String> issuanceStatus = new ArrayList<String>() {{
                add("request_retrieved"); add("issuance_successful"); add("issuance_error");
            } };
            ArrayList<String> selfieStatus = new ArrayList<String>() {{ add("selfie_taken" ); } };

            if (   (requestType.equals( "issuance") && issuanceStatus.contains(callbackEvent.requestStatus) ) 
                || (requestType.equals( "presentation") && presentationStatus.contains(callbackEvent.requestStatus) ) 
                || (requestType.equals( "selfie") && selfieStatus.contains(callbackEvent.requestStatus) ) 
               ) {
                String data = cache.getIfPresent( callbackEvent.state ); 
                if ( data == null ) {
                    lgr.info( "Unknown state: " + callbackEvent.state );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Unknown state" );
                } else {
                    JsonNode cachedData = objectMapper.readTree( data );
                    ((ObjectNode)cachedData).put( "status", callbackEvent.requestStatus );
                    ((ObjectNode)cachedData).put( "callback", body );
                    cache.put( callbackEvent.state, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cachedData) );
                }
            } else {
                lgr.info( "Unsupported requestStatus: " + callbackEvent.requestStatus );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Unsupported requestStatus: " + callbackEvent.requestStatus );
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
        }            
        return ResponseEntity.ok()
          .body( "{}" );
    }

    @RequestMapping(value = "/api/issuer/issue-request-callback", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> issueRequestCallback( HttpServletRequest request
                                                      , @RequestHeader HttpHeaders headers
                                                      , @RequestBody String body ) {
        return handleRequestCallback(request, headers, body, "issuance");
    }

    @RequestMapping(value = "/api/verifier/presentation-request-callback", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> presentationRequestCallback( HttpServletRequest request
                                                             , @RequestHeader HttpHeaders headers
                                                             , @RequestBody String body ) {
        return handleRequestCallback(request, headers, body, "presentation");
    }
    /**
     * this function is called from the UI polling for a response from the AAD VC Service.
     * when a callback is recieved at the presentationCallback service the session will be updated
     * this method will respond with the status so the UI can reflect if the QR code was scanned and with the result of the presentation
     * @param request
     * @param headers
     * @param id the correlation id that was set in the state attribute in the payload
     * @return response to the browser on the progress of the issuance
     */
    @GetMapping("/api/request-status")
    public ResponseEntity<String> requestStatus( HttpServletRequest request
                                               , @RequestHeader HttpHeaders headers
                                               , @RequestParam String id ) {
        traceHttpRequest( request );
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");    
        String responseBody = "{ 'status': 'request_not_created', 'message': 'No data'}";
        try {
            String cachedData = cache.getIfPresent( id );        

            if ( (cachedData == null || cachedData.isEmpty()) ) {
                return ResponseEntity.ok().headers(responseHeaders).body( responseBody );  
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode cacheData = objectMapper.readTree( cachedData  );
            String requestStatus = cacheData.path("status").asText();

            ObjectNode statusResponse = objectMapper.createObjectNode();
            statusResponse.put("status", requestStatus );

            CallbackEvent callbackEvent = null;
            String callback = cacheData.path("callback").asText();
            if ( callback != null && !callback.isEmpty() ) {
                try {
                    callbackEvent = objectMapper.readValue(callback, CallbackEvent.class);  
                } catch(java.io.IOException exc) {
                    // there is something wrong with the callback event. Try and grab the error atleast
                    JsonNode cb = objectMapper.readTree( callback  );
                    callbackEvent = new CallbackEvent();
                    callbackEvent.requestId = cb.path("requestId").asText();
                    callbackEvent.requestStatus = cb.path("requestStatus").asText();
                    callbackEvent.state = cb.path("state").asText();
                    callbackEvent.error = new CallbackError();
                    callbackEvent.error.code = cb.path("error").path("code").asText();  
                    callbackEvent.error.message = cb.path("error").path("message").asText();  
                }
    
            }
            
            switch( requestStatus ) {
                case "request_created":
                statusResponse.put("message", "Waiting to scan QR code" );
                break;
            case "request_retrieved":
                statusResponse.put("message", "QR code is scanned. Waiting for user action..." );
                break;
            case "issuance_error":
                statusResponse.put("message", "Issuance failed: " + callbackEvent.error.message );
                break;
            case "issuance_successful":
                statusResponse.put("message", "Issuance successful" );
                break;
            case "presentation_error":
                statusResponse.put("message", "Presentation failed:" + callbackEvent.error.message );
                break;
            case "presentation_verified": 
                statusResponse.put("subject", callbackEvent.subject );
                String vcData = responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callbackEvent.verifiedCredentialsData);
                statusResponse.put("payload", objectMapper.readTree( vcData )  ); 
                statusResponse.set("type", objectMapper.valueToTree(callbackEvent.verifiedCredentialsData.get(0).type));
                statusResponse.put("issuanceDate", callbackEvent.verifiedCredentialsData.get(0).issuanceDate );
                statusResponse.put("expirationDate", callbackEvent.verifiedCredentialsData.get(0).expirationDate );
                if ( callbackEvent.receipt != null && callbackEvent.receipt.vp_token != null) {
                    String vp = base64Decode( callbackEvent.receipt.vp_token.split("\\.")[1] );
                    JsonNode vpToken = objectMapper.readTree( vp );  
                    String vc = base64Decode( vpToken.path("vp").path("verifiableCredential").get(0).asText().split("\\.")[1] );
                    JsonNode vcToken = objectMapper.readTree( vc );  
                    statusResponse.put("jti", vcToken.path("jti").asText() );
                }
                break;
            }
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusResponse);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
        }    
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( responseBody );

    }
    @GetMapping("/api/callback/cache")
    public ResponseEntity<String> verifierCache( HttpServletRequest request, @RequestHeader HttpHeaders headers, @RequestParam String id ) {
        traceHttpRequest( request );
        String responseBody = cache.getIfPresent( id );        
        if ( responseBody == null ) {
            responseBody = "is null";
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");    
        return ResponseEntity.ok()
        .headers(responseHeaders)
        .body( responseBody );
    }

} // cls