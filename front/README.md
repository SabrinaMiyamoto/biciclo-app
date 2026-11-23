# Biciclo Floripa — app Angular

Front-end em Angular 22 que consome a **biciclo-api** (Spring Boot, pasta `biciclo-angular`).
Mapa: **Google Maps JavaScript API**. Busca de endereços Nominatim (OpenStreetMap), rotas de bicicleta
pelo servidor público do FOSSGIS (routing.openstreetmap.de), clima Open-Meteo e login Firebase Auth (plano Spark).

## Telas

| Rota | Quem vê | O que faz | Endpoints |
|---|---|---|---|
| `/` | todos | Início + clima atual | Open-Meteo |
| `/mapa` | todos | Traça rota de bicicleta, mostra pontos de apoio por tipo e (logado) lista oficinas do Google | `GET /pois`, `GET /oficinas` |
| `/entrar` | todos | Login e cadastro (e-mail/senha ou Google), ciclista ou parceiro | `/auth/firebase`, `/auth/register` |
| `/recompensas` | todos (resgate só ciclista) | Catálogo, resgate e "Meus cupons" | `/recompensas`, `/resgatar`, `/cupons/meus` |
| `/pedalar` | ciclista | Grava o trajeto pelo GPS e envia; lista "Meus trajetos" | `/trajetos`, `/trajetos/mine` |
| `/alertas` | ciclista | Marca um problema no mapa e envia; lista os seus | `/alertas-infraestrutura`, `/minhas` |
| `/perfil` | logado | Dados, saldo e extrato de pontos | `/usuarios/me`, `/me/extrato` |
| `/parceiro` | parceiro, admin | Cadastrar/editar/desativar recompensas; validar cupom (só parceiro) | `/recompensas/minhas`, `/cupons/validar` |
| `/admin` | admin | Moderar alertas, CRUD de pontos + importar do OSM, mudar perfil de usuários | `/admin/**` |

## Como rodar

Pré-requisito: **Node.js 24 LTS** (o Angular 22 aceita Node 22.22+ ou 24.15+).

```bash
npm install
npm start          # abre em http://localhost:4200
```

A API precisa estar rodando em `http://localhost:8080` (veja o README da API).
Ela já libera CORS para `http://localhost:4200`.

## Configurar o Google Maps (uma vez)

1. Entre em https://console.cloud.google.com, crie (ou escolha) um projeto. Pode ser o mesmo projeto do Firebase (`biciclo-18016`).
2. **Faturamento**: o Google exige uma conta de faturamento com cartão, mesmo no uso gratuito.
   O mapa dinâmico tem uma cota mensal grátis (pelo preço atual, 10 mil carregamentos por mês), mais do que suficiente para um trabalho de escola.
   Para não ter surpresa, crie um **alerta de orçamento** (Faturamento → Orçamentos e alertas) de R$ 1.
3. APIs e serviços → Biblioteca → ative **Maps JavaScript API**.
4. APIs e serviços → Credenciais → **Criar credenciais → Chave de API**. Depois, edite a chave:
   - Restrição de aplicativo: **Referenciadores HTTP** → `http://localhost:4200/*` (e o endereço onde for publicar).
   - Restrição de API: só **Maps JavaScript API**.
5. Cole a chave em `src/environments/environment.ts`, em `googleMaps.apiKey`.
   O `mapId: 'DEMO_MAP_ID'` já serve para testes (é ele que permite os marcadores personalizados).

Se aparecer "Ops! Algo deu errado" no mapa, abra o console do navegador (F12): o Google diz o motivo
(`InvalidKeyMapError` = chave errada, `RefererNotAllowedMapError` = falta liberar o endereço, `BillingNotEnabledMapError` = falta faturamento).

> A chave do Maps fica visível no navegador; é normal. A proteção é a restrição por referenciador do passo 4.
> A chave do Places da API (`GOOGLE_PLACES_API_KEY`) é outra: fica só no servidor e nunca vai para este projeto.

## Configurar o Firebase (uma vez)

1. Console do Firebase → projeto `biciclo-18016` → ⚙ Configurações do projeto → **Seus apps** → **</> Web** (registre um app se não houver).
2. Copie `apiKey` e `appId` para `src/environments/environment.ts`.
3. Authentication → Settings → **Authorized domains**: confira se `localhost` está na lista.
4. Authentication → Sign-in method: ative **E-mail/senha** e **Google**.

> Esses valores do app Web não são secretos. O arquivo `*-firebase-adminsdk-*.json` é a chave
> privada da API: **nunca** copie para este projeto nem para o GitHub.

Para ter um ADMIN, coloque o e-mail em `ADMIN_EMAILS` ao subir a API e faça login de novo.

## Testes

```bash
npm test           # Vitest
npm run build      # gera dist/ para publicar
```

## Estrutura

```
src/app/
  core/       api.service.ts (todas as chamadas HTTP), auth.service.ts (Firebase → JWT),
              auth.interceptor.ts (Bearer + 401), guards.ts (rotas por perfil), models.ts
  shared/     mapa.ts (classe MapaBiciclo sobre o Google Maps, marcadores, Nominatim), formatos.ts (pipes de distância/duração)
  pages/      uma tela por arquivo (componentes standalone com signals)
src/environments/environment.ts   URL da API, config do Firebase, simulação de trajeto
```

## Decisões e limitações desta primeira versão

- **Simular trajeto**: o botão em `/pedalar` envia um passeio fictício de ~2 km para testar sem sair
  pedalando. Desligue com `permitirSimulacao: false` antes da apresentação final.
- **GPS**: o navegador só libera localização em `localhost` ou HTTPS, e a gravação para se a tela
  do celular apagar. Para uso real seria preciso um app nativo (por exemplo, Capacitor).
- **Rotas**: continuam vindo do roteador de bicicleta do FOSSGIS (grátis), desenhadas sobre o mapa do Google.
  Trocar pelo roteamento do Google exigiria ativar mais uma API paga, e o modo bicicleta do Google tem pouca cobertura no Brasil.
  Se o FOSSGIS falhar, o app mostra uma linha reta com distância estimada.
- **Endereços**: a busca continua no Nominatim (grátis), para não precisar ativar a Geocoding API do Google.
- **Oficinas do Google**: só aparecem se a API tiver `GOOGLE_PLACES_API_KEY` (sem ela a API responde 503 e o botão some). Pelos termos do Google, o app mostra "Dados: Google Maps", abre o lugar pelo `googleMapsUrl` e não guarda os resultados. Atenção: a Places API exige conta de faturamento (cartão) no Google Cloud.
- **Tipos de alerta** (BURACO, ILUMINACAO...) são sugestões: a API aceita qualquer texto.
- **Avaliações** do protótipo não foram trazidas: a API ainda não tem esse endpoint.
- O nome da recompensa em "Meus cupons" vem do catálogo; cupons de recompensas já inativas aparecem como "Recompensa".
