#!/bin/bash

# This bash script is intended to run on Mac/Linux and requires Azure-CLI
# https://docs.microsoft.com/en-us/cli/azure/install-azure-cli-macos
clientCertificate="secret"
while getopts "t:c:" flag; do
    case "${flag}" in
        t) tenantId=${OPTARG};;
        c) clientCertificate=${OPTARG};;
    esac
done

acct=$(az account show)
if [[ -z $acct ]]; then 
    if [[ -z $tenantId ]]; then az login; else az login -t $tenantId; fi
fi

appName="Verifiable Credentials Java sample"
appShortName="vcjavasample"

# get things we need
echo "Getting things..."
tenantId=$(az account show --query "tenantId" -o tsv)
tenantDomainName=$(az ad signed-in-user show --query 'userPrincipalName' -o tsv | cut -d '@' -f 2)

# create the app and the sp
echo "Creating the app and the sp"
appId=$(az ad app create --display-name "$appName" --identifier-uris "https://$tenantDomainName/$appShortName" --query "appId" -o tsv)
spId=$(az ad sp create --id $appId)

# set the current user as app owner
echo "Assigning owner"
userId=$(az ad signed-in-user show --query objectId -o tsv)
az ad app owner add --id $appId --owner-object-id $userId

clientSecret=""
certSubject=""

if [[ $clientCertificate != cert* ]]; then
    # create a client_secret
    echo "Generating client_secret"
    clientSecret=$(az ad app credential reset --id $appId --credential-description "Default" --query "password" -o tsv)
else
    echo "Generating self-signed certificate"
    certSubject="CN=$appShortName"
    openssl genrsa -out ./aadappcert.pem 2048
    openssl pkcs8 -topk8 -inform PEM -outform DER -in ./aadappcert.pem -nocrypt -out ./aadappcert.key
    openssl req -new -key ./aadappcert.pem -out ./aadappcert.csr -subj "/$certSubject"
    openssl x509 -req -days 365 -in ./aadappcert.csr -signkey ./aadappcert.pem -out ./aadappcert.crt                
    az ad app credential reset --id $appId --cert "@~/aadappcert.crt" --append
fi

# add permissions
echo "Assigning permissions"
vcrsAppId=$(az ad sp list --display-name "Verifiable Credential Request Service" --query "[0].appId" -o tsv)
vcrsPermissionId=$(az ad sp list --display-name "Verifiable Credential Request Service" --query "[0].appRoles" | grep id | cut -d "\"" -f 4)
perm=$(az ad app permission add --id $appId --api $vcrsAppId --api-permissions $vcrsPermissionId=Role)

# updating the the run files to set envvars
apiKey=$(uuidgen)
declare -a arr=("run.sh" "docker-run.sh")
for i in "${arr[@]}"
do
    echo "Updating ../$i"
    sed -i -e "s/<YOUR-AAD-TENANDID>/$tenantId/g" ../$i
    sed -i -e "s/<YOUR-AAD-CLIENTID-FOR-KEYVAULT-ACCESS>/$appId/g" ../$i
    sed -i -e "s/<YOUR-AAD-CLIENTSECRET-FOR-KEYVAULT-ACCESS>/$clientSecret/g" ../$i
    sed -i -e "s/<YOUR-CERTNAME>/$certSubject/g" ../$i
    sed -i -e "s/<YOUR-APIKEY>/$apiKey/g" ../$i
done

# creating report for the user
clientPortalUrl="https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationMenuBlade/CallAnAPI/appId/$appId"

echo ""
echo "IMPORTANT: Please follow the instructions below to complete a few manual step(s) in the Azure portal"
echo "- For '$appName'"
echo "  - Navigate to $clientPortalUrl"
echo "  - Click on 'Grant admin consent for $tenantDomainName' in the API Permissions page"
