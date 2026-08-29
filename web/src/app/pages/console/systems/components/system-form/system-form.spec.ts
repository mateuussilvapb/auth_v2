import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { SystemForm } from './system-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { MessageService } from '../../../../../shared/services/message.service';
import { SystemResponse } from '../../../../../core/models/admin-api.models';

describe('SystemForm', () => {
  let fixture: ComponentFixture<SystemForm>;
  let adminApiStub: {
    createSystem: Mock;
    updateSystem: Mock;
    addRedirectUri: Mock;
    removeRedirectUri: Mock;
    rotateSecret: Mock;
  };
  let router: Router;
  let messageService: MessageService;

  const system: SystemResponse = {
    id: '1',
    clientId: 'CRM_ACME',
    name: 'CRM',
    status: 'ACTIVE',
    publicClient: false,
    redirectUris: ['https://crm.acme.com/callback'],
    thirdParty: false,
  };

  function newAdminApiStub() {
    return {
      createSystem: vi.fn().mockReturnValue(of(system)),
      updateSystem: vi.fn().mockReturnValue(of(system)),
      addRedirectUri: vi.fn().mockReturnValue(of({ ...system, redirectUris: [...system.redirectUris, 'https://crm.acme.com/dev'] })),
      removeRedirectUri: vi.fn().mockReturnValue(of({ ...system, redirectUris: [] })),
      rotateSecret: vi.fn().mockReturnValue(of(system)),
    };
  }

  describe('modo criação (rota)', () => {
    beforeEach(async () => {
      adminApiStub = newAdminApiStub();

      await TestBed.configureTestingModule({
        imports: [SystemForm],
        providers: [
          provideRouter([]),
          ConfirmationService,
          DialogService,
          MessageServicePG,
          { provide: AdminApiService, useValue: adminApiStub },
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: new Map([['tenantId', 't1']]), data: { formMode: 'cadastro' } } },
          },
        ],
      }).compileComponents();

      router = TestBed.inject(Router);
      vi.spyOn(router, 'navigate').mockResolvedValue(true);
      messageService = TestBed.inject(MessageService);

      fixture = TestBed.createComponent(SystemForm);
      fixture.detectChanges();
    });

    it('inicia com um campo de redirect URI e sem secret gerado (público por padrão)', () => {
      expect(fixture.componentInstance.redirectUris()).toEqual(['']);
      expect(fixture.componentInstance.generatedSecret()).toBeNull();
    });

    it('gera um secret ao desmarcar client público e limpa ao marcar de novo', () => {
      const component = fixture.componentInstance;
      component.form.get('publicClient')?.setValue(false);
      component.onPublicClientChange();
      expect(component.generatedSecret()).not.toBeNull();

      component.form.get('publicClient')?.setValue(true);
      component.onPublicClientChange();
      expect(component.generatedSecret()).toBeNull();
    });

    it('cria o sistema com o secret gerado e navega para a listagem', async () => {
      const component = fixture.componentInstance;
      component.form.setValue({ clientId: 'ERP_ACME', name: 'ERP', publicClient: false, thirdParty: false });
      component.onPublicClientChange();
      component.updateRedirectUriField(0, 'https://erp.acme.com/callback');

      await component.submit();

      expect(adminApiStub.createSystem).toHaveBeenCalledWith('t1', {
        clientId: 'ERP_ACME',
        name: 'ERP',
        publicClient: false,
        clientSecret: component.generatedSecret(),
        initialRedirectUris: ['https://erp.acme.com/callback'],
        thirdParty: false,
      });
      expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'systems']);
    });

    it('não submete sem nenhuma redirect URI preenchida', async () => {
      const component = fixture.componentInstance;
      const errorSpy = vi.spyOn(messageService, 'showError');
      component.form.setValue({ clientId: 'ERP_ACME', name: 'ERP', publicClient: true, thirdParty: false });

      await component.submit();

      expect(adminApiStub.createSystem).not.toHaveBeenCalled();
      expect(errorSpy).toHaveBeenCalledWith('Informe ao menos uma redirect URI.');
    });

    it('mostra o erro da API quando a criação falha', async () => {
      adminApiStub.createSystem.mockReturnValue(throwError(() => ({ error: { message: 'client_id já em uso.' } })));
      const errorSpy = vi.spyOn(messageService, 'showError');
      const component = fixture.componentInstance;
      component.form.setValue({ clientId: 'ERP_ACME', name: 'ERP', publicClient: true, thirdParty: false });
      component.updateRedirectUriField(0, 'https://erp.acme.com/callback');

      await component.submit();

      expect(errorSpy).toHaveBeenCalledWith('client_id já em uso.');
    });
  });

  describe('modo edição (diálogo)', () => {
    let closeSpy: Mock;

    beforeEach(async () => {
      adminApiStub = newAdminApiStub();
      closeSpy = vi.fn();
      const dialogRefStub = { close: closeSpy } as unknown as DynamicDialogRef;
      const dialogServiceStub = {
        getInstance: vi.fn().mockReturnValue({ data: { mode: 'edicao', id: '1', valoresIniciais: system } }),
      };

      await TestBed.configureTestingModule({
        imports: [SystemForm],
        providers: [
          provideRouter([]),
          ConfirmationService,
          MessageServicePG,
          { provide: DialogService, useValue: dialogServiceStub },
          { provide: DynamicDialogRef, useValue: dialogRefStub },
          { provide: AdminApiService, useValue: adminApiStub },
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map(), data: {} } } },
        ],
      }).compileComponents();

      messageService = TestBed.inject(MessageService);
      fixture = TestBed.createComponent(SystemForm);
      fixture.detectChanges();
    });

    it('carrega os dados iniciais e desabilita campos imutáveis', () => {
      const component = fixture.componentInstance;
      expect(component.isEditMode()).toBe(true);
      expect(component.form.getRawValue()).toEqual({
        clientId: 'CRM_ACME',
        name: 'CRM',
        publicClient: false,
        thirdParty: false,
      });
      expect(component.form.get('clientId')?.disabled).toBe(true);
      expect(component.form.get('publicClient')?.disabled).toBe(true);
      expect(component.form.get('thirdParty')?.disabled).toBe(true);
      expect(component.redirectUris()).toEqual(['https://crm.acme.com/callback']);
    });

    it('atualiza só o nome ao submeter', async () => {
      const component = fixture.componentInstance;
      component.form.get('name')?.setValue('CRM Corp');

      await component.submit();

      expect(adminApiStub.updateSystem).toHaveBeenCalledWith('1', { name: 'CRM Corp' });
      expect(closeSpy).toHaveBeenCalledWith(true);
    });

    it('adiciona uma redirect URI', async () => {
      const component = fixture.componentInstance;
      component.newRedirectUri.set('https://crm.acme.com/dev');

      await component.addRedirectUri();

      expect(adminApiStub.addRedirectUri).toHaveBeenCalledWith('1', { uri: 'https://crm.acme.com/dev' });
      expect(component.redirectUris()).toContain('https://crm.acme.com/dev');
      expect(component.newRedirectUri()).toBe('');
    });

    it('pede confirmação antes de remover uma redirect URI', () => {
      const confirmationService = TestBed.inject(ConfirmationService);
      const confirmSpy = vi.spyOn(confirmationService, 'confirm');
      const component = fixture.componentInstance;

      component.removeRedirectUri('https://crm.acme.com/callback');

      expect(confirmSpy).toHaveBeenCalled();
      expect(adminApiStub.removeRedirectUri).not.toHaveBeenCalled();

      confirmSpy.mock.calls[0][0].accept?.();

      expect(adminApiStub.removeRedirectUri).toHaveBeenCalledWith('1', 'https://crm.acme.com/callback');
    });

    it('pede confirmação antes de rotacionar o secret e exibe o novo valor', async () => {
      const confirmationService = TestBed.inject(ConfirmationService);
      const confirmSpy = vi.spyOn(confirmationService, 'confirm');
      const component = fixture.componentInstance;

      component.rotateSecret();

      expect(confirmSpy).toHaveBeenCalled();
      expect(adminApiStub.rotateSecret).not.toHaveBeenCalled();

      confirmSpy.mock.calls[0][0].accept?.();
      await Promise.resolve();

      expect(adminApiStub.rotateSecret).toHaveBeenCalledWith('1', { newSecret: expect.any(String) });
      expect(component.rotatedSecret()).not.toBeNull();
    });
  });
});
