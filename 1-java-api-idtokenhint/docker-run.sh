# get the tenant region scope and if it is an EU tenant, modify the endpoint 
export AADVC_TenantId=<YOUR-AAD-TENANDID>
export AADVC_ClientID=<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>
export AADVC_ApiKey=$(uuidgen)
export AADVC_ApiEndpoint=https://verifiedid.did.msidentity.com/v1.0/
export AADVC_CREDENTIALMANIFEST=<YOUR-CREDENTIAL-MANIFEST-URL>

docker run --rm -it -p 8080:8080 \
    -e AADVC_TenantId=$AADVC_TenantId \
    -e AADVC_ClientID=$AADVC_ClientID \
    -e AADVC_ClientSecret=<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS> \
    -e AADVC_CertName=<YOUR-CERTNAME> \
    -e AADVC_CertLocation=/usr/local/lib/aadappcert.crt \
    -e AADVC_CertKeyLocation=/usr/local/lib/aadappcert.key \
    -e AADVC_ApiEndpoint=$AADVC_ApiEndpoint \
    -e AADVC_ApiKey=$AADVC_ApiKey \
    -e AADVC_CREDENTIALMANIFEST=$AADVC_CREDENTIALMANIFEST \
    -e AADVC_ISSUERAUTHORITY=<YOUR-did:ion> \
    -e AADVC_VERIFIERAUTHORITY=<VCs-did:ion> \
    -e AADVC_PRESENTATIONFILE=/usr/local/lib/presentation_request_config.json \
    -e AADVC_ISSUANCEFILE=/usr/local/lib/issuance_request_config.json \
    java-aadvc-api-idtokenhint:latest
