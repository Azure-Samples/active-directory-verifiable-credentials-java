[CmdletBinding()]
param(
    [Parameter(Mandatory=$False, HelpMessage='Tenant ID (This is a GUID which represents the "Directory ID" of the AzureAD tenant into which you want to create the apps')][string] $tenantId,
    [Parameter(Mandatory=$False, HelpMessage='Switch if you want to generate a client_secret for the app')][switch]$ClientSecret = $False,
    [Parameter(Mandatory=$False, HelpMessage='Switch if you want to generate a client certificate for the app')][switch]$ClientCertificate = $False
)

# Pre-requisites
if ($null -eq (Get-Module -ListAvailable -Name "Az.Accounts")) {  
    Install-Module -Name "Az.Accounts" -Scope CurrentUser 
}
if ($null -eq (Get-Module -ListAvailable -Name "Az.Resources")) {  
    Install-Module "Az.Resources" -Scope CurrentUser 
}
Import-Module -Name "Az.Accounts"
Import-Module -Name "Az.Resources"

$isMacLinux = ($env:PATH -imatch "/usr/bin" )
# default to client_secret
if ( !$ClientSecret -and !$ClientCertificate ) { $ClientSecret = $True }

Function UpdateTextFile([string] $configFilePath, [System.Collections.HashTable] $dictionary, [string] $delimiter)
{
    Write-Host "Updating the sample code $configFilePath"
    $lines = Get-Content $configFilePath
    for( $index = 0; $index -lt $lines.Length; $index++ ) {
        foreach($key in $dictionary.Keys) {
            if ($lines[$index].Contains($key)) {
                if ( ($pos = $lines[$index].IndexOf('=')) -ige 0 ) {
                    $value = $dictionary[$key]
                    $lines[$index] = $lines[$index].Substring(0, $pos+1) + "$value$delimiter"
                }
            }
        }
    }
    Set-Content -Path $configFilePath -Value $lines -Force
}

$ctx = Get-AzContext
if ( !$ctx ) {
    if ( $tenantId ) {
        $creds = Connect-AzAccount -TenantId $tenantId
    } else {
        $creds = Connect-AzAccount
        $tenantId = $creds.Context.Account.Tenants[0]
    }
} else {
    if ( $TenantId -and $TenantId -ne $ctx.Tenant.TenantId ) {
        write-error "You are targeting tenant $tenantId but you are signed in to tennant $($ctx.Tenant.TenantId)"
    }    
    $tenantId = $ctx.Tenant.TenantId
}

$tenant = Get-AzTenant
$tenantDomainName =  ($tenant | Where { $_.Id -eq $tenantId }).Domains[0]
$tenantName =  ($tenant | Where { $_.Id -eq $tenantId }).Name

# Create the client AAD application
$appName = "Verifiable Credentials Java sample"
$appShortName = "vcjavasample"
$clientAadApplication = Get-AzADApplication -DisplayName $appName
if ($null -ne $clientAadApplication) {
    Write-Host "App $appName ($($clientAadApplication.AppId)) already exists"
    exit
}
Write-Host "Creating the AAD application ($appName)"
$clientAadApplication = New-AzADApplication -DisplayName $appName `
                                            -IdentifierUris "https://$tenantDomainName/$appShortName" 
$clientServicePrincipal = ($clientAadApplication | New-AzADServicePrincipal)
Write-Host "AppId $($clientAadApplication.AppId)"
# Generate a certificate or client_secret
$client_secret = ""
$certSubject = ""
if ( $ClientCertificate ) {
    $certSubject = "CN=$appShortName"
    Write-Host "Generating self-signed certificate $certSubject"
    # generating a self signed certificate is done differently on Windows vs Mac/Linux
    if ( $False -eq $isMacLinux ) {
        if (!(Test-Path ".\aadappcert.crt")) {
            write-warning "Certificate file 'aadappcert.crt' missing. You need to manually generate it and upload it - see README.md for details"
        }
        $certData = Get-Content (Resolve-Path ".\aadappcert.crt").Path | Out-String
        $certData =[Convert]::ToBase64String( [System.Text.Encoding]::Ascii.GetBytes($certData) ) 
    } else { # Mac/Linux - generate the self-signed certificate via openssl
        & openssl genrsa -out ./aadappcert.pem 2048
        & openssl pkcs8 -topk8 -inform PEM -outform DER -in ./aadappcert.pem -nocrypt -out ./aadappcert.key
        & openssl req -new -key ./aadappcert.pem -out ./aadappcert.csr -subj "/$certSubject"
        & openssl x509 -req -days 365 -in ./aadappcert.csr -signkey ./aadappcert.pem -out ./aadappcert.crt                
        $certData = Get-Content ./aadappcert.crt | Out-String
        $certData =[Convert]::ToBase64String( [System.Text.Encoding]::Ascii.GetBytes($certData) ) 
    }
    $clientAadApplication | New-AzADAppCredential -CertValue $certData        
}
if ( $ClientSecret ) {
    # Get a 1 year client secret for the client Application
    Write-Host "Generating client_secret"
    $fromDate = [DateTime]::Now
    $appCreds = ($clientAadApplication | New-AzADAppCredential -StartDate $fromDate -EndDate $fromDate.AddYears(1) )
    $client_secret = $appCreds.SecretText
}

# Add Required Resources Access (from 'client' to 'Verifiable Credential Request Service')
$permissionName = "VerifiableCredential.Create.All"
Write-Host "Adding API Permission $permissionName"
$spVCSR = Get-AzADServicePrincipal -DisplayName "Verifiable Credentials Service Request"
$permissionId = ($spVCSR.AppRole | where {$_.DisplayName -eq $permissionName}).Id
Add-AzADAppPermission -ObjectId $clientAadApplication.Id -ApiId $spVCSR.AppId -PermissionId $permissionId -Type "Role"

Write-Host "Done creating the client application ($appName)"

# URL of the AAD application in the Azure portal
# Future? $clientPortalUrl = "https://portal.azure.com/#@"+$tenantName+"/blade/Microsoft_AAD_RegisteredApps/ApplicationMenuBlade/Overview/appId/"+$clientAadApplication.AppId+"/objectId/"+$clientAadApplication.ObjectId+"/isMSAApp/"
$clientPortalUrl = "https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationMenuBlade/CallAnAPI/appId/"+$clientAadApplication.AppId+"/objectId/"+$clientAadApplication.ObjectId+"/isMSAApp/"

# create the HTML file with deployment details
Set-Content -Value "<html><body><table>" -Path createdApps.html
Add-Content -Value "<thead><tr><th>Application</th><th>AppId</th><th>Url in the Azure portal</th></tr></thead><tbody>" -Path createdApps.html
Add-Content -Value "<tr><td>$appName</td><td>$($clientAadApplication.AppId)</td><td><a href='$clientPortalUrl'>$appName</a></td></tr>" -Path createdApps.html
Add-Content -Value "</tbody></table></body></html>" -Path createdApps.html  

# Update config file for the app
$dictionary = @{ "AADVC_TenantId" = $tenantId; "AADVC_ClientID" = $clientAadApplication.AppId; "AADVC_ClientSecret" = $client_secret; 
    "AADVC_CertName" = $certSubject; "AADVC_CertLocation" = ""; "AADVC_CertKeyLocation" = "";
    "AADVC_ApiKey" = (new-guid).Guid
};
$configPath = $pwd.Path + "$([IO.Path]::DirectorySeparatorChar)..$([IO.Path]::DirectorySeparatorChar)"

# run.cmd is a Windows batch script and we use the %cd% environment variable to locate the certs
$dictionary["AADVC_CertLocation"] = "%cd%\AppCreationScripts\aadappcert.crt"
$dictionary["AADVC_CertKeyLocation"] = "%cd%\AppCreationScripts\aadappcert.key"
if ( !$ClientSecret ) {
    $dictionary["AADVC_ClientSecret"] = "`"`"" # no value in CMD SET statement removes the envvar. We need a blank one
}
UpdateTextFile -configFilePath "$($configPath)run.cmd" -dictionary $dictionary -delimiter ""

# run.sh is a Mac/Linux bash script and we use $pwd to locate the certs
$dictionary["AADVC_CertLocation"] = "`$(pwd)/AppCreationScripts/aadappcert.crt"
$dictionary["AADVC_CertKeyLocation"] = "`$(pwd)/AppCreationScripts/aadappcert.key"
UpdateTextFile -configFilePath "$($configPath)run.sh" -dictionary $dictionary -delimiter ""

# Fixed path as per Dockerfile
$dictionary["AADVC_CertLocation"] = "/usr/local/lib/aadappcert.crt"
$dictionary["AADVC_CertKeyLocation"] = "/usr/local/lib/aadappcert.key"
UpdateTextFile -configFilePath "$($configPath)docker-run.cmd" -dictionary $dictionary -delimiter " ^"
UpdateTextFile -configFilePath "$($configPath)docker-run.sh" -dictionary $dictionary -delimiter " \"

Write-Host ""
Write-Host "IMPORTANT: Please follow the instructions below to complete a few manual step(s) in the Azure portal":
Write-Host "- For '$appName'"
Write-Host "  - Navigate to $clientPortalUrl"
Write-Host "  - Click on 'Grant admin consent for $tenantName' in the API Permissions page"
