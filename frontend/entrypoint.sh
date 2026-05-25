#!/bin/sh

# Configurar valores por defecto en caso de que falten en el entorno
AZURE_TENANT_ID=${AZURE_TENANT_ID:-common}
MSAL_REDIRECT_URI=${MSAL_REDIRECT_URI:-http://localhost}
OAUTH_REDIRECT_URI=${OAUTH_REDIRECT_URI:-${MSAL_REDIRECT_URI}/oauth/callback}
API_SCOPE=${API_SCOPE:-api://${AZURE_LOGIN_CLIENT_ID}/access_as_user}

echo "Inyectando variables de entorno en archivos Angular estáticos..."
echo "  - AZURE_LOGIN_CLIENT_ID: $AZURE_LOGIN_CLIENT_ID"
echo "  - AZURE_TENANT_ID: $AZURE_TENANT_ID"
echo "  - MSAL_REDIRECT_URI: $MSAL_REDIRECT_URI"
echo "  - OAUTH_REDIRECT_URI: $OAUTH_REDIRECT_URI"
echo "  - API_SCOPE: $API_SCOPE"

# Reemplazar los placeholders en todos los archivos compilados .js dentro de Nginx
find /usr/share/nginx/html -type f -name "*.js" -exec sed -i \
  -e "s|AZURE_LOGIN_CLIENT_ID_PLACEHOLDER|$AZURE_LOGIN_CLIENT_ID|g" \
  -e "s|AZURE_TENANT_ID_PLACEHOLDER|$AZURE_TENANT_ID|g" \
  -e "s|MSAL_REDIRECT_URI_PLACEHOLDER|$MSAL_REDIRECT_URI|g" \
  -e "s|OAUTH_REDIRECT_URI_PLACEHOLDER|$OAUTH_REDIRECT_URI|g" \
  -e "s|API_SCOPE_PLACEHOLDER|$API_SCOPE|g" \
  {} +

echo "Inyección de variables completada. Iniciando Nginx..."

# Ejecutar el comando por defecto (Nginx)
exec "$@"
