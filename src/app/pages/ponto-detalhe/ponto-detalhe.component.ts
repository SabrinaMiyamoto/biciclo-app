import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { PontosService } from '../../services/pontos.service';
import { PontoApoio } from '../../models/ponto-apoio.model';

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
  selector: 'app-ponto-detalhe',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ponto-detalhe.component.html',
  styleUrl: './ponto-detalhe.component.scss'
})
export class PontoDetalheComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer?: ElementRef<HTMLDivElement>;

  ponto?: PontoApoio;
  private map?: L.Map;

  constructor(route: ActivatedRoute, private pontosService: PontosService) {
    const id = route.snapshot.paramMap.get('id') ?? '';
    this.ponto = this.pontosService.porId(id);
  }

  ngAfterViewInit(): void {
    if (!this.ponto || !this.mapContainer) return;
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [this.ponto.lat, this.ponto.lng],
      zoom: 15,
      zoomControl: false
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19
    }).addTo(this.map);

    L.marker([this.ponto.lat, this.ponto.lng], { icon: icone })
      .addTo(this.map)
      .bindPopup(this.ponto.nome);
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  abrirRota() {
    if (!this.ponto) return;
    const url = `https://www.openstreetmap.org/directions?to=${this.ponto.lat}%2C${this.ponto.lng}`;
    window.open(url, '_blank');
  }
}
