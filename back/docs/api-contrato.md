# Contrato da API

Status: `[OK]` implementado (referência) · `[TODO]` planejado — implemente seguindo
[`como-adicionar-um-endpoint.md`](como-adicionar-um-endpoint.md).

## Autenticação

A identidade é gerenciada pelo **Firebase Auth** (Google e e-mail/senha). A API
**nunca recebe senha**: o cliente autentica no SDK do Firebase e envia o
`idToken` resultante.

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público (exige `idToken`) | `[OK]` |
| `POST` | `/api/v1/auth/firebase` | Público (exige `idToken`) | `[OK]` |

### Registro de ciclista

```
POST /api/v1/auth/register
{ "idToken": "<firebase-id-token>", "role": "CYCLIST", "nome": "Ana" }

201 Created
{ "token": "<jwt>", "id": "<uid>", "nome": "Ana", "email": "a@b.com", "role": "CYCLIST" }
```

### Registro de parceiro

```
POST /api/v1/auth/register
{
  "idToken": "<firebase-id-token>",
  "role": "PARTNER", "nome": "Padaria Pão Quente",
  "nomeFantasia": "Pão Quente", "cnpj": "12.345.678/0001-90",
  "latitude": -27.59, "longitude": -48.55
}
```

### Login (Google ou e-mail/senha)

```
POST /api/v1/auth/firebase
{ "idToken": "<firebase-id-token>" }

200 OK
{ "token": "<jwt>", "id": "<uid>", "nome": "Ana", "email": "a@b.com", "role": "CYCLIST" }
```

> O primeiro login via Firebase cria automaticamente um perfil `CYCLIST` quando
> não existe cadastro. Para `PARTNER`, use `/auth/register`.
> E-mails listados em `ADMIN_EMAILS` são promovidos a `ADMIN` no login (só
> ciclistas; parceiros não mudam). É assim que se cria o primeiro administrador.
> Contas antigas com senha local (`senhaHash`) precisam ser migradas manualmente.

## Pontos de Interesse (POIs)

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `GET` | `/api/v1/pois` | Público | `[OK]` |
| `GET` | `/api/v1/admin/pois/{id}` | Admin | `[OK]` |
| `POST` | `/api/v1/admin/pois` | Admin | `[OK]` |
| `PUT` | `/api/v1/admin/pois/{id}` | Admin | `[OK]` |
| `DELETE` | `/api/v1/admin/pois/{id}` | Admin | `[OK]` (soft delete) |
| `POST` | `/api/v1/admin/pois/importar-osm` | Admin | `[OK]` (RF06) |

```
GET /api/v1/pois?latitude=-27.59&longitude=-48.55&raio=1000

200 OK
[ { "id": "abc123", "tipo": "BICICLETARIO", "descricao": "...",
    "latitude": -27.59, "longitude": -48.55, "capacidade": 20,
    "ativo": true, "origem": "OSM" } ]
```

- `raio` em metros, maior que 0 e no máximo 5000 (RNF02); padrão 1000.
- Só POIs ativos aparecem; no máximo 50, do mais próximo ao mais distante.

### Cadastrar / editar POI (admin)

```
POST /api/v1/admin/pois
Authorization: Bearer <jwt de ADMIN>
{ "tipo": "OFICINA", "descricao": "Oficina do Centro",
  "latitude": -27.5969, "longitude": -48.5495, "capacidade": null }

201 Created
{ "id": "xyz", "tipo": "OFICINA", ..., "ativo": true, "origem": "MANUAL" }
```

- `tipo`: `BICICLETARIO`, `OFICINA`, `PONTO_APOIO` ou `TERMINAL`.
- `PUT` aceita o mesmo corpo e, opcionalmente, `"ativo": true` para reativar.
- `DELETE` só desativa (`ativo=false`).

### Importar do OpenStreetMap (admin)

```
POST /api/v1/admin/pois/importar-osm?forcar=false
Authorization: Bearer <jwt de ADMIN>

200 OK
{ "recebidos": 412, "criados": 380, "atualizados": 20, "ignorados": 12 }
```

- Consulta a Overpass API na região de `OVERPASS_BBOX` (padrão: Florianópolis).
- Mapeamento: `amenity=bicycle_parking` → `BICICLETARIO`; `shop=bicycle` →
  `OFICINA`; `amenity=bicycle_repair_station` e `bicycle_rental` → `PONTO_APOIO`;
  `amenity=bus_station` → `TERMINAL`.
- O resultado da Overpass fica 24 h no Redis (RNF04). `forcar=true` ignora o cache.
- Reimportar não duplica: o id do documento é `osm-<tipo>-<id>`. POIs que um
  ADMIN desativou continuam desativados (contam em `ignorados`).
- `503` quando a Overpass está fora e não há cache.

## Oficinas (Google Places)

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `GET` | `/api/v1/oficinas` | Autenticado | `[OK]` |

```
GET /api/v1/oficinas?latitude=-27.59&longitude=-48.55&raio=2000
Authorization: Bearer <jwt>

200 OK
[ { "placeId": "ChIJ...", "nome": "Pedal Floripa", "endereco": "Rua A, 10",
    "latitude": -27.59, "longitude": -48.55, "nota": 4.7, "totalAvaliacoes": 120,
    "abertoAgora": true, "googleMapsUrl": "https://maps.google.com/?cid=..." } ]
```

- Consulta ao vivo o Nearby Search da Places API (New), tipo `bicycle_store`,
  até 20 resultados ordenados por distância. `raio` em metros (padrão 2000, máx. 5000).
- Complementa os POIs do OpenStreetMap: bicicletários e estações de reparo
  continuam vindo do OSM (`/api/v1/pois`).
- Exige login porque cada chamada gasta cota paga do Google.
- Sem `GOOGLE_PLACES_API_KEY` configurada, ou com o Google fora: `503`.
- **Termos do Google:** os resultados não são gravados no Firestore (só o
  `placeId` poderia ser guardado) e o front-end deve mostrar a atribuição
  "Google Maps" junto da lista e usar `googleMapsUrl` para abrir o lugar.

## Trajetos (Gamificação)

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `POST` | `/api/v1/trajetos` | Ciclista | `[OK]` (RF08–RF11) |
| `GET`  | `/api/v1/trajetos/mine` | Ciclista | `[OK]` |

### Registrar trajeto

```
POST /api/v1/trajetos
Authorization: Bearer <jwt>
{
  "pontos": [
    { "latitude": -27.5900, "longitude": -48.5500, "timestamp": "2026-10-27T10:00:00Z" },
    { "latitude": -27.6000, "longitude": -48.5500, "timestamp": "2026-10-27T10:10:00Z" }
  ]
}

201 Created
{
  "id": "abc123", "distanciaMetros": 1113.2, "duracaoSegundos": 600,
  "velocidadeMedia": 6.68, "pontosGanhos": 11,
  "statusAnalise": "APROVADO", "createdAt": "2026-10-27T13:10:00Z"
}
```

Regras (RF08–RF11):

- distância, duração e velocidade são calculadas **no servidor** (haversine)
- **10 pontos por km**, mínimo de 200 m e máximo de 60 km/h
- fora das regras → `statusAnalise: "REJEITADO"` e `pontosGanhos: 0`
- timestamps em ordem cronológica, duração > 0 e sem datas no futuro
- ao aprovar, trajeto + extrato + saldo são gravados na **mesma transação** do Firestore (RNF01)

### Meus trajetos

```
GET /api/v1/trajetos/mine
Authorization: Bearer <jwt>

200 OK
[ { "id": "abc123", "distanciaMetros": 1113.2, "duracaoSegundos": 600,
    "velocidadeMedia": 6.68, "pontosGanhos": 11,
    "statusAnalise": "APROVADO", "createdAt": "2026-10-27T13:10:00Z" } ]
```

## Recompensas e Cupons

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `GET`  | `/api/v1/recompensas` | Público | `[OK]` (RF12) |
| `GET`  | `/api/v1/recompensas/{id}` | Autenticado | `[OK]` |
| `GET`  | `/api/v1/recompensas/minhas` | Parceiro/Admin | `[OK]` |
| `POST` | `/api/v1/recompensas` | Parceiro/Admin | `[OK]` |
| `PUT`  | `/api/v1/recompensas/{id}` | Parceiro/Admin (dono) | `[OK]` |
| `DELETE` | `/api/v1/recompensas/{id}` | Parceiro/Admin (dono) | `[OK]` (soft delete) |
| `POST` | `/api/v1/recompensas/{id}/resgatar` | Ciclista | `[OK]` (RF13) |
| `GET`  | `/api/v1/cupons/meus` | Ciclista | `[OK]` |
| `POST` | `/api/v1/cupons/validar` | Parceiro | `[OK]` (RF14) |

### Catálogo de recompensas

```
GET /api/v1/recompensas

200 OK
[ { "id": "rec1", "parceiroId": "<uid-do-parceiro>", "titulo": "10% na oficina",
    "descricao": "...", "custoPontos": 11, "quantidadeEstoque": 5,
    "dataExpiracao": "2027-12-31", "ativa": true } ]
```

Só aparecem recompensas `ativa = true`, com `quantidadeEstoque > 0` e dentro da
validade. `dataExpiracao` aceita `yyyy-MM-dd` ou ISO-8601; vazio = sem validade.

### Gerenciar recompensas (parceiro/admin)

Criar:

```
POST /api/v1/recompensas
Authorization: Bearer <jwt de PARTNER ou ADMIN>
{
  "titulo": "10% na oficina",
  "descricao": "Desconto em peças",
  "custoPontos": 11,
  "quantidadeEstoque": 5,
  "dataExpiracao": "2027-12-31",
  "ativa": true
}

201 Created
{ "id": "rec1", "parceiroId": "<uid>", "titulo": "10% na oficina",
  "custoPontos": 11, "quantidadeEstoque": 5,
  "dataExpiracao": "2027-12-31", "ativa": true }
```

- `parceiroId` é sempre o UID do usuário autenticado;
- somente `ADMIN` pode enviar `parceiroId` no corpo para criar em nome de outro parceiro;
- `ativa` ausente = `true`; `dataExpiracao` vazia = sem validade.

Editar (somente o dono ou ADMIN):

```
PUT /api/v1/recompensas/rec1
{
  "titulo": "15% na oficina", "descricao": "...", "custoPontos": 20,
  "quantidadeEstoque": 3, "dataExpiracao": null, "ativa": true
}
```

- `parceiroId` não pode ser alterado;
- `ativa` ausente mantém o valor atual.

Desativar (soft delete):

```
DELETE /api/v1/recompensas/rec1

204 No Content
```

A recompensa **não é apagada**: fica `ativa = false`, preservando os cupons já
emitidos. Para reexibi-la, use `PUT` com `ativa: true`.

Minhas recompensas (inclui inativas e sem estoque):

```
GET /api/v1/recompensas/minhas
Authorization: Bearer <jwt de PARTNER ou ADMIN>
```

Erros: `404` recompensa não encontrada; `403` recompensa de outro parceiro.

### Resgatar recompensa

```
POST /api/v1/recompensas/{id}/resgatar
Authorization: Bearer <jwt>

201 Created
{ "id": "cupom1", "codigoValidacao": "cupom1", "recompensaId": "rec1",
  "ciclistaId": "<uid>", "status": "PENDENTE",
  "dataResgate": "2026-10-27T20:00:00Z", "dataUtilizacao": null }
```

O resgate é **atômico** (RNF01): valida saldo/estoque/validade, debita os pontos,
baixa o estoque, cria o cupom e registra a saída no extrato na mesma transação.

Erros: `404` recompensa/usuário; `422` saldo insuficiente, estoque esgotado,
recompensa inativa ou expirada.

### Meus cupons

```
GET /api/v1/cupons/meus
Authorization: Bearer <jwt>
```

### Validar cupom (parceiro)

```
POST /api/v1/cupons/validar
Authorization: Bearer <jwt do parceiro>
{ "codigo": "cupom1" }

200 OK
{ "id": "cupom1", "codigoValidacao": "cupom1", "recompensaId": "rec1",
  "ciclistaId": "<uid>", "status": "UTILIZADO",
  "dataResgate": "...", "dataUtilizacao": "..." }
```

- o cupom precisa estar `PENDENTE`;
- a recompensa do cupom precisa pertencer ao parceiro autenticado
  (`parceiroId` = UID do usuário parceiro);
- erros: `404` cupom não encontrado, `403` cupom de outro parceiro,
  `422` cupom já utilizado ou expirado.

## Perfil / Administração

| Método | Rota | Perfil | Status |
|---|---|---|---|
| `GET`  | `/api/v1/usuarios/me` | Autenticado | `[OK]` (RF03) |
| `GET`  | `/api/v1/usuarios/me/extrato` | Autenticado | `[OK]` (RF03) |
| `POST` | `/api/v1/alertas-infraestrutura` | Ciclista | `[OK]` (RF07) |
| `GET`  | `/api/v1/alertas-infraestrutura/minhas` | Ciclista | `[OK]` |
| `GET`  | `/api/v1/admin/alertas?status=` | Admin | `[OK]` |
| `PATCH` | `/api/v1/admin/alertas/{id}/status` | Admin | `[OK]` |
| `GET`  | `/api/v1/admin/usuarios?role=` | Admin | `[OK]` |
| `PATCH` | `/api/v1/admin/usuarios/{id}/role` | Admin | `[OK]` |

### Moderação de alertas (admin)

```
PATCH /api/v1/admin/alertas/{id}/status
Authorization: Bearer <jwt de ADMIN>
{ "status": "EM_ANALISE" }

200 OK
{ "id": "a1", ..., "status": "EM_ANALISE", "updatedAt": "..." }
```

- Transições: `ABERTO` → `EM_ANALISE` ou `RESOLVIDO`; `EM_ANALISE` → `RESOLVIDO`
  ou `ABERTO`. `RESOLVIDO` é final (`422` ao tentar mudar).
- `GET /api/v1/admin/alertas` lista os 100 mais recentes; `status` filtra.

### Perfil de usuário (admin)

```
PATCH /api/v1/admin/usuarios/{id}/role
Authorization: Bearer <jwt de ADMIN>
{ "role": "ADMIN" }
```

- Só alterna entre `CYCLIST` e `ADMIN`. Parceiros não mudam por aqui.
- O admin não altera o próprio perfil (evita ficar sem nenhum admin).
- O novo perfil vale no próximo login: o JWT atual guarda o perfil antigo.

### Perfil

```
GET /api/v1/usuarios/me
Authorization: Bearer <jwt>

200 OK
{ "id": "<uid>", "nome": "Ana", "email": "ana@biciclo.com",
  "role": "CYCLIST", "provedor": "google.com", "fotoUrl": "...",
  "saldoPontos": 11, "createdAt": "..." }
```

### Extrato de pontos

```
GET /api/v1/usuarios/me/extrato
Authorization: Bearer <jwt>

200 OK
[ { "id": "e1", "tipoMovimento": "ENTRADA", "quantidade": 11,
    "referenciaTipo": "TRAJETO", "referenciaId": "t1", "createdAt": "..." } ]
```

### Alerta de infraestrutura

```
POST /api/v1/alertas-infraestrutura
Authorization: Bearer <jwt de ciclista>
{ "tipo": "BURACO", "descricao": "Buraco na ciclovia",
  "latitude": -27.59, "longitude": -48.55, "fotoUrl": "https://..." }

201 Created
{ "id": "a1", "ciclistaId": "<uid>", "tipo": "BURACO", ...,
  "status": "ABERTO", "createdAt": "..." }
```

## Códigos HTTP

| Código | Significado |
|---|---|
| `200` | Sucesso |
| `201` | Recurso criado |
| `400` | Payload inválido |
| `401` | Não autenticado / token inválido |
| `403` | Perfil sem permissão |
| `404` | Recurso não encontrado |
| `409` | Conflito (e-mail ou CNPJ já cadastrado) |
| `422` | Regra de negócio (saldo insuficiente, estoque esgotado) |
| `500` | Erro interno |

## Erro padronizado

```json
{ "timestamp": "...", "status": 422, "error": "Unprocessable Entity", "message": "..." }
```
