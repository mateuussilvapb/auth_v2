import { TestBed } from '@angular/core/testing';
import { MessageService as MessageServicePG } from 'primeng/api';
import type { Mock } from 'vitest';

import { MessageService } from './message.service';

describe('MessageService', () => {
  let service: MessageService;
  let primeMessageStub: { add: Mock };

  beforeEach(() => {
    primeMessageStub = { add: vi.fn() };

    TestBed.configureTestingModule({
      providers: [MessageService, { provide: MessageServicePG, useValue: primeMessageStub }],
    });

    service = TestBed.inject(MessageService);
  });

  it('showSuccess usa severity success e summary padrão', () => {
    service.showSuccess('Tenant criado.');

    expect(primeMessageStub.add).toHaveBeenCalledWith({
      severity: 'success',
      summary: 'Sucesso',
      detail: 'Tenant criado.',
      life: 5000,
    });
  });

  it('showError usa severity error e summary padrão', () => {
    service.showError('Falha ao salvar.');

    expect(primeMessageStub.add).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Erro',
      detail: 'Falha ao salvar.',
      life: 5000,
    });
  });

  it('aceita summary customizado', () => {
    service.showWarning('Sessão expira em breve.', 'Aviso');

    expect(primeMessageStub.add).toHaveBeenCalledWith({
      severity: 'warn',
      summary: 'Aviso',
      detail: 'Sessão expira em breve.',
      life: 5000,
    });
  });
});
