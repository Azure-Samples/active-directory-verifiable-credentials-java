#!/bin/bash
export AADVC_TenantId=<YOUR-AAD-TENANDID>
export AADVC_ClientID=<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>
export AADVC_ClientSecret=<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS>
export AADVC_CertName=<YOUR-CERTNAME>
export AADVC_CertLocation=$(pwd)/AppCreationScripts/aadappcert.crt
export AADVC_CertKeyLocation=$(pwd)/AppCreationScripts/aadappcert.key
export AADVC_ApiKey=<YOUR-APIKEY>
#export AADVC_CREDENTIALMANIFEST=<YOUR-MANIFEST-URL>
export AADVC_ISSUERAUTHORITY=<YOUR-did:ion>
export AADVC_VERIFIERAUTHORITY=<VCs-did:ion>
export AADVC_PRESENTATIONFILE=$(pwd)/presentation_request_config.json
export AADVC_ISSUANCEFILE=$(pwd)/issuance_request_config.json

# get the tenant region scope and if it is an EU tenant, modify the endpoint 
hostNameRegion=""
tenantRegionScope=$(curl https://login.microsoftonline.com/$AADVC_TenantId/v2.0/.well-known/openid-configuration | sed -e 's/[{}]/''/g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | grep tenant_region_scope | cut -d ':' -f 2 | sed -e 's/"//g')
if [[ "$tenantRegionScope" == "EU" ]]; then
    hostNameRegion=.eu
fi
export AADVC_ApiEndpoint="https://beta$hostNameRegion.did.msidentity.com/v1.0/{0}/verifiablecredentials/request"
export AADVC_CREDENTIALMANIFEST="https://beta$hostNameRegion.did.msidentity.com/v1.0/$AADVC_TenantId/verifiableCredential/contracts/<YOUR-NAME>"

java -jar ./target/java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar
