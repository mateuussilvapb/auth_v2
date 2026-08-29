// Produção: o Angular é servido pelo mesmo nginx/domínio do auth server (seção 11 do
// plano) — URL relativa, sem CORS.
export const environment = {
  production: true,
  apiBaseUrl: '',
  // Deve bater com authserver.console-client.client-id (application.yml) — client OAuth2
  // PKCE estático do console administrativo (seção 2.2/D6, RegisteredClientRepositoryConfig).
  consoleClientId: 'console',
};
