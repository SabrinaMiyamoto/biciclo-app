import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PontosService } from '../../services/pontos.service';
import { CategoriaPonto } from '../../models/ponto-apoio.model';

type Filtro = 'todos' | CategoriaPonto;

const FILTROS: { key: Filtro; label: string }[] = [
  { key: 'todos', label: 'Todos' },
  { key: 'bicicletaria', label: 'Bicicletaria' },
  { key: 'manutencao', label: 'Manutenção' },
  { key: 'estacionamento', label: 'Estacionamento' }
];

@Component({
  selector: 'app-pontos-apoio',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pontos-apoio.component.html',
  styleUrl: './pontos-apoio.component.scss'
})
export class PontosApoioComponent {
  filtros = FILTROS;
  filtroAtivo = signal<Filtro>('todos');
  pontos;

  constructor(private pontosService: PontosService) {
    this.pontos = this.pontosService.listar();
  }

  get pontosFiltrados() {
    const f = this.filtroAtivo();
    if (f === 'todos') return this.pontos;
    return this.pontos.filter(p => p.categoria === f);
  }

  selecionar(f: Filtro) {
    this.filtroAtivo.set(f);
  }
}
