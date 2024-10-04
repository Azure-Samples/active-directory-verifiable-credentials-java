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

docker run --rm -it -p 8080:8080 ^
    -e AzureAD__TenantId=%AzureAD__TenantId% ^
    -e AzureAD__ClientID=%AzureAD__ClientId% ^
    -e AzureAD__ClientSecret=%AzureAD__ClientSecret% ^
    -e AzureAD__CertName=%AzureAD__CertName% ^
    -e AzureAD__CertLocation=/usr/local/lib/aadappcert.crt ^
    -e AzureAD__CertKeyLocation=/usr/local/lib/aadappcert.key ^
    -e VerifiedID__ApiKey=%VerifiedID__ApiKey% ^
    -e VerifiedID__ApiEndpoint=%VerifiedID__ApiEndpoint% ^
    -e VerifiedID__CREDENTIALMANIFEST=%VerifiedID__CREDENTIALMANIFEST% ^
    -e VerifiedID__DIDAUTHORITY=%VerifiedID__DIDAUTHORITY% ^
    -e VerifiedID__CREDENTIALTYPE=%VerifiedID__CREDENTIALTYPE% ^
    -e VerifiedID__PinCodeLength=%VerifiedID__PinCodeLength% ^
    -e VerifiedID__PinCodeLength=%VerifiedID__HashPinCode% ^
    -e VerifiedID__PhotoClaimName=%VerifiedID__PhotoClaimName% ^
    -e VerifiedID__ClientName=%VerifiedID__ClientName% ^
    -e VerifiedID__Purpose=%VerifiedID__Purpose% ^
    java-aadvc-api-idtokenhint:latest
