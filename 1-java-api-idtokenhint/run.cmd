set AzureAD__TenantId=<YOUR-ENTRA-TENANDID>
set AzureAD__ClientID=<YOUR-ENTRA-CLIENTID>
set AzureAD__ClientSecret=<YOUR-ENTRA-CLIENTSECRET>
set AzureAD__CertName=<YOUR-CERTNAME_not_used_if_secret_is_set>
set AzureAD__CertLocation=%cd%\AppCreationScripts\aadappcert.crt
set AzureAD__CertKeyLocation=%cd%\AppCreationScripts\aadappcert.key
set VerifiedID__ApiKey=<YOUR-APIKEY>
set VerifiedID__DIDAUTHORITY=<YOUR-DID>
set VerifiedID__ApiEndpoint=https://verifiedid.did.msidentity.com/v1.0/
set VerifiedID__CREDENTIALMANIFEST=<YOUR-CREDENTIAL-MANIFEST-URL>
set VerifiedID__CREDENTIALTYPE=VerifiedCredentialExpert
set VerifiedID__ClientName=Java Sample App
set VerifiedID__Purpose=To test Verified ID
set VerifiedID__PinCodeLength=4
set VerifiedID__HashPinCode=false
set VerifiedID__PhotoClaimName=photo
set VerifiedID__UseFaceCheck=false

java -version
java -jar .\target\java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar
