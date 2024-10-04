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
export VerifiedID__HashPinCode=false
export VerifiedID__PhotoClaimName=photo
export VerifiedID__UseFaceCheck=false

docker run --rm -it -p 8080:8080 \
    -e AzureAD__TenantId=$AzureAD_TenantId \
    -e AzureAD__ClientID=$AzureAD_ClientId \
    -e AzureAD__ClientSecret=$AzureAD_ClientSecret \
    -e AzureAD__CertName=$AzureAD_CertName \
    -e AzureAD__CertLocation=/usr/local/lib/aadappcert.crt \
    -e AzureAD__CertKeyLocation=/usr/local/lib/aadappcert.key \
    -e VerifiedID__ApiKey=$VerifiedID_ApiKey \
    -e VerifiedID__ApiEndpoint=$VerifiedID_ApiEndpoint \
    -e VerifiedID__CREDENTIALMANIFEST=$VerifiedID_CREDENTIALMANIFEST \
    -e VerifiedID__DIDAUTHORITY=$VerifiedID_DIDAUTHORITY \
    -e VerifiedID__CREDENTIALTYPE=$VerifiedID_CREDENTIALTYPE \
    -e VerifiedID__PinCodeLength=$VerifiedID_PinCodeLength \
    -e VerifiedID__HashPinCode=$VerifiedID_HashPinCode \
    -e VerifiedID__PhotoClaimName=$VerifiedID_PhotoClaimName \
    -e VerifiedID__ClientName=$VerifiedID_ClientName \
    -e VerifiedID__Purpose=$VerifiedID_Purpose \
    java-aadvc-api-idtokenhint:latest
