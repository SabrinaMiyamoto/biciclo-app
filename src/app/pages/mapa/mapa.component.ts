import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { PontosService } from '../../services/pontos.service';
import { PontoApoio } from '../../models/ponto-apoio.model';
import { environment } from '../../../environments/environment';

const icone = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.scss'
})
export class MapaComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;

  termo = '';
  pontos: PontoApoio[];
  private map?: L.Map;
  private marcadoresLayer = L.layerGroup();

  constructor(private pontosService: PontosService, private router: Router) {
    this.pontos = this.pontosService.listar();
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [environment.florianopolisCenter.lat, environment.florianopolisCenter.lng],
      zoom: 13
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.map);

    this.marcadoresLayer.addTo(this.map);
    this.atualizarMarcadores();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  get marcadores() {
    const t = this.termo.trim().toLowerCase();
    if (!t) return this.pontos;
    return this.pontos.filter(p => p.nome.toLowerCase().includes(t));
  }

  onBuscar() {
    this.atualizarMarcadores();
  }

  private atualizarMarcadores() {
    this.marcadoresLayer.clearLayers();
    for (const ponto of this.marcadores) {
      const marker = L.marker([ponto.lat, ponto.lng], { icon: icone })
        .bindPopup(`<strong>${ponto.nome}</strong><br>${ponto.bairro}`);
      marker.on('click', () => marker.openPopup());
      marker.on('popupopen', () => {
        const el = document.getElementById(`ir-${ponto.id}`);
        el?.addEventListener('click', () => this.router.navigate(['/pontos', ponto.id]));
      });
      marker.bindPopup(
        `<strong>${ponto.nome}</strong><br>${ponto.bairro}<br><a id="ir-${ponto.id}" href="javascript:void(0)">Ver detalhes</a>`
      );
      this.marcadoresLayer.addLayer(marker);
    }
  }
}
