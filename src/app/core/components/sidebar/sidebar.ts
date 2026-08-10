//Angular
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/**
 * Sidebar mínima do console (guia de estilo, seção 7.1) — só Tenants por enquanto. Os
 * demais itens (Sistemas, Perfis, Usuários, Vínculos, Platform Admins) entram na Fase 7,
 * quando o shell completo é construído.
 */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
})
export class Sidebar {}
