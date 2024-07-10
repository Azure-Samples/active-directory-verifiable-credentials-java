package com.verifiablecredentials.javaaadvcapiidtokenhint.helpers;

import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import com.microsoft.aad.msal4j.*;

public class MSALHelper {
    private static final Logger lgr = Logger.getLogger(MSALHelper.class.getName());

    // *********************************************************************************
    // helpers
    // *********************************************************************************

    public static String getAccessToken() throws Exception {
        Boolean managedIdentity = AppConfig.getEntraManagedIdentity();
        String scope = AppConfig.getRequestAPIScope();
        if ( managedIdentity ) {
            lgr.info( "MSAL Acquire AccessToken via Managed Identity" );
            ManagedIdentityId managedIdentityId = ManagedIdentityId.systemAssigned();
            ManagedIdentityApplication msiApp = ManagedIdentityApplication.builder(managedIdentityId).logPii(true).build();
            IAuthenticationResult result = msiApp.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder( scope ).build()).get();
            return result.accessToken();
        }
        String authority = AppConfig.getEntraAuthority();
        lgr.info( authority );
        ConfidentialClientApplication app = null;
        String clientId = AppConfig.getEntraClientId();
        String clientSecret = AppConfig.getEntraClientSecret();
        if ( !clientSecret.isEmpty() ) {
            lgr.info( "MSAL Acquire AccessToken via Client Credentials" );
            app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(clientSecret))
                .authority(authority)
                .build();
        } else {
            String certLocation = AppConfig.getEntraCertLocation();
            String certKeyLocation = AppConfig.getEntraCertKeyLocation();
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
                Collections.singleton( scope ))
                .build();
        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
        IAuthenticationResult result = future.get();
        return result.accessToken();
    }    
} // cls