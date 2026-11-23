/**
 * Configuração do app.
 *
 * - apiUrl: endereço da API Spring Boot (biciclo-api). Em dev ela roda em localhost:8080.
 * - firebase: copie do Console do Firebase → Configurações do projeto → Seus apps → App da Web.
 *   Esses valores NÃO são segredo (vão para o navegador de qualquer forma).
 *   Nunca coloque aqui o arquivo *-firebase-adminsdk-*.json: ele é a chave privada da API.
 */
export const environment = {
  apiUrl: 'http://localhost:8080/api/v1',
  firebase: {
    apiKey: 'COLE_A_WEB_API_KEY',
    authDomain: 'biciclo-18016.firebaseapp.com',
    projectId: 'biciclo-18016',
    appId: 'COLE_O_APP_ID',
  },
  /**
   * Google Maps JavaScript API. Chave criada no Google Cloud Console (veja o README).
   * Restrinja a chave por "Referenciadores HTTP" (ex.: http://localhost:4200/*) e só à Maps JavaScript API.
   * mapId: 'DEMO_MAP_ID' funciona para testes; para produção crie um Map ID no console.
   */
  googleMaps: {
    apiKey: 'COLE_A_CHAVE_DO_GOOGLE_MAPS',
    mapId: 'DEMO_MAP_ID',
  },
  /** Mostra o botão "Simular trajeto" para testar sem sair pedalando. Use false na versão final. */
  permitirSimulacao: true,
  /** Centro padrão do mapa (Florianópolis). */
  centro: { latitude: -27.5954, longitude: -48.548 },
};
