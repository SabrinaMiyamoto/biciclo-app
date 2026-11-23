// Tipos espelhando os DTOs da API (docs/api-contrato.md).

export type Role = 'CYCLIST' | 'PARTNER' | 'ADMIN';
export type TipoPoi = 'BICICLETARIO' | 'OFICINA' | 'PONTO_APOIO' | 'TERMINAL';
export type StatusAnalise = 'PENDENTE' | 'APROVADO' | 'REJEITADO';
export type StatusCupom = 'PENDENTE' | 'UTILIZADO' | 'EXPIRADO';
export type StatusAlerta = 'ABERTO' | 'EM_ANALISE' | 'RESOLVIDO';

export interface LoginResponse {
  token: string;
  id: string;
  nome: string;
  email: string;
  role: Role;
}

export interface RegisterRequest {
  idToken: string;
  role: Role;
  nome?: string;
  nomeFantasia?: string;
  cnpj?: string;
  latitude?: number;
  longitude?: number;
}

export interface Usuario {
  id: string;
  nome: string;
  email: string;
  role: Role;
  provedor?: string;
  fotoUrl?: string;
  saldoPontos?: number;
  createdAt?: string;
}

export interface Extrato {
  id: string;
  tipoMovimento: 'ENTRADA' | 'SAIDA';
  quantidade: number;
  referenciaTipo: 'TRAJETO' | 'CUPOM';
  referenciaId: string;
  createdAt: string;
}

export interface Poi {
  id: string;
  tipo: TipoPoi;
  descricao: string;
  latitude: number;
  longitude: number;
  capacidade?: number;
  ativo?: boolean;
  origem?: string;
}

export interface PoiRequest {
  tipo: TipoPoi;
  descricao: string;
  latitude: number;
  longitude: number;
  capacidade?: number | null;
  ativo?: boolean;
}

/** Oficina vinda do Google Places (ao vivo, não pode ser guardada). */
export interface Oficina {
  placeId: string;
  nome: string;
  endereco?: string;
  latitude: number;
  longitude: number;
  nota?: number;
  totalAvaliacoes?: number;
  abertoAgora?: boolean;
  googleMapsUrl?: string;
}

export interface PontoTrajeto {
  latitude: number;
  longitude: number;
  timestamp: string; // ISO-8601
}

export interface Trajeto {
  id: string;
  distanciaMetros: number;
  duracaoSegundos: number;
  velocidadeMedia: number;
  pontosGanhos: number;
  statusAnalise: StatusAnalise;
  createdAt: string;
}

export interface Recompensa {
  id: string;
  parceiroId: string;
  titulo: string;
  descricao?: string;
  custoPontos: number;
  quantidadeEstoque: number;
  dataExpiracao?: string | null;
  ativa: boolean;
}

export interface RecompensaRequest {
  titulo: string;
  descricao?: string;
  custoPontos: number;
  quantidadeEstoque: number;
  dataExpiracao?: string | null;
  ativa?: boolean;
}

export interface Cupom {
  id: string;
  codigoValidacao: string;
  recompensaId: string;
  ciclistaId: string;
  status: StatusCupom;
  dataResgate: string;
  dataUtilizacao?: string | null;
}

export interface AlertaRequest {
  tipo: string;
  descricao: string;
  latitude: number;
  longitude: number;
  fotoUrl?: string;
}

export interface Alerta extends AlertaRequest {
  id: string;
  ciclistaId: string;
  status: StatusAlerta;
  createdAt: string;
  updatedAt?: string;
}

export interface ImportacaoOsm {
  recebidos: number;
  criados: number;
  atualizados: number;
  ignorados: number;
}

/** Corpo de erro padronizado da API. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

export const NOMES_POI: Record<TipoPoi, string> = {
  BICICLETARIO: 'Bicicletário',
  OFICINA: 'Oficina',
  PONTO_APOIO: 'Ponto de apoio',
  TERMINAL: 'Terminal',
};

export const NOMES_ROLE: Record<Role, string> = {
  CYCLIST: 'Ciclista',
  PARTNER: 'Parceiro',
  ADMIN: 'Administrador',
};
