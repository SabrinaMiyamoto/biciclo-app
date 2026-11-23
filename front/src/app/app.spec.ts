import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { tokenExpirado } from './core/auth.service';
import { DistanciaPipe, DuracaoPipe } from './shared/formatos';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('mostra o botão Entrar quando não há sessão', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.nav-cta')?.textContent).toContain('Entrar');
  });
});

describe('formatos', () => {
  it('formata distância', () => {
    const p = new DistanciaPipe();
    expect(p.transform(850)).toBe('850 m');
    expect(p.transform(1113.2)).toBe('1,11 km');
  });

  it('formata duração', () => {
    const p = new DuracaoPipe();
    expect(p.transform(754)).toBe('12 min 34 s');
    expect(p.transform(3900)).toBe('1 h 5 min');
  });
});

describe('tokenExpirado', () => {
  const jwt = (exp: number) => `x.${btoa(JSON.stringify({ exp }))}.y`;
  it('detecta token vencido e válido', () => {
    expect(tokenExpirado(jwt(Date.now() / 1000 - 60))).toBe(true);
    expect(tokenExpirado(jwt(Date.now() / 1000 + 3600))).toBe(false);
    expect(tokenExpirado('lixo')).toBe(true);
  });
});
