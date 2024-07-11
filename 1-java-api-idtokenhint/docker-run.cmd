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
set VerifiedID__PhotoClaimName=photo
set VerifiedID__UseFaceCheck=false

docker run --rm -it -p 8080:8080 ^
    -e AzureAD_TenantId=%AzureAD_TenantId% ^
    -e AzureAD_ClientID=%AzureAD_ClientId% ^
    -e AzureAD_ClientSecret=%AzureAD_ClientSecret% ^
    -e AzureAD_CertName=%AzureAD_CertName% ^
    -e AzureAD_CertLocation=/usr/local/lib/aadappcert.crt ^
    -e AzureAD_CertKeyLocation=/usr/local/lib/aadappcert.key ^
    -e VerifiedID_ApiKey=%VerifiedID_ApiKey% ^
    -e VerifiedID_ApiEndpoint=%VerifiedID_ApiEndpoint% ^
    -e VerifiedID_CREDENTIALMANIFEST=%VerifiedID_CREDENTIALMANIFEST% ^
    -e VerifiedID_DIDAUTHORITY=%VerifiedID_DIDAUTHORITY% ^
    -e VerifiedID_CREDENTIALTYPE=%VerifiedID_CREDENTIALTYPE% ^
    -e VerifiedID_PinCodeLength=%VerifiedID_PinCodeLength% ^
    -e VerifiedID_PhotoClaimName=%VerifiedID_PhotoClaimName% ^
    -e VerifiedID_ClientName=%VerifiedID_ClientName% ^
    -e VerifiedID_Purpose=%VerifiedID_Purpose% ^
    java-aadvc-api-idtokenhint:latest
