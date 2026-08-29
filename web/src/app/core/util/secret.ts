/**
 * Gera um client secret aleatório forte, inteiramente no navegador (guia de estilo, seção
 * 5.5) — o admin nunca digita o secret; o backend só recebe o valor final para hashear
 * (`ClientSecret.fromPlainText`, custo BCrypt 12) e nunca o devolve depois deste momento.
 */
export function generateClientSecret(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCodePoint(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
