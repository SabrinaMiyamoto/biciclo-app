import { Injectable } from '@angular/core';
import { PontoApoio } from '../models/ponto-apoio.model';

// Dados mock. Troque pelos dados reais de uma API (ex: Google Places) quando integrar o backend.
const MOCK_PONTOS: PontoApoio[] = [
  {
    id: '1',
    nome: 'Bicicletaria Mar Verde',
    categoria: 'bicicletaria',
    bairro: 'Barra da Lagoa',
    distanciaKm: 1.2,
    avaliacao: 4.7,
    endereco: 'Rua das Rendeiras, 320 - Barra da Lagoa',
    descricao: 'Loja e manutenção de bicicletas, com peças e acessórios.',
    lat: -27.5744,
    lng: -48.4463
  },
  {
    id: '2',
    nome: 'Parque do Lua',
    categoria: 'estacionamento',
    bairro: 'Lagoa da Conceição',
    distanciaKm: 2.4,
    avaliacao: 4.5,
    endereco: 'Av. das Rendeiras - Lagoa da Conceição',
    descricao: 'Estacionamento de bicicletas próximo à orla da lagoa.',
    lat: -27.6033,
    lng: -48.4581
  },
  {
    id: '3',
    nome: 'Ciclovia da Lagoa',
    categoria: 'ciclovia',
    bairro: 'Lagoa da Conceição',
    distanciaKm: 0.8,
    avaliacao: 4.8,
    endereco: 'Av. das Rendeiras, Lagoa da Conceição - Florianópolis (SC)',
    descricao: 'Ponto de apoio com infraestrutura básica e sinalização.',
    lat: -27.5948,
    lng: -48.5146
  },
  {
    id: '4',
    nome: 'Oficina Pedale',
    categoria: 'manutencao',
    bairro: 'Trindade',
    distanciaKm: 3.6,
    avaliacao: 4.6,
    endereco: 'Trindade, Florianópolis - SC',
    descricao: 'Suporte técnico, reparos e venda de peças.',
    lat: -27.5966,
    lng: -48.5225
  },
  {
    id: '5',
    nome: 'Bike Tech Floripa',
    categoria: 'bicicletaria',
    bairro: 'Santa Mônica',
    distanciaKm: 4.1,
    avaliacao: 4.4,
    endereco: 'Santa Mônica, Florianópolis - SC',
    descricao: 'Aluguel de bicicletas e equipamentos.',
    lat: -27.6009,
    lng: -48.5192
  }
];

@Injectable({ providedIn: 'root' })
export class PontosService {
  listar(): PontoApoio[] {
    return MOCK_PONTOS;
  }

  porId(id: string): PontoApoio | undefined {
    return MOCK_PONTOS.find(p => p.id === id);
  }

  buscar(termo: string): PontoApoio[] {
    const t = termo.trim().toLowerCase();
    if (!t) return MOCK_PONTOS;
    return MOCK_PONTOS.filter(p =>
      p.nome.toLowerCase().includes(t) ||
      p.bairro.toLowerCase().includes(t) ||
      p.categoria.toLowerCase().includes(t)
    );
  }
}
