import { generateClientSecret } from './secret';

describe('generateClientSecret', () => {
  it('gera um valor com pelo menos 32 caracteres e sem padding base64', () => {
    const secret = generateClientSecret();

    expect(secret.length).toBeGreaterThanOrEqual(32);
    expect(secret).not.toContain('=');
    expect(secret).not.toContain('+');
    expect(secret).not.toContain('/');
  });

  it('gera valores diferentes a cada chamada', () => {
    const a = generateClientSecret();
    const b = generateClientSecret();

    expect(a).not.toBe(b);
  });
});
