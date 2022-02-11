docker run --rm -it -p 8080:8080 ^
    -e AADVC_TenantId=<YOUR-AAD-TENANDID> ^
    -e AADVC_ClientID=<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS> ^
    -e AADVC_ClientSecret=<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS> ^
    -e AADVC_CertName=<YOUR-CERTNAME> ^
    -e AADVC_CertLocation=/usr/local/lib/aadappcert.crt ^
    -e AADVC_CertKeyLocation=/usr/local/lib/aadappcert.key ^
    -e AADVC_ApiKey=<YOUR-APIKEY> ^
    -e AADVC_CREDENTIALMANIFEST=<YOUR-MANIFEST-URL> ^
    -e AADVC_ISSUERAUTHORITY=<YOUR-did:ion> ^
    -e AADVC_VERIFIERAUTHORITY=<VCs-did:ion> ^
    -e AADVC_PRESENTATIONFILE=/usr/local/lib/presentation_request_fawltytowers2_campuspass.json ^
    -e AADVC_ISSUANCEFILE=/usr/local/lib/issuance_request_fawltytowers2_campuspass.json ^
    java-aadvc-api-idtokenhint:latest
