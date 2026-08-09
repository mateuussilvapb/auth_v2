// Desenvolvimento: `ng serve` roda em localhost:4200, origem diferente do backend
// (localhost:8080) — authserver.cors.allowed-origins libera essa origem em
// application-dev.yml (Fase 8 do plano).
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
};
