# Firebase + testes locais — Biciclo API

> Guia operacional do backend. Projeto Firebase atual: **`biciclo-18016`**.
> Atualize este arquivo conforme novas funcionalidades forem implementadas.

## 📌 Status do backend

| Recurso | Endpoints | Status |
|---|---|---|
| Autenticação (Firebase) | `POST /api/v1/auth/register`, `POST /api/v1/auth/firebase` | ✅ |
| Pontos de interesse | `GET /api/v1/pois` | ✅ |
| Trajetos + gamificação | `POST /api/v1/trajetos`, `GET /api/v1/trajetos/mine` | ✅ |
| Recompensas / cupons | `/api/v1/recompensas`, `/api/v1/cupons/**` | ✅ |
| Perfil / extrato | `/api/v1/usuarios/me`, `/api/v1/usuarios/me/extrato` | ✅ |
| Alertas de infraestrutura | `POST /api/v1/alertas-infraestrutura` | ✅ |

---

## 1. Checklist no Console do Firebase

### 1.1 Confirmar o projeto

1. Abra: <https://console.firebase.google.com/project/biciclo-18016>
2. Se aparecer **"projeto não encontrado"**, o projeto foi removido:
   - crie um projeto novo;
   - gere uma nova chave de conta de serviço;
   - atualize o `.firebaserc` (campo `default`) e a variável `FIREBASE_SERVICE_ACCOUNT_PATH`;
   - descarte a chave antiga.

### 1.2 Criar o Firestore

Console → **Build → Firestore Database → Criar banco de dados**

- Modo: **produção** (o backend usa o Admin SDK, que ignora as regras)
- Local: **`southamerica-east1` (São Paulo)**
- Se já existir, apenas confirme que o banco `(default)` está criado.

> Sem o Firestore criado, as queries falham com `NOT_FOUND` / `The database (default) does not exist`.

### 1.3 Habilitar os provedores de login

Console → **Build → Authentication → Sign-in method**

- **Google**: habilitar
  - e-mail de suporte do projeto
  - nome público do app
- **E-mail/senha**: habilitar

Console → **Authentication → Settings → Authorized domains**

Adicionar (além dos que já vêm):

- `localhost`
- `127.0.0.1`
- o domínio de produção quando existir

> O domínio autorizado é necessário para o `signInWithPopup` do Google funcionar no navegador.

### 1.4 Conta de serviço (chave do backend)

A chave usada hoje está na raiz do projeto:

```text
biciclo-18016-firebase-adminsdk-fbsvc-8b14fdbde2.json
```

Ela já está protegida pelo `.gitignore`, mas:

- 🔐 **recomendado rotacionar**: Console → ⚙️ Configurações do projeto → **Contas de serviço** → **Gerar nova chave privada**;
- apague a chave antiga no Console;
- mova o novo JSON para fora do repositório/OneDrive, por exemplo:
  `C:\Users\1\secrets\biciclo-firebase.json`;
- **nunca** commite o JSON nem o exponha no navegador.

### 1.5 Índices compostos do Firestore

As queries com filtro + ordenação exigem índices. Eles estão declarados em
[`firestore.indexes.json`](../firestore.indexes.json):

| Coleção | Campos | Usado por |
|---|---|---|
| `pontos_interesse` | `ativo` ASC, `geohash` ASC | busca de POIs |
| `pontos_interesse` | `tipo` ASC, `ativo` ASC | consultas por tipo |
| `trajetos` | `ciclistaId` ASC, `createdAt` DESC | `GET /trajetos/mine` |
| `cupons` | `ciclistaId` ASC, `dataResgate` DESC | futuro `/cupons/meus` |
| `extrato_pontos` | `ciclistaId` ASC, `createdAt` DESC | futuro `/usuarios/me/extrato` |

**Opção A — Firebase CLI (recomendado)**

```powershell
npm install -g firebase-tools
firebase login
cd "C:\Users\1\OneDrive\Documentos\biciclo-angular"
firebase use biciclo-18016
firebase deploy --only firestore
```

O `firebase deploy --only firestore` aplica **regras + índices**.
Para aplicar só os índices: `firebase deploy --only firestore:indexes`.

**Opção B — manual no Console**

Firestore → **Índices** → **Criar índice** → *Índice composto*, repetindo a tabela acima.

> Se uma query falhar com `FAILED_PRECONDITION`, o log da API traz um link direto
> para criar o índice que falta.

### 1.6 Regras do Firestore

O app Angular deve falar **somente com a API** (não acessar o Firestore direto).
Por isso as regras negam todo acesso de cliente:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

Arquivo: [`firestore.rules`](../firestore.rules). Aplique com:

```powershell
firebase deploy --only firestore:rules
```

> O Admin SDK (backend) **ignora** essas regras — a API continua funcionando.
> Só relaxe as regras se, no futuro, o app precisar ler o Firestore diretamente.

### 1.7 Criar o primeiro ADMIN

O autocadastro só permite `CYCLIST` e `PARTNER` (proteção contra escalada de privilégio).

1. Faça o cadastro normal de um usuário (via app ou REST).
2. No Console → Firestore → coleção `usuarios` → documento com o UID do usuário.
3. Edite o campo `role` para `ADMIN`.
4. Faça login novamente para gerar um JWT com a nova role.

---

## 2. Ambiente local

### 2.1 Requisitos

- JDK 17+ (testado com JDK 21)
- Maven 3.9+ (`mvn` não está no PATH desta máquina; use o Maven embutido do IntelliJ
  ou instale o Maven)
- Redis (opcional — o cache ainda não é usado em nenhuma rota)

### 2.2 Variáveis de ambiente

```powershell
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\caminho\para\biciclo-firebase.json"
# opcional em produção:
$env:JWT_SECRET="uma-chave-longa-e-aleatoria-com-no-minimo-32-bytes"
```

No IntelliJ: **Run → Edit Configurations → Environment variables**.

> Sem a variável (e sem `GOOGLE_APPLICATION_CREDENTIALS`), a API não sobe e mostra
> `Credenciais do Firebase não configuradas`.

### 2.3 Rodar

```powershell
cd "C:\Users\1\OneDrive\Documentos\biciclo-angular"
mvn spring-boot:run
```

- API: <http://localhost:8080>
- Swagger: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

> O Swagger ainda não tem botão de Bearer; para testar rotas autenticadas use
> PowerShell/Postman/Insomnia.

### 2.4 Redis (opcional)

```powershell
docker compose up -d redis
```

Sem o Redis, o `/actuator/health` pode aparecer `DOWN` (contribuinte do Redis).
Isso **não** impede o login nem os trajetos hoje.

---

## 3. Como testar (passo a passo)

### 3.1 Pegar a Web API Key

Console → ⚙️ **Configurações do projeto → Geral → Seus apps**

- se não houver app Web, clique em `</>` e registre um;
- copie a **Web API Key** (ela é pública, mas deve ser restringida depois).

### 3.2 Criar um usuário de teste (e-mail/senha via REST)

```powershell
$apiKey = "COLE_A_WEB_API_KEY"

$body = @{
  email = "ana.teste@biciclo.com"
  password = "Senha123!"
  returnSecureToken = $true
} | ConvertTo-Json

$auth = Invoke-RestMethod -Method Post `
  -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey" `
  -ContentType "application/json" -Body $body

$idToken = $auth.idToken
$uid = $auth.localId
"UID: $uid"
```

Se o e-mail já existir, use login em vez de cadastro:

```powershell
$auth = Invoke-RestMethod -Method Post `
  -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey" `
  -ContentType "application/json" -Body $body
$idToken = $auth.idToken
```

> O `idToken` do Firebase expira em ~1 hora. Para renovar, use o `refreshToken`:
> `POST https://securetoken.googleapis.com/v1/token?key=$apiKey`
> com `{ "grant_type": "refresh_token", "refresh_token": "<refreshToken>" }`.

### 3.3 Cadastrar o perfil na API

```powershell
$registro = @{
  idToken = $idToken
  role = "CYCLIST"
  nome = "Ana Teste"
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/register" `
  -ContentType "application/json" -Body $registro

$jwt = $login.token
$headers = @{ Authorization = "Bearer $jwt" }
$login | ConvertTo-Json
```

Resposta esperada (`201`):

```json
{ "token": "<jwt>", "id": "<uid>", "nome": "Ana Teste", "email": "ana.teste@biciclo.com", "role": "CYCLIST" }
```

### 3.4 Login (nos acessos seguintes)

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/firebase" `
  -ContentType "application/json" `
  -Body (@{ idToken = $idToken } | ConvertTo-Json)
$jwt = $login.token
$headers = @{ Authorization = "Bearer $jwt" }
```

### 3.5 Buscar POIs (público)

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/pois?latitude=-27.59&longitude=-48.55&raio=1000"
```

Sem dados cadastrados, retorna `[]`. Para popular, insira documentos na coleção
`pontos_interesse` (o campo `geohash` é gerado pelo `save` do repository; via
Console, preencha também `localizacao` como `GeoPoint` e `geohash` de 5 caracteres,
ou use um script de seed).

### 3.6 Registrar trajeto aprovado

```powershell
$trajeto = @{
  pontos = @(
    @{ latitude = -27.5900; longitude = -48.5500; timestamp = "2025-01-01T10:00:00Z" },
    @{ latitude = -27.6000; longitude = -48.5500; timestamp = "2025-01-01T10:10:00Z" }
  )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/trajetos" `
  -Headers $headers -ContentType "application/json" -Body $trajeto
```

Resposta esperada (`201`):

```json
{
  "id": "<id>",
  "distanciaMetros": 1111.9,
  "duracaoSegundos": 600,
  "velocidadeMedia": 6.67,
  "pontosGanhos": 11,
  "statusAnalise": "APROVADO",
  "createdAt": "..."
}
```

### 3.7 Testar trajeto rejeitado

Distância muito curta (≈5 m):

```powershell
$curto = @{
  pontos = @(
    @{ latitude = -27.5900; longitude = -48.5500; timestamp = "2025-01-01T10:00:00Z" },
    @{ latitude = -27.59005; longitude = -48.5500; timestamp = "2025-01-01T10:10:00Z" }
  )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/trajetos" `
  -Headers $headers -ContentType "application/json" -Body $curto
```

Esperado: `statusAnalise: "REJEITADO"` e `pontosGanhos: 0` (HTTP `201`).

### 3.8 Listar meus trajetos

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/v1/trajetos/mine" -Headers $headers
```

### 3.9 Conferir no Firestore

Após um trajeto aprovado, verifique:

| Coleção | Documento esperado |
|---|---|
| `usuarios` | `saldoPontos` incrementado (ex.: `11`) |
| `trajetos` | `ciclistaId`, `trajeto` (array de GeoPoint), `distanciaMetros`, `duracaoSegundos`, `velocidadeMedia`, `pontosGanhos`, `statusAnalise`, `createdAt` |
| `extrato_pontos` | `ciclistaId`, `tipoMovimento: "ENTRADA"`, `quantidade: 11`, `referenciaTipo: "TRAJETO"`, `referenciaId`, `createdAt` |

As três escritas acontecem na **mesma transação**: se uma falhar, nenhuma é aplicada.

### 3.10 Testar erros esperados

```powershell
# 401 — sem token
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/trajetos/mine"

# 403 — usuário PARTNER tentando criar trajeto
# (cadastre um segundo usuário com role PARTNER e use o JWT dele no POST /trajetos)

# 409 — cadastrar o mesmo perfil duas vezes
# repita o POST /auth/register com o mesmo idToken
```

### 3.11 Testar login com Google

O backend é o mesmo: o cliente obtém o ID token do Google via Firebase e chama
`POST /api/v1/auth/firebase`.

Formas de testar sem o app Angular:

- **Página HTML mínima** (apêndice A) servida em `http://localhost:3000` com
  `npx serve .` — não abra como `file://`;
- **Angular** com `@angular/fire` (`signInWithPopup`).

Antes, confirme no Console:

- provedor **Google** habilitado;
- `localhost` nos domínios autorizados;
- app Web registrado (para pegar `apiKey`, `authDomain`, `projectId`, `appId`).

### 3.12 Recompensas e cupons

**1. Criar um parceiro de teste**

Repita o passo 3.2 com **outro e-mail** e guarde o token em `$idTokenParceiro`;
depois cadastre o perfil como PARTNER:

```powershell
$registroParceiro = @{
  idToken = $idTokenParceiro
  role = "PARTNER"
  nome = "Oficina Teste"
  nomeFantasia = "Oficina Teste"
  cnpj = "12.345.678/0001-90"
  latitude = -27.59
  longitude = -48.55
} | ConvertTo-Json

$loginParceiro = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/register" `
  -ContentType "application/json" -Body $registroParceiro

$jwtParceiro = $loginParceiro.token
$parceiroId = $loginParceiro.id
```

> O `parceiroId` da recompensa é o **UID do usuário parceiro** — o mesmo id
> dos documentos em `usuarios` e em `parceiros`.

**2. Como parceiro, criar a recompensa pela API**

```powershell
$headersParceiro = @{ Authorization = "Bearer $jwtParceiro" }

$novaRecompensa = @{
  titulo = "10% na oficina"
  descricao = "Desconto de teste"
  custoPontos = 11
  quantidadeEstoque = 5
  dataExpiracao = "2027-12-31"
  ativa = $true
} | ConvertTo-Json

$recompensa = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/recompensas" `
  -Headers $headersParceiro -ContentType "application/json" -Body $novaRecompensa

$recompensaId = $recompensa.id
$recompensa | ConvertTo-Json
```

Esperado: `201` com `parceiroId` igual ao UID do parceiro autenticado.
O `parceiroId` é definido pelo backend a partir do JWT (não envie no corpo).

**3. Como ciclista, listar e resgatar**

```powershell
$recompensas = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/recompensas"
$recompensas | ConvertTo-Json -Depth 5
# $recompensaId veio do passo 2

$cupom = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/recompensas/$recompensaId/resgatar" `
  -Headers $headers
$cupom | ConvertTo-Json
$codigo = $cupom.codigoValidacao
```

Esperado: `status: "PENDENTE"`, saldo reduzido em 11 e um registro `SAIDA`
em `extrato_pontos`.

**4. Como parceiro, validar o cupom**

```powershell
$headersParceiro = @{ Authorization = "Bearer $jwtParceiro" }

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/cupons/validar" `
  -Headers $headersParceiro -ContentType "application/json" `
  -Body (@{ codigo = $codigo } | ConvertTo-Json)
```

Esperado: `status: "UTILIZADO"` e `dataUtilizacao` preenchida.

**5. Testar erros e CRUD**

- resgatar sem saldo → `422 Saldo insuficiente`;
- zerar `quantidadeEstoque` e resgatar → `422 Estoque esgotado`;
- validar o mesmo cupom duas vezes → `422 Cupom já utilizado`;
- validar com o JWT de outro parceiro → `403 Cupom não pertence a este parceiro`;
- `GET /recompensas/minhas` como parceiro mostra a recompensa, inclusive inativa;
- `PUT /recompensas/{id}` com o JWT de outro parceiro → `403 Recompensa pertence a outro parceiro`;
- `DELETE /recompensas/{id}` → `204`; a recompensa sai do catálogo público,
  mas continua em `/minhas` com `ativa: false` (soft delete);
- para reativar: `PUT` com `"ativa": true`.

> O `DELETE` é **soft delete** de propósito: os cupons já emitidos continuam
> apontando para a recompensa e podem ser validados normalmente.

---

## 4. Troubleshooting

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Credenciais do Firebase não configuradas` | env var ausente | defina `FIREBASE_SERVICE_ACCOUNT_PATH` |
| `Credencial do Firebase não encontrada ou sem permissão` | caminho errado | confira o caminho do JSON |
| `The database (default) does not exist` / `NOT_FOUND` | Firestore não criado | crie o banco no Console |
| `FAILED_PRECONDITION: The query requires an index` | índice faltando | crie o índice (log traz o link) |
| `PERMISSION_DENIED` | projeto/chave errados ou IAM | confira `project_id` e a conta de serviço |
| `Token Firebase inválido ou expirado` | ID token de outro projeto ou > 1h | gere um ID token novo |
| `401` nas rotas da API | JWT ausente/expirado (2h) | refaça `/auth/firebase` |
| `403` em `/trajetos` | usuário não é `CYCLIST` | use um usuário ciclista |
| `409 E-mail já cadastrado` | perfil já existe para outro UID | use o login, não o register |
| `/actuator/health` `DOWN` | Redis não está rodando | suba `docker compose up -d redis` |
| `Port 8080 was already in use` | outra instância rodando | pare o processo ou use `--server.port=8081` |
| `CORS` no Angular | origem não permitida | em dev o backend aceita `*`; em prod ajuste `SecurityConfig` |

---

## 5. Segurança — não esquecer

- [ ] Rotacionar a chave de conta de serviço atual (está no OneDrive).
- [ ] Mover o JSON para fora do repositório (`C:\Users\1\secrets\...`).
- [ ] Confirmar que o JSON **não** está versionado: `git ls-files "*.json"` não deve listá-lo.
- [ ] Aplicar `firestore.rules` (nega acesso direto do cliente).
- [ ] Restringir a Web API Key no Google Cloud (APIs e domínios permitidos).
- [ ] Em produção, definir `JWT_SECRET` próprio (o default é só para dev).
- [ ] Nunca logar o `idToken` nem a chave privada.

---

## 6. Pendências do backend

- [x] Recompensas: listagem + resgate atômico (saldo/estoque/cupom/extrato).
- [x] Cupons: `GET /cupons/meus` e `POST /cupons/validar` (parceiro).
- [x] CRUD de recompensas (parceiro/admin) — criar, listar, editar e desativar.
- [x] Perfil/extrato: `GET /usuarios/me` e `GET /usuarios/me/extrato`.
- [x] Alertas de infraestrutura (RF07).
- [ ] Swagger com esquema Bearer para testar rotas autenticadas na UI.
- [ ] Refresh do JWT da API (hoje: 2h; renovar via `/auth/firebase`).
- [ ] Política de e-mail verificado (hoje o registro não bloqueia).
- [ ] Paginação nas listagens.
- [ ] Atualizar `docs/padroes-de-codigo.md` e `docs/como-adicionar-um-endpoint.md`
      (ainda falam de JPA/PostGIS/Flyway; o projeto usa Firestore).
- [ ] Testes de integração (hoje só testes de unidade).

---

## Apêndice A — página mínima para testar Google

Salve como `google-login-test.html`, preencha o `firebaseConfig` e sirva com
`npx serve .` (abra em `http://localhost:3000`):

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head><meta charset="utf-8"><title>Teste Google Login — Biciclo</title></head>
<body>
  <button id="login">Entrar com Google</button>
  <pre id="out"></pre>

  <script type="module">
    import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
    import {
      getAuth, GoogleAuthProvider, signInWithPopup
    } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";

    const firebaseConfig = {
      apiKey: "COLE_A_WEB_API_KEY",
      authDomain: "biciclo-18016.firebaseapp.com",
      projectId: "biciclo-18016",
      appId: "COLE_O_APP_ID"
    };

    const auth = getAuth(initializeApp(firebaseConfig));

    document.getElementById("login").onclick = async () => {
      const out = document.getElementById("out");
      try {
        const cred = await signInWithPopup(auth, new GoogleAuthProvider());
        const idToken = await cred.user.getIdToken();
        out.textContent = "ID token obtido. Trocando pelo JWT da API...\n\n";

        const resp = await fetch("http://localhost:8080/api/v1/auth/firebase", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ idToken })
        });
        out.textContent += JSON.stringify(await resp.json(), null, 2);
      } catch (e) {
        out.textContent = "Erro: " + e.message;
      }
    };
  </script>
</body>
</html>
```

> Se aparecer `auth/unauthorized-domain`, adicione `localhost` em
> Authentication → Settings → Authorized domains.

---

## Referências

- Console: <https://console.firebase.google.com/project/biciclo-18016>
- Firestore: <https://console.firebase.google.com/project/biciclo-18016/firestore>
- Authentication: <https://console.firebase.google.com/project/biciclo-18016/authentication/providers>
- Índices: <https://console.firebase.google.com/project/biciclo-18016/firestore/indexes>
- Firebase Auth REST: <https://firebase.google.com/docs/reference/rest/auth>
- Admin SDK (Java): <https://firebase.google.com/docs/admin/setup>
