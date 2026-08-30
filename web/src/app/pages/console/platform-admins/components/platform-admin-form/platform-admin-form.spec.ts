import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { PlatformAdminForm } from './platform-admin-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { MessageService } from '../../../../../shared/services/message.service';
import { PlatformAdminResponse } from '../../../../../core/models/admin-api.models';

describe('PlatformAdminForm', () => {
  let fixture: ComponentFixture<PlatformAdminForm>;
  let adminApiStub: { createPlatformAdmin: Mock };
  let router: Router;
  let messageService: MessageService;

  const admin: PlatformAdminResponse = { id: '1', username: 'novo_admin', email: 'novo@example.com', name: 'Novo Admin', status: 'ACTIVE' };

  beforeEach(async () => {
    adminApiStub = { createPlatformAdmin: vi.fn().mockReturnValue(of(admin)) };

    await TestBed.configureTestingModule({
      imports: [PlatformAdminForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map(), data: { formMode: 'cadastro' } } } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    messageService = TestBed.inject(MessageService);

    fixture = TestBed.createComponent(PlatformAdminForm);
    fixture.detectChanges();
  });

  it('cria o platform admin e navega para a listagem', async () => {
    fixture.componentInstance.form.setValue({
      username: 'novo_admin',
      name: 'Novo Admin',
      email: 'novo@example.com',
      password: 'senhaForte123',
    });

    await fixture.componentInstance.submit();

    expect(adminApiStub.createPlatformAdmin).toHaveBeenCalledWith({
      username: 'novo_admin',
      email: 'novo@example.com',
      password: 'senhaForte123',
      name: 'Novo Admin',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/console/platform-admins']);
  });

  it('não submete quando o formulário é inválido', async () => {
    await fixture.componentInstance.submit();
    expect(adminApiStub.createPlatformAdmin).not.toHaveBeenCalled();
  });

  it('mostra a mensagem de erro da API quando falha ao salvar', async () => {
    adminApiStub.createPlatformAdmin.mockReturnValue(throwError(() => ({ error: { message: 'username já em uso.' } })));
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture.componentInstance.form.setValue({
      username: 'novo_admin',
      name: 'Novo Admin',
      email: 'novo@example.com',
      password: 'senhaForte123',
    });
    await fixture.componentInstance.submit();

    expect(errorSpy).toHaveBeenCalledWith('username já em uso.');
  });

  it('cancelar navega para a listagem', () => {
    fixture.componentInstance.cancel();
    expect(router.navigate).toHaveBeenCalledWith(['/console/platform-admins']);
  });
});
