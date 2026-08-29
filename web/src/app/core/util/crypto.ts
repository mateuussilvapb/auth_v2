interface EncryptedPayload {
  iv: string;
  data: string;
}

/**
 * AES-GCM com chave derivada (SHA-256) de um segredo fornecido pelo chamador — usado para
 * cifrar dados de baixo risco em localStorage (guia de estilo, seção 6). Não é um cofre: o
 * segredo (access token) também vive no cliente, então a barreira é contra leitura casual do
 * valor em repouso, não contra um atacante com execução de JS na página.
 */
async function deriveKey(secret: string): Promise<CryptoKey> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(secret));
  return crypto.subtle.importKey('raw', digest, 'AES-GCM', false, ['encrypt', 'decrypt']);
}

function toBase64(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCodePoint(byte);
  }
  return btoa(binary);
}

function fromBase64(base64: string): ArrayBuffer {
  return Uint8Array.from(atob(base64), (char) => char.codePointAt(0) ?? 0).buffer as ArrayBuffer;
}

export async function encryptJson<T>(value: T, secret: string): Promise<string> {
  const key = await deriveKey(secret);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    new TextEncoder().encode(JSON.stringify(value)),
  );
  const payload: EncryptedPayload = { iv: toBase64(iv), data: toBase64(new Uint8Array(ciphertext)) };
  return JSON.stringify(payload);
}

export async function decryptJson<T>(raw: string, secret: string): Promise<T> {
  const payload = JSON.parse(raw) as EncryptedPayload;
  const key = await deriveKey(secret);
  const plaintext = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: fromBase64(payload.iv) },
    key,
    fromBase64(payload.data),
  );
  return JSON.parse(new TextDecoder().decode(plaintext)) as T;
}
