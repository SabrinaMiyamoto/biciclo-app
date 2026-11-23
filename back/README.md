# 🚴 Biciclo — API

API REST do **Biciclo**, plataforma de mobilidade urbana por bicicleta com gamificação (pontos e recompensas) e dados geoespaciais de infraestrutura cicloviária de Florianópolis.

## 🛠️ Stack
- **Java 17** + **Spring Boot 3.4**
- **Firebase Firestore** (banco NoSQL) + **Firebase Auth** (Google e e-mail/senha)
- **Busca por proximidade** com **geohash** (células de ~4,9 km + filtro haversine)
- **Spring Security** + **JWT** (RBAC: `CYCLIST`, `PARTNER`, `ADMIN`)
- **Redis** (cache de dados do OpenStreetMap / Overpass API)
- **Lombok**, **springdoc-openapi** (Swagger UI)

## 📋 Pré-requisitos

| Ferramenta | Versão |
| :--- | :--- |
| **JDK** | 17+ (testado com 21) |
| **Maven** | 3.9+ |
| **Firebase** | Projeto com Firestore e Auth habilitados |
| **Redis** | 6+ (opcional, apenas cache) |

## 🔥 Configuração do Firebase

1. **Crie o projeto** no [Firebase Console](https://console.firebase.google.com/).
2. **Habilite o Firestore:** Build → Firestore Database → Criar banco.
3. **Habilite o Auth:** Build → Authentication → escolha os provedores
   (email/senha, Google, etc.).
4. **Gere a chave da conta de serviço:** ⚙️ Configurações do projeto →
   aba **Contas de serviço** → **Gerar nova chave privada**.
5. **Aponte a variável de ambiente** para o JSON baixado:
   ```powershell
   $env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\caminho\para\firebase-adminsdk-xxxxx.json"
   ```

> ⚠️ **Nunca** commite o arquivo JSON da conta de serviço no Git.
> Em ambientes gerenciados (Cloud Run, GKE), as credenciais padrão são usadas
> automaticamente quando `FIREBASE_SERVICE_ACCOUNT_PATH` não é definida.
> Mantenha o JSON **fora** da pasta do projeto e confira que o `.gitignore`
> contém o padrão `*-firebase-adminsdk-*.json`.

> 📖 **Passo a passo completo** de configuração do Firebase (Console, índices,
> regras, credenciais) e de testes locais:
> [`docs/firebase-setup-e-testes.md`](docs/firebase-setup-e-testes.md).

### Índices do Firestore

As consultas que combinam filtros e ordenação (e a busca por geohash) exigem
índices compostos. Eles estão declarados em
[`firestore.indexes.json`](firestore.indexes.json):

| Coleção | Campos |
| :--- | :--- |
| `pontos_interesse` | `ativo` + `geohash` |
| `pontos_interesse` | `tipo` + `ativo` |
| `trajetos` | `ciclistaId` + `createdAt` (DESC) |
| `cupons` | `ciclistaId` + `dataResgate` (DESC) |
| `extrato_pontos` | `ciclistaId` + `createdAt` (DESC) |
| `alertas_infraestrutura` | `status` + `createdAt` (DESC) |
| `alertas_infraestrutura` | `ciclistaId` + `createdAt` (DESC) |

Aplique com o Firebase CLI (regras + índices):

```powershell
firebase deploy --only firestore
```

> Alternativa: crie os índices manualmente no Console (Firestore → Índices).
> Sem eles, as consultas acima falham com `FAILED_PRECONDITION`.

## 🚀 Como rodar

1. **Defina as variáveis de ambiente** (no perfil `dev` o `JWT_SECRET` tem um
   valor só para desenvolvimento; fora dele a API não sobe sem a variável):
   ```powershell
   $env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\caminho\para\firebase-adminsdk-xxxxx.json"
   $env:JWT_SECRET="uma-chave-longa-e-aleatoria-com-no-minimo-32-bytes"
   # Origens do front-end liberadas no CORS (padrão: http://localhost:4200)
   $env:CORS_ALLOWED_ORIGINS="http://localhost:4200,https://biciclo.example"
   # E-mails que viram ADMIN ao entrar (cria o primeiro administrador)
   $env:ADMIN_EMAILS="seu-email@exemplo.com"
   # Opcional: oficinas com nota e horário via Google Places (ver abaixo)
   $env:GOOGLE_PLACES_API_KEY="sua-chave-do-google-cloud"
   # Opcional (cache):
   $env:REDIS_HOST="localhost"
   $env:REDIS_PORT="6379"
   ```

2. **(Opcional) Suba o Redis:**
   ```powershell
   docker compose up -d redis
   ```

3. **Execute a aplicação:**
   ```powershell
   mvn spring-boot:run
   ```

4. **Acesse a documentação do Swagger:**
   ```text
   http://localhost:8080/swagger-ui.html
   ```

## 📂 Estrutura do projeto

```text
biciclo-api/
├── pom.xml
├── docs/
│   ├── padroes-de-codigo.md           # Convenções de código e camadas
│   ├── como-adicionar-um-endpoint.md  # Receita passo a passo p/ novos domínios
│   └── api-contrato.md                # Contrato da API (implementado vs planejado)
└── src/
    └── main/
        ├── java/com/biciclo/
        │   ├── BicicloApplication.java
        │   ├── config/                # Firebase, JWT, Redis, propriedades
        │   ├── security/              # JWT: filtro, serviço, principal, Firebase Auth
        │   ├── common/
        │   │   ├── exception/         # Exceções, ApiError
        │   │   ├── enums/             # Enums de domínio
        │   │   └── firestore/         # Utilitários do Firestore
        │   └── domain/
        │       ├── auth/              # [REFERÊNCIA] register/firebase
        │       ├── usuario/
        │       ├── parceiro/
        │       ├── pontointeresse/    # [REFERÊNCIA] busca por proximidade (geohash)
        │       ├── trajeto/
        │       ├── recompensa/
        │       ├── cupom/
        │       └── extrato/
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            └── application-prod.yml
```

### Convenção de camadas (por domínio)
Cada domínio segue rigorosamente a ordem **Controller → Service → Repository → Model**:

```text
domain/<modulo>/
├── <Modulo>.java           # Model (POJO mapeado no Firestore)
├── Repository.java         # Acesso ao Firestore (via Firebase Admin SDK)
├── Service.java            # Regras de negócio
├── Controller.java         # Endpoints REST
└── dto/                    # Objetos de entrada/saída (nunca expor models)
```

### Coleções do Firestore

| Coleção | Documento |
| :--- | :--- |
| `usuarios` | `Usuario` |
| `parceiros` | `Parceiro` |
| `pontos_interesse` | `PontoInteresse` (com `localizacao` + `geohash`) |
| `trajetos` | `Trajeto` |
| `recompensas` | `Recompensa` |
| `cupons` | `Cupom` |
| `extrato_pontos` | `ExtratoPontos` |
| `alertas_infraestrutura` | `AlertaInfraestrutura` |

## 🛣️ Endpoints implementados (referência)

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Cadastro do perfil (ciclista ou parceiro) |
| `POST` | `/api/v1/auth/firebase` | Login/cadastro via Firebase Auth → JWT da API |
| `GET` | `/api/v1/pois?latitude=&longitude=&raio=` | Busca de POIs por proximidade (geohash) |
| `POST` | `/api/v1/trajetos` | Registra trajeto e credita pontos (RF08–RF11) |
| `GET` | `/api/v1/trajetos/mine` | Lista os trajetos do ciclista autenticado |
| `GET` | `/api/v1/recompensas` | Catálogo público de recompensas ativas |
| `GET` | `/api/v1/recompensas/{id}` | Detalhe de uma recompensa |
| `GET` | `/api/v1/recompensas/minhas` | Recompensas do parceiro autenticado |
| `POST` | `/api/v1/recompensas` | Cria recompensa (parceiro/admin) |
| `PUT` | `/api/v1/recompensas/{id}` | Edita recompensa (dono/admin) |
| `DELETE` | `/api/v1/recompensas/{id}` | Desativa recompensa (soft delete) |
| `POST` | `/api/v1/recompensas/{id}/resgatar` | Resgate atômico (saldo + estoque + cupom + extrato) |
| `GET` | `/api/v1/cupons/meus` | Cupons do ciclista autenticado |
| `POST` | `/api/v1/cupons/validar` | Validação do cupom pelo parceiro |
| `GET` | `/api/v1/usuarios/me` | Perfil do usuário autenticado |
| `GET` | `/api/v1/usuarios/me/extrato` | Extrato de pontos do usuário |
| `POST` | `/api/v1/alertas-infraestrutura` | Registra alerta de infraestrutura (RF07) |
| `GET` | `/api/v1/alertas-infraestrutura/minhas` | Alertas do ciclista autenticado |
| `GET` | `/api/v1/admin/alertas?status=` | Fila de moderação de alertas (admin) |
| `PATCH` | `/api/v1/admin/alertas/{id}/status` | Modera um alerta (admin) |
| `GET` / `POST` | `/api/v1/admin/pois` | Detalhe / cadastro de POI (admin) |
| `PUT` / `DELETE` | `/api/v1/admin/pois/{id}` | Edita / desativa POI (admin) |
| `POST` | `/api/v1/admin/pois/importar-osm?forcar=` | Importa POIs do OpenStreetMap via Overpass, com cache Redis (admin) |
| `GET` | `/api/v1/oficinas?latitude=&longitude=&raio=` | Oficinas próximas com nota e horário, ao vivo no Google Places (autenticado) |
| `GET` | `/api/v1/admin/usuarios?role=` | Lista usuários (admin) |
| `PATCH` | `/api/v1/admin/usuarios/{id}/role` | Alterna perfil CYCLIST/ADMIN (admin) |

> 🔐 **Fluxo de autenticação:** o cliente cria/entra na conta pelo **Firebase Auth
> SDK** (Google ou e-mail/senha) e obtém um **ID token**. No primeiro acesso,
> envia os dados do perfil em `POST /api/v1/auth/register`; nos seguintes, usa
> `POST /api/v1/auth/firebase`. A API valida o ID token, cria/localiza o perfil
> no Firestore e devolve um **JWT** próprio, usado como
> `Authorization: Bearer <token>` nas demais requisições. A senha nunca trafega
> pela API.

> 🗺️ O contrato completo, com exemplos, está em
> [`docs/api-contrato.md`](docs/api-contrato.md).

## 🗺️ Google Places (oficinas)

A infraestrutura cicloviária vem do OpenStreetMap (gratuito). O Google Places
entra só na busca de oficinas, que traz nota, avaliações e horário.

1. No [Google Cloud Console](https://console.cloud.google.com/), crie um projeto
   e **ative o faturamento** (exige cartão, mesmo dentro da cota gratuita).
2. Ative a **Places API (New)**.
3. Em *APIs e serviços → Credenciais*, crie uma chave de API e **restrinja-a à
   Places API (New)**. Como a chave fica só no servidor, não use restrição por site.
4. Defina `GOOGLE_PLACES_API_KEY` com a chave. Nunca a coloque no código nem no front-end.
5. Recomendado: em *Cotas*, limite as requisições por dia da Places API e crie
   um alerta de orçamento em *Faturamento → Orçamentos e alertas*.

> 💰 **Custo:** cada busca é cobrada. Pedir nota, avaliações e horário coloca a
> chamada na faixa mais cara do Nearby Search (SKU "Enterprise"), que tem a
> menor cota gratuita mensal. Confira os valores atuais na página de preços do
> Google Maps Platform. Sem a chave, só a rota `/api/v1/oficinas` fica
> indisponível (`503`).

## ⚠️ Formato de erro padronizado

Toda resposta de erro na plataforma segue estritamente o padrão (RNF05):

```json
{
  "timestamp": "2026-09-22T12:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente"
}
```

## 🤝 Como contribuir

1. Leia o guia de [`docs/padroes-de-codigo.md`](docs/padroes-de-codigo.md).
2. Siga o passo a passo em [`docs/como-adicionar-um-endpoint.md`](docs/como-adicionar-um-endpoint.md).
3. Todo novo domínio deve obrigatoriamente replicar a arquitetura dos módulos padrão `auth` e `pontointeresse`.
