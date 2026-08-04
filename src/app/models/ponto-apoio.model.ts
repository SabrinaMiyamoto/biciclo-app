export type CategoriaPonto = 'bicicletaria' | 'manutencao' | 'estacionamento' | 'ciclovia';

export interface PontoApoio {
  id: string;
  nome: string;
  categoria: CategoriaPonto;
  bairro: string;
  distanciaKm: number;
  avaliacao: number;
  endereco: string;
  descricao: string;
  lat: number;
  lng: number;
}
