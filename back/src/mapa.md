# 🚀 Guia de Cola Rápida: Entrevista Majors Asset

---

## 📌 1. Apresentação Pessoal (2 Minutos)

> "Sou desenvolvedora Full Stack com experiência no ecossistema Python no backend e React/Next.js no frontend. No backend, tenho foco na construção de APIs REST resilientes e de alta performance, aplicando boas práticas de arquitetura, TDD com Pytest, ORM com SQLAlchemy e processamento assíncrono com AWS SQS e Redis.
>
> Tenho muita afinidade com produtos financeiros e de dados, onde a integridade dos dados, a idempotência e o controle de concorrência no banco de dados são fundamentais. No frontend, busco aplicar uma componentização limpa no Next.js aproveitando Server Components e estratégias eficientes de consumo de API."

---

## 🧠 2. Mapa Mental de Palavras-Chave (Gatilhos Rápidos)

### 💳 Concorrência & Mercado Financeiro (O mais importante)
* **2 workers no mesmo contrato / saldo ao mesmo tempo:**
    * 👉 **Palavra-chave:** `Pessimistic Locking` / `SELECT FOR UPDATE`
    * **Explicação:** Trava a linha no PostgreSQL até a transação fazer o `commit`. Impede que dois processos alterem o saldo simultaneamente.
* **Mensagem do SQS reprocessada ou repetida:**
    * 👉 **Palavra-chave:** `Idempotência`
    * **Explicação:** Garantir que processar 1 ou 10 vezes o mesmo evento gere exatamente o mesmo resultado, sem duplicar transações/cobranças.
* **Worker cai no meio do processamento:**
    * 👉 **Palavra-chave:** `Visibility Timeout` + `Dead Letter Queue (DLQ)`
    * **Explicação:** Se o worker cair, o tempo de visibilidade expira e a mensagem volta para a fila. Se falhar repetidamente (`maxReceiveCount`), vai para a DLQ.

---

### 🐍 Falcon Framework (Backend Python)
* **Arquitetura da API:**
    * 👉 **Palavra-chave:** `Orientada a Recursos (Resource-Oriented)`
    * **Explicação:** Mapeamento de URIs para classes com métodos HTTP explicitamente declarados (`on_get`, `on_post`, `on_put`).
* **Validação de Payload/JSON sem sujar a classe do recurso:**
    * 👉 **Palavra-chave:** `Hooks (@falcon.before)` + `Pydantic`
    * **Explicação:** O Hook roda antes do método do recurso, valida os dados e coloca o objeto validado em `req.context`.
* **Tratamento Global de Exceções:**
    * 👉 **Palavra-chave:** `app.add_error_handler()`
    * **Explicação:** Intercepta exceções não tratadas do Python/ORM e devolve respostas JSON padronizadas (ex: 400, 404, 500) sem vazar *stack trace*.
* **Assincronismo / Alta Concorrência:**
    * 👉 **Palavra-chave:** `ASGI (falcon.asgi)` + `AsyncSession` no SQLAlchemy
    * **Explicação:** Para usar `async/await` com Uvicorn, o driver do banco **deve** ser assíncrono (`asyncpg`), caso contrário trava o *Event Loop*.

---

### 🗄️ SQLAlchemy & PostgreSQL
* **Consultas lentas ao carregar relacionamentos:**
    * 👉 **Palavra-chave:** `Problema do N+1` $\rightarrow$ Solução: `Eager Loading`
    * **Explicação:** Resolver usando `joinedload` (para relacionamentos 1:1) ou `selectinload` (para relacionamentos 1:N).
* **Isolamento de banco entre testes:**
    * 👉 **Palavra-chave:** `Transação com Rollback nas Fixtures`
    * **Explicação:** A fixture do Pytest abre uma transação antes do teste e dá `rollback` no final (`yield`). Nada é salvo de fato no banco e os testes rodam super rápido.

---

### 📨 AWS SQS & Redis
* **Otimização de rotas pesadas (Processamento em background):**
    * 👉 **Palavra-chave:** `Desacoplamento Assíncrono`
    * **Explicação:** A API aceita o arquivo/requisição, retorna HTTP 200 OK rapidamente para o cliente e envia o evento para o SQS processar em background.
* **Casos de uso do Redis na Majors:**
    * 👉 **Palavra-chave:** `Cache-Aside` e `Distributed Lock (SETNX)`
    * **Explicação:** Cache para tabelas de taxas e indicadores (com TTL) e travas distribuídas para evitar concorrência entre microserviços.

---

### 🧪 TDD & Qualidade de Código (Pytest)
* **Ciclo do TDD:**
    * 👉 **Palavra-chave:** `Red -> Green -> Refactor`
    * **Explicação:** 1. Escreve o teste e vê falhar (**Red**); 2. Escreve o código mínimo para passar (**Green**); 3. Melhora a estrutura do código (**Refactor**).
* **Isolar dependências externas nos testes (AWS / APIs):**
    * 👉 **Palavra-chave:** `Mocks` / `pytest-mock` / `moto`
    * **Explicação:** A biblioteca `moto` simula o SQS em memória durante a execução dos testes e o `pytest-mock` simula chamadas a APIs de terceiros.

---

### 💻 Frontend React + Next.js
* **Quando usar Server ou Client Component:**
    * 👉 **Palavra-chave:** `Server por Padrão` / `Client para Interatividade`
    * **Explicação:** Server Components para listagens e dados do servidor sem enviar JS extra ao cliente. Client Components (`'use client'`) apenas onde há interatividade (filtros, formulários, modais).
* **Atualização em tempo quase real (D+0):**
    * 👉 **Palavra-chave:** `SWR` ou `TanStack Query (React Query)`
    * **Explicação:** Ferramentas para gerenciamento de estado de servidor, cache, *polling* automático e controle de estados de `isLoading` e `isError`.

---

## ❓ 3 Perguntas Estratégicas para Você Fazer no Final

1. *"Como é estruturada a esteira de CI/CD para validar a cobertura de testes automatizados (TDD) antes dos deploys em produção?"*
2. *"Para o acompanhamento e conciliação de carteiras em D+0, qual é o maior desafio atual da arquitetura em relação ao volume de mensagens no SQS?"*
3. *"Como funciona a divisão de tarefas no dia a dia entre as entregas de backend Python e a evolução das interfaces em Next.js no time?"*
