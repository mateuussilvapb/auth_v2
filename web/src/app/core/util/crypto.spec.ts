import { decryptJson, encryptJson } from './crypto';

describe('crypto', () => {
  it('cifra e decifra mantendo o valor original', async () => {
    const value = { id: 't1', code: 'acme' };

    const raw = await encryptJson(value, 'segredo-1');
    const decrypted = await decryptJson<typeof value>(raw, 'segredo-1');

    expect(decrypted).toEqual(value);
  });

  it('produz um payload que não contém o valor em texto plano', async () => {
    const raw = await encryptJson({ code: 'acme-secreto' }, 'segredo-1');

    expect(raw).not.toContain('acme-secreto');
  });

  it('falha ao decifrar com um segredo diferente do usado para cifrar', async () => {
    const raw = await encryptJson({ code: 'acme' }, 'segredo-1');

    await expect(decryptJson(raw, 'segredo-2')).rejects.toThrow();
  });
});
