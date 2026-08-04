import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface InfoCard {
  icon: string;
  titulo: string;
  descricao: string;
}

@Component({
  selector: 'app-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './info.component.html',
  styleUrl: './info.component.scss'
})
export class InfoComponent {
  cards: InfoCard[] = [
    { icon: '🚲', titulo: 'Rotas', descricao: 'Ciclovias e ciclofaixas mapeadas na cidade.' },
    { icon: '🛡️', titulo: 'Segurança', descricao: 'Dicas para pedalar com mais tranquilidade.' },
    { icon: '☁️', titulo: 'Clima', descricao: 'Previsão do tempo para o seu trajeto.' },
    { icon: '🚦', titulo: 'Trânsito', descricao: 'Condições das vias em tempo real.' },
    { icon: '🔧', titulo: 'Manutenção', descricao: 'Oficinas e pontos de assistência.' },
    { icon: '📋', titulo: 'Legislação', descricao: 'Regras de circulação para ciclistas.' }
  ];
}
