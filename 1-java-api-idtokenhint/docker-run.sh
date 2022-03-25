# get the tenant region scope and if it is an EU tenant, modify the endpoint 
export AADVC_TenantId=<YOUR-AAD-TENANDID>
export AADVC_ClientID=<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>
export AADVC_ApiKey=$(uuidgen)

hostNameRegion=""
tenantRegionScope=$(curl https://login.microsoftonline.com/$AADVC_TenantId/v2.0/.well-known/openid-configuration | sed -e 's/[{}]/''/g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | grep tenant_region_scope | cut -d ':' -f 2 | sed -e 's/"//g')
if [[ "$tenantRegionScope" == "EU" ]]; then
    hostNameRegion=.eu
fi
export AADVC_ApiEndpoint="https://beta$hostNameRegion.did.msidentity.com/v1.0/{0}/verifiablecredentials/request"
export AADVC_CREDENTIALMANIFEST="https://beta$hostNameRegion.did.msidentity.com/v1.0/$AADVC_TenantId/verifiableCredential/contracts/<YOUR-CREDENTIAL-NAME>"

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
