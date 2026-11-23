import { importLibrary, setOptions } from '@googlemaps/js-api-loader';
import { environment } from '../../environments/environment';
import { TipoPoi } from '../core/models';

export interface Coord { lat: number; lng: number }

let configurado = false;

/** Carrega o Google Maps JavaScript API uma única vez. */
async function carregarGoogle() {
  if (!configurado) {
    setOptions({ key: environment.googleMaps.apiKey, v: 'weekly', language: 'pt-BR', region: 'BR' });
    configurado = true;
  }
  const [maps, marker] = await Promise.all([importLibrary('maps'), importLibrary('marker')]);
  return { maps, marker };
}

export const EMOJI_POI: Record<TipoPoi, string> = {
  BICICLETARIO: '🅿️',
  OFICINA: '🔧',
  PONTO_APOIO: '🚰',
  TERMINAL: '🚌',
};

export interface OpcoesMarcador {
  titulo?: string;
  /** Texto do balão (mostrado com textContent, sem risco de HTML injetado). */
  popup?: string;
  aoClicar?: () => void;
}

/**
 * Pequena camada sobre o Google Maps para as telas não dependerem da API do Google
 * diretamente: criar mapa, marcadores, linhas e cliques.
 */
export class MapaBiciclo {
  private info: google.maps.InfoWindow;

  private constructor(
    readonly map: google.maps.Map,
    private libs: Awaited<ReturnType<typeof carregarGoogle>>,
  ) {
    this.info = new libs.maps.InfoWindow();
  }

  /** Cria o mapa dentro do elemento. Se a chave estiver faltando ou errada, mostra um aviso no lugar. */
  static async criar(el: HTMLElement, zoom = 13): Promise<MapaBiciclo | null> {
    if (environment.googleMaps.apiKey.startsWith('COLE_')) {
      el.innerHTML = '<p class="mapa-aviso">Configure a chave do Google Maps em src/environments/environment.ts (veja o README).</p>';
      return null;
    }
    try {
      const libs = await carregarGoogle();
      const map = new libs.maps.Map(el, {
        center: { lat: environment.centro.latitude, lng: environment.centro.longitude },
        zoom,
        mapId: environment.googleMaps.mapId,
        streetViewControl: false,
        mapTypeControl: false,
        fullscreenControl: false,
        clickableIcons: false,
      });
      return new MapaBiciclo(map, libs);
    } catch {
      el.innerHTML = '<p class="mapa-aviso">Não foi possível carregar o Google Maps. Confira a internet e a chave.</p>';
      return null;
    }
  }

  centro(): Coord {
    const c = this.map.getCenter()!;
    return { lat: c.lat(), lng: c.lng() };
  }

  centralizar(p: Coord, zoom?: number) {
    this.map.panTo(p);
    if (zoom) this.map.setZoom(zoom);
  }

  /** Ajusta o zoom para mostrar todos os pontos. */
  enquadrar(pontos: Coord[]) {
    const b = new google.maps.LatLngBounds();
    pontos.forEach((p) => b.extend(p));
    this.map.fitBounds(b, 40);
  }

  aoClicar(cb: (p: Coord) => void) {
    this.map.addListener('click', (e: google.maps.MapMouseEvent) => e.latLng && cb({ lat: e.latLng.lat(), lng: e.latLng.lng() }));
  }

  aoMover(cb: () => void) {
    this.map.addListener('idle', cb);
  }

  /** Marcador em forma de gota com um emoji, no estilo do protótipo. */
  marcador(p: Coord, conteudo: string, cor = '#173f2c', op: OpcoesMarcador = {}) {
    const gota = document.createElement('div');
    gota.className = 'marcador';
    const span = document.createElement('span');
    span.style.background = cor;
    const i = document.createElement('i');
    i.textContent = conteudo;
    span.appendChild(i);
    gota.appendChild(span);

    const m = new this.libs.marker.AdvancedMarkerElement({
      map: this.map, position: p, content: gota, title: op.titulo ?? '', gmpClickable: !!(op.popup || op.aoClicar),
    });
    if (op.popup || op.aoClicar) {
      m.addListener('click', () => {
        if (op.popup) {
          const div = document.createElement('div');
          div.className = 'mapa-popup';
          div.textContent = op.popup;
          this.info.setContent(div);
          this.info.open({ map: this.map, anchor: m });
        }
        op.aoClicar?.();
      });
    }
    return m;
  }

  marcadorPoi(p: Coord, tipo: TipoPoi, op: OpcoesMarcador = {}) {
    return this.marcador(p, EMOJI_POI[tipo] ?? '📍', '#ffffff', op);
  }

  /** Move um marcador já existente (ou cria, se ainda não existe). */
  mover(m: google.maps.marker.AdvancedMarkerElement | null, p: Coord, conteudo: string, cor: string) {
    if (m) {
      m.position = p;
      m.map = this.map;
      return m;
    }
    return this.marcador(p, conteudo, cor);
  }

  linha(pontos: Coord[], cor = '#2f6847') {
    return new this.libs.maps.Polyline({ map: this.map, path: pontos, strokeColor: cor, strokeWeight: 5, strokeOpacity: 0.9 });
  }

  fecharPopup() {
    this.info.close();
  }
}

/** Remove marcadores/linhas do mapa. */
export function limpar(itens: (google.maps.marker.AdvancedMarkerElement | google.maps.Polyline)[]) {
  for (const it of itens) {
    if (it instanceof google.maps.Polyline) it.setMap(null);
    else it.map = null;
  }
  itens.length = 0;
}

/** Distância em metros entre dois pontos (fórmula de haversine, a mesma da API). */
export function distanciaMetros(a: Coord, b: Coord): number {
  const R = 6371000, rad = Math.PI / 180;
  const dLat = (b.lat - a.lat) * rad, dLng = (b.lng - a.lng) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * rad) * Math.cos(b.lat * rad) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** Busca de endereço no Nominatim (OpenStreetMap, gratuito), limitada a Florianópolis. */
export async function buscarEndereco(texto: string): Promise<{ lat: number; lon: number; nome: string } | null> {
  const params = new URLSearchParams({
    q: texto.toLowerCase().includes('florian') ? texto : `${texto}, Florianópolis, SC`,
    format: 'json',
    limit: '1',
    countrycodes: 'br',
    viewbox: '-48.62,-27.38,-48.35,-27.85',
    bounded: '1',
  });
  const resp = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, {
    headers: { 'Accept-Language': 'pt-BR' },
  });
  const dados = await resp.json();
  if (!dados.length) return null;
  return { lat: +dados[0].lat, lon: +dados[0].lon, nome: dados[0].display_name };
}
