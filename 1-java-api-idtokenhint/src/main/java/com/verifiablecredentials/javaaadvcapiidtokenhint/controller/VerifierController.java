package com.verifiablecredentials.javaaadvcapiidtokenhint.controller;

import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.util.stream.*;
//import java.text.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
//import com.fasterxml.jackson.databind.node.ArrayNode;
import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cache.annotation.*;
import com.github.benmanes.caffeine.cache.*;
import com.microsoft.aad.msal4j.*;

@RestController
@EnableCaching
public class VerifierController {
    private static final Logger lgr = Logger.getLogger(VerifierController.class.getName());

    private Cache<String, String> cache = Caffeine.newBuilder()
                                            .expireAfterWrite(15, TimeUnit.MINUTES)
                                            .maximumSize(100)
                                            .build();

    // *********************************************************************************
    // application properties - from envvars
    // *********************************************************************************
    @Value("${aadvc_PresentationFile}")
    private String presentationJsonFIle;

    @Value("${aadvc_ApiEndpoint}")
    private String apiEndpoint;

    @Value("${aadvc_TenantId}")
    private String tenantId;

    @Value("${aadvc_scope}")
    private String scope;

    @Value("${aadvc_ApiKey}")
    private String apiKey;

    @Value("${aadvc_ClientId}")
    private String clientId;
    
    @Value("${aadvc_ClientSecret}")
    private String clientSecret;
    
    @Value("${aadvc_CertName}")
    private String certName;
    
    @Value("${aadvc_CertLocation}")
    private String certLocation;
    
    @Value("${aadvc_CertKeyLocation}")
    private String certKeyLocation;
    
    @Value("${aadvc_Authority}")
    private String aadAuthority;

    @Value("${aadvc_IssuerAuthority}")
    private String issuerAuthority;

    @Value("${aadvc_VerifierAuthority}")
    private String verifierAuthority;

    @Value("${aadvc_CredentialManifest}")
    private String credentialManifest;

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

    private static String readFileAllText(String filePath) 
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
    
    private String callVCClientAPI( String payload ) {
        String accessToken = "";
        try {
            accessToken = cache.getIfPresent( "MSALAccessToken" );
            if ( accessToken == null || accessToken.isEmpty() ) {
                accessToken = getMSALAccessToken();
                lgr.info( accessToken );
                cache.put( "MSALAccessToken", accessToken );
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
            return null;
        }
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

    private String getMSALAccessToken() throws Exception {
        String authority = aadAuthority.replace("{0}", tenantId );
        lgr.info( aadAuthority );
        lgr.info( authority );
        ConfidentialClientApplication app = null;
        if ( !clientSecret.isEmpty() ) {
            lgr.info( "MSAL Acquire AccessToken via Client Credentials" );
            app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(clientSecret))
                .authority(authority)
                .build();
        } else {
            lgr.info( "MSAL Acquire AccessToken via Certificate" );
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(certKeyLocation)));
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(spec);    
            java.io.InputStream certStream = (java.io.InputStream)new ByteArrayInputStream(Files.readAllBytes(Paths.get(certLocation)));
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certStream);            
            app = ConfidentialClientApplication.builder(
                   clientId,
                   ClientCredentialFactory.createFromCertificate(key, cert))
                   .authority(authority)
                   .build();
        }
        ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                Collections.singleton(scope))
                .build();
        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
        IAuthenticationResult result = future.get();
        return result.accessToken();
    }    

    /**
     * This method is called from the UI to initiate the presentation of the verifiable credential
     * @param request
     * @param headers
     * @return JSON object with the address to the presentation request and optionally a QR code and a state value which can be used to check on the response status
     */
    @CrossOrigin(origins = "*") // needed for B2C
    @GetMapping("/api/verifier/presentation-request")
    public ResponseEntity<String> presentationRequest( HttpServletRequest request, @RequestHeader HttpHeaders headers ) {
        traceHttpRequest( request );
        String jsonRequest = cache.getIfPresent("presentationJsonFIle");
        if ( jsonRequest == null || jsonRequest.isEmpty() ) {
            lgr.info( "presentationJsonFIle=" + presentationJsonFIle );
            jsonRequest = readFileAllText( presentationJsonFIle );
        }
        if ( jsonRequest == null || jsonRequest.isEmpty() ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Presentation request file not found" );        
        }
        String callback = getBasePath( request ) + "api/verifier/presentation-request-callback";
        String correlationId = java.util.UUID.randomUUID().toString();
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = "";
        try {
            JsonNode rootNode = objectMapper.readTree( jsonRequest );
            // modify the callback method to make it easier to debug 
            // with tools like ngrok since the URI changes all the time
            // this way you don't need to modify the callback URL in the payload every time
            // ngrok changes the URI
            ((ObjectNode)rootNode).put("authority", verifierAuthority );
            ((ObjectNode)(rootNode.path("callback"))).put("url", callback );
            ((ObjectNode)(rootNode.path("callback"))).put("state", correlationId );
            // set our api-key so we check that callbacks are legitimate
            ((ObjectNode)(rootNode.path("callback").path("headers"))).put("api-key", apiKey );
            // copy the issuerDID from the settings and fill in the acceptedIssuer part of the payload
            // this means only that issuer should be trusted for the requested credentialtype
            // this value is an array in the payload, you can trust multiple issuers for the same credentialtype
            // very common to accept the test VCs and the Production VCs coming from different verifiable credential services
            ((ArrayNode)(rootNode.path("requestedCredentials").get(0).path("acceptedIssuers"))).set( 0, new TextNode( issuerAuthority ) );
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            responseBody = callVCClientAPI( payload );
            JsonNode apiResponse = objectMapper.readTree( responseBody );
            ((ObjectNode)apiResponse).put( "id", correlationId );
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse);
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

    /**
     * This method is called by the VC Request API when the user scans a QR code and presents a Verifiable Credential to the service 
     * @param request
     * @param headers
     * @param body callback json payload
     * @return
     */
    @RequestMapping(value = "/api/verifier/presentation-request-callback", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> presentationRequestCallback( HttpServletRequest request
                                                             , @RequestHeader HttpHeaders headers
                                                             , @RequestBody String body ) {
        traceHttpRequest( request );
        lgr.info( body );
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // we need to get back our api-key in the header to make sure we don't accept unsolicited calls
            if ( !request.getHeader("api-key").equals(apiKey) ) {
                lgr.info( "api-key wrong or missing" );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body( "api-key wrong or missing" );
            }
            JsonNode presentationResponse = objectMapper.readTree( body );
            String requestStatus = presentationResponse.path("requestStatus").asText();
            ObjectNode data = null;
            // there are 2 different callbacks. 1 if the QR code is scanned (or deeplink has been followed)
            // Scanning the QR code makes Authenticator download the specific request from the server
            // the request will be deleted from the server immediately.
            // That's why it is so important to capture this callback and relay this to the UI so the UI can hide
            // the QR code to prevent the user from scanning it twice (resulting in an error since the request is already deleted)            
            if ( requestStatus.equals( "request_retrieved" ) ) {
                data = objectMapper.createObjectNode();
                data.put("message", "QR Code is scanned. Waiting for validation..." );
            }
            // the 2nd callback is the result with the verified credential being verified.
            // typically here is where the business logic is written to determine what to do with the result
            // the response in this callback contains the claims from the Verifiable Credential(s) being presented by the user
            // In this case the result is put in the in memory cache which is used by the UI when polling for the state so the UI can be updated.
            if ( requestStatus.equals( "presentation_verified") ) {
                data = objectMapper.createObjectNode();
                data.put("message", "Presentation received" );
                data.set("payload", presentationResponse.path("verifiedCredentialsData") ); 
                data.put("subject", presentationResponse.path("subject").asText() );
                data.put("firstName", presentationResponse.path("verifiedCredentialsData").get(0).path("claims").path("firstName").asText() );
                data.put("lastName", presentationResponse.path("verifiedCredentialsData").get(0).path("claims").path("lastName").asText() );
                data.set("presentationResponse", presentationResponse );
            }
            if ( data != null ) {
                data.put("status", requestStatus );
                cache.put( presentationResponse.path("state").asText(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data) );
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
        }            
        return ResponseEntity.ok()
          .body( "{}" );
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
    @CrossOrigin(origins = "*") // needed for B2C
    @GetMapping("/api/verifier/presentation-response")
    public ResponseEntity<String> presentationResponseStatus( HttpServletRequest request
                                                            , @RequestHeader HttpHeaders headers
                                                            , @RequestParam String id ) {
        traceHttpRequest( request );
        String responseBody = "";
        String data = cache.getIfPresent( id ); // id == correlationId
        if ( !(data == null || data.isEmpty()) ) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode cacheData = objectMapper.readTree( data  );
                ObjectNode statusResponse = objectMapper.createObjectNode();
                statusResponse.put("status", cacheData.path("status").asText() );
                statusResponse.put("message", cacheData.path("message").asText() );
                statusResponse.put("firstName", cacheData.path("firstName").asText() );
                statusResponse.put("lastName", cacheData.path("lastName").asText() );
                statusResponse.put("subject", cacheData.path("subject").asText() );
                statusResponse.set("payload", cacheData.path("payload") );
                responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusResponse);
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body( "Technical error" );
            }    
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");    
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( responseBody );
    }
     /**
     * B2C REST API Endpoint for retrieveing the VC presentation response
     * @param request POST Request that comes from B2C 
     * @param headers
     * @param body The InputClaims from the B2C policy. It will only be one claim named 'id'
     * @return a JSON structure with claims from the VC presented
     */
    @RequestMapping(value = "/api/verifier/presentation-response-b2c", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> presentationResponseB2C( HttpServletRequest request
                                                             , @RequestHeader HttpHeaders headers
                                                             , @RequestBody String body ) {
        traceHttpRequest( request );
        lgr.info( body );
        String responseBody = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode b2cRequest = objectMapper.readTree( body );
            String id = b2cRequest.path("id").asText();
            String data = cache.getIfPresent( id ); 
            if ( (data == null || data.isEmpty()) ) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body( formatB2CError( "Verifiable Credentials not presented" ) );
            } 
            JsonNode cacheData = objectMapper.readTree( data  );
            String didSubject = cacheData.path("presentationResponse").path("subject").asText();
            // vcKey is the shorthand did but with the colon chars replaced with a period.
            // Reason for this is if you want to store it as a way to signin, you need to add it
            // to the identities collection on the userProfile, the issuerAssignedId does not allow the colon char
            String vcKey = didSubject.replace("did:ion:", "did.ion.").split(":")[0];
            // The type collection always 2..n entries where [0] is the generic base type 'VerifiableCredentials'.
            // We take the last type to pass back for simplicity
            String credentialType = "";
            for (JsonNode arrayElement : cacheData.path("presentationResponse").path("verifiedCredentialsData").get(0).path("type")) {
                credentialType = arrayElement.asText();
            }
            // get the claims from the VC and add a few extra claims that we pass back to B2C
            JsonNode vcClaims = cacheData.path("presentationResponse").path("verifiedCredentialsData").get(0).path("claims");
            ((ObjectNode)vcClaims).put("vcType", credentialType);
            ((ObjectNode)vcClaims).put("vcIss", cacheData.path("presentationResponse").path("subject").asText() );
            ((ObjectNode)vcClaims).put("vcSub", didSubject);
            ((ObjectNode)vcClaims).put("vcKey", vcKey );
            responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vcClaims);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body( formatB2CError( "Technical error" ) );
        }     
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "application/json");    
        return ResponseEntity.ok()
          .headers(responseHeaders)
          .body( responseBody );
    }

    private String formatB2CError( String message ) {
        return "{\"version\": \"1.0.0\", \"status\": 400, \"userMessage\": \"{0}}\"}".replace( "{0}", message );
    }

} // cls