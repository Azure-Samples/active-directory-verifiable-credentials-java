# get the tenant region scope and if it is an EU tenant, modify the endpoint 
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

docker run --rm -it -p 8080:8080 \
    -e AzureAD_TenantId=$AzureAD_TenantId \
    -e AzureAD_ClientID=$AzureAD_ClientId \
    -e AzureAD_ClientSecret=$AzureAD_ClientSecret \
    -e AzureAD_CertName=$AzureAD_CertName \
    -e AzureAD_CertLocation=/usr/local/lib/aadappcert.crt \
    -e AzureAD_CertKeyLocation=/usr/local/lib/aadappcert.key \
    -e VerifiedID_ApiKey=$VerifiedID_ApiKey \
    -e VerifiedID_ApiEndpoint=$VerifiedID_ApiEndpoint \
    -e VerifiedID_CREDENTIALMANIFEST=$VerifiedID_CREDENTIALMANIFEST \
    -e VerifiedID_DIDAUTHORITY=$VerifiedID_DIDAUTHORITY \
    -e VerifiedID_CREDENTIALTYPE=$VerifiedID_CREDENTIALTYPE \
    -e VerifiedID_PinCodeLength=$VerifiedID_PinCodeLength \
    -e VerifiedID_PhotoClaimName=$VerifiedID_PhotoClaimName \
    -e VerifiedID_ClientName=$VerifiedID_ClientName \
    -e VerifiedID_Purpose=$VerifiedID_Purpose \
    java-aadvc-api-idtokenhint:latest
