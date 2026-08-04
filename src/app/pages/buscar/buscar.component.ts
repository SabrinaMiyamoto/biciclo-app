import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PontosService } from '../../services/pontos.service';

const CATEGORIAS = [
  { key: 'ciclovia', label: 'Ciclovias', icon: '🚲' },
  { key: 'bicicletaria', label: 'Bicicletaria', icon: '🏪' },
  { key: 'manutencao', label: 'Oficinas', icon: '🔧' },
  { key: 'estacionamento', label: 'Estacionamento', icon: '🅿️' }
];

@Component({
  selector: 'app-buscar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './buscar.component.html',
  styleUrl: './buscar.component.scss'
})
export class BuscarComponent {
  termo = '';
  categorias = CATEGORIAS;

  constructor(private pontosService: PontosService) {}

  get resultados() {
    return this.pontosService.buscar(this.termo);
  }
}
