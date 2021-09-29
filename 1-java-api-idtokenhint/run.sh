#!/bin/bash
export AADVC_TenantId="<YOUR-AAD-TENANDID>"
export AADVC_ClientID="<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>"
export AADVC_ClientSecret="<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS>"
export AADVC_CREDENTIALMANIFEST="<YOUR-MANIFEST-URL>"
export AADVC_ISSUERAUTHORITY="<YOUR-did:ion>"
export AADVC_VERIFIERAUTHORITY="<VCs-did:ion>"
export AADVC_PRESENTATIONFILE="$(pwd)/presentation_request_config.json"
export AADVC_ISSUANCEFILE="$(pwd)/issuance_request_config.json"

java -jar ./target/java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar
