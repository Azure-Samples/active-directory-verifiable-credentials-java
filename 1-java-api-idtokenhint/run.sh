#!/bin/bash
export AzureAD__TenantId=<YOUR-ENTRA-TENANDID>
export AzureAD__ClientID=<YOUR-ENTRA-CLIENTID>
export AzureAD__ClientSecret=<YOUR-ENTRA-CLIENTSECRET>
export AzureAD__CertName=<YOUR-CERTNAME_not_used_if_secret_is_set>
export AzureAD__CertLocation=$(pwd)/AppCreationScripts/aadappcert.crt
export AzureAD__CertKeyLocation=$(pwd)/AppCreationScripts/aadappcert.key
export VerifiedID__ApiKey=$(uuidgen)
export VerifiedID__DIDAUTHORITY=<YOUR-DID>
export VerifiedID__ApiEndpoint=https://verifiedid.did.msidentity.com/v1.0/
export VerifiedID__CREDENTIALMANIFEST=<YOUR-CREDENTIAL-MANIFEST-URL>
export VerifiedID__CREDENTIALTYPE=VerifiedCredentialExpert
export VerifiedID__ClientName="Java Sample App"
export VerifiedID__Purpose="To test Verified ID"
export VerifiedID__PinCodeLength=4
export VerifiedID__PhotoClaimName=photo
export VerifiedID__UseFaceCheck=false

java -version
java -jar ./target/java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar
