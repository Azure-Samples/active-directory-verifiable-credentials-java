param( [Parameter(Mandatory=$True)][Alias('t')][string]$TenantId )
    $tenantMetadata = invoke-restmethod -Uri "https://login.microsoftonline.com/$tenantId/v2.0/.well-known/openid-configuration"
    return $tenantMetadata.tenant_region_scope # WW, NA, EU, AF, AS, OC, SA
