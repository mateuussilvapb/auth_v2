import { Injectable, signal } from '@angular/core';

import { decryptJson, encryptJson } from '../util/crypto';

export interface SelectedTenant {
  id: string;
  code: string;
}

const STORAGE_KEY = 'console.tenant-context';

/**
 * Contexto de tenant selecionado pelo platform admin no console (guia de estilo — decisão de
 * produto 2026-08-29). Guarda só `id`/`code` (não o objeto completo) em `localStorage`,
 * cifrado com AES-GCM cuja chave deriva do access token corrente — sem o token válido (ex.:
 * após logout, quando o token some), o valor gravado deixa de ser decifrável.
 * <p>
 * Só é limpo explicitamente em dois pontos: logout ({@link ConsoleAuthService}) e troca de
 * tenant pelo usuário (topbar) — nunca implicitamente por expiração de sessão, para que o
 * guard consiga distinguir "sem tenant selecionado" de "sessão expirada".
 */
@Injectable({ providedIn: 'root' })
export class TenantContextService {
  readonly selectedTenant = signal<SelectedTenant | null>(null);

  async restore(accessToken: string): Promise<SelectedTenant | null> {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      this.selectedTenant.set(null);
      return null;
    }

    try {
      const tenant = await decryptJson<SelectedTenant>(raw, accessToken);
      this.selectedTenant.set(tenant);
      return tenant;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      this.selectedTenant.set(null);
      return null;
    }
  }

  async select(tenant: SelectedTenant, accessToken: string): Promise<void> {
    const raw = await encryptJson(tenant, accessToken);
    localStorage.setItem(STORAGE_KEY, raw);
    this.selectedTenant.set(tenant);
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.selectedTenant.set(null);
  }
}
