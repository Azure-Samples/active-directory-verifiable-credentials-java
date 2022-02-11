set AADVC_TenantId=<YOUR-AAD-TENANDID>
set AADVC_ClientID=<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>
set AADVC_ClientSecret=<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS>
set AADVC_CertName=<YOUR-CERTNAME>
set AADVC_CertLocation=%cd%\AppCreationScripts\aadappcert.crt
set AADVC_CertKeyLocation=%cd%\AppCreationScripts\aadappcert.key
set AADVC_ApiKey=<YOUR-APIKEY>
set AADVC_CREDENTIALMANIFEST=<YOUR-MANIFEST-URL>
set AADVC_ISSUERAUTHORITY=<YOUR-did:ion>
set AADVC_VERIFIERAUTHORITY=<VCs-did:ion>
set AADVC_PRESENTATIONFILE=%cd%\presentation_request_config.json
set AADVC_ISSUANCEFILE=%cd%\issuance_request_config.json

java -jar .\target\java-aadvc-api-idtokenhint-0.0.1-SNAPSHOT.jar
