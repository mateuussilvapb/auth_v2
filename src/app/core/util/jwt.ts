/** Decodifica o payload de um JWT sem validar assinatura — só para exibição na UI. */
export function decodeJwtPayload(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  return JSON.parse(atob(padded));
}
