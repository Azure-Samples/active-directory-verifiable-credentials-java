package com.verifiablecredentials.javaaadvcapiidtokenhint.helpers;

public class AppConfig {
    private static String aadAuthority = System.getenv( "AzureAD__aadAuthority");
    private static String tenantId = System.getenv("AzureAD__tenantId" );
    private static String managedIdentity = System.getenv("AzureAD__managedIdentity");
    private static String clientId = System.getenv("AzureAD__clientId");
    private static String clientSecret = System.getenv("AzureAD__clientSecret");
    private static String certLocation = System.getenv("AzureAD__certLocation");
    private static String certKeyLocation = System.getenv("AzureAD__certKeyLocation");
    private static String scope = System.getenv("AzureAD__scope");
    private static String apiEndpoint = System.getenv("VerifiedID__apiEndpoint");
    private static String didAuthority = System.getenv( "VerifiedID__didAuthority" );
    private static String clientName = System.getenv("VerifiedID__clientName");
    private static String apiKey = System.getenv( "VerifiedID__apiKey" );
    private static String credentialType = System.getenv("VerifiedID__credentialType");
    private static String purpose = System.getenv("VerifiedID__purpose");
    private static String manifestUrl = System.getenv("VerifiedID__credentialManifest");
    private static String pinCodeLength = System.getenv("VerifiedID__pinCodeLength");        
    private static String photoClaimName = System.getenv("VerifiedID__photoClaimName");        
    private static String useFaceCheck = System.getenv("VerifiedID__useFaceCheck");        

    public static String getEntraAuthority() {
        if ( aadAuthority == null || aadAuthority.isEmpty() ) {
            aadAuthority = "https://login.microsoftonline.com/{0}";
        }
        if ( aadAuthority.contains( "{0}")) {
            aadAuthority = aadAuthority.replace("{0}", tenantId );
        }
        return aadAuthority;
    }    
    public static String getEntraTenantId() {
        return tenantId;
    }    
    public static Boolean getEntraManagedIdentity() {
        if ( (managedIdentity != null && !managedIdentity.isEmpty() ) ) {
            return Boolean.parseBoolean(managedIdentity);
        } else {
            return false;
        }
    }    
    public static String getEntraClientId() {
        return clientId;
    }    
    public static String getEntraClientSecret() {
        return clientSecret;
    }    
    public static String getEntraCertKeyLocation() {
        return certKeyLocation;
    }    
    public static String getEntraCertLocation() {
        return certLocation;
    }    
    public static String getRequestAPIScope() {
        if ( scope == null || scope.isEmpty()) {
            scope = "3db474b9-6a0c-4840-96ac-1fceb342124f/.default";
        }
        return scope;
    }    
    public static String getApiEndpoint() {
        if ( apiEndpoint == null || ( apiEndpoint != null && apiEndpoint.isEmpty() )) {
            return "https://verifiedid.did.msidentity.com/v1.0/";
        }
        return apiEndpoint;
    }    
    public static String getDidAuthority() {
        return didAuthority;
    }    
    public static String getClientName() {
        if ( clientName == null || (clientName != null && clientName.isEmpty() ) ) {
            return "Java Sample app";
        }
        return clientName;
    }    
    public static String getPurpose() {
        if ( purpose == null || (purpose != null && purpose.isEmpty() ) ) {
            return "Java Sample app";
        }
        return purpose;
    }    
    public static String getApiKey() {
        return apiKey;
    }    
    public static String getCredentialType() {
        return credentialType;
    }    
    public static String getManifestURL() {
        return manifestUrl;
    }    
    public static Integer getPinCodeLength() {
        int pcl = 0;
        try {
            pcl = Integer.parseInt(pinCodeLength);
        } catch(NumberFormatException ex) {
            pcl = 0;
        }    
        return pcl;
    }    
    public static String getPhotoClaimName() {
        return photoClaimName;
    }    
    public static Boolean getUseFaceCheck() {
        if ( useFaceCheck != null && !useFaceCheck.isEmpty() ) {
            return Boolean.parseBoolean(useFaceCheck);
        } else {
            return false;
        }
    }    
} // cls