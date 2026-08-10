import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { ListBase } from './list-base';

class TestListBase extends ListBase {}

describe('ListBase', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), ConfirmationService, DialogService, MessageServicePG],
    });
  });

  function create(): TestListBase {
    return TestBed.runInInjectionContext(() => new TestListBase());
  }

  it('inicia com form vazio e submitting false', () => {
    const base = create();

    expect(base.form).toBeTruthy();
    expect(base.submitting()).toBe(false);
  });

  it('getContentFieldOrDefault retorna "-" para vazio/nulo', () => {
    const base = create();

    expect(base.getContentFieldOrDefault('')).toBe('-');
    expect(base.getContentFieldOrDefault(null)).toBe('-');
    expect(base.getContentFieldOrDefault(undefined)).toBe('-');
  });

  it('getContentFieldOrDefault preserva string não vazia e outros tipos', () => {
    const base = create();

    expect(base.getContentFieldOrDefault('acme')).toBe('acme');
    expect(base.getContentFieldOrDefault(42)).toBe(42);
  });
});
