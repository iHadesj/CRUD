# CRUD de Pessoas

CRUD em Java pra cadastrar pessoas (nome e idade), com três jeitos de usar a mesma lógica:

- **Terminal** (`CrudExecutor`): menu no console
- **Desktop** (`CrudApp`): interface em JavaFX com busca, edição e exportação pra CSV
- **API HTTP** (`CrudServer`): servidor usando o `HttpServer` do próprio JDK, sem framework

Os três usam a mesma classe `Crud` e salvam no mesmo arquivo, então o que você cadastra num aparece nos outros.

## Estrutura

```
src/
├── Person.java        modelo + validação (nome obrigatório, idade de 0 a 130)
├── Crud.java          create / read / update / delete
├── PersonStore.java   leitura e gravação do CSV
├── CrudExecutor.java  versão terminal
├── CrudApp.java       versão JavaFX
├── CrudServer.java    versão HTTP
└── main/resources/    CSS e imagem de fundo do JavaFX
```

Os dados ficam em `~/.crud-pessoas/pessoas.csv` (no Windows, `C:\Users\<você>\.crud-pessoas\pessoas.csv`).

## Requisitos

- JDK 20+
- Maven (o do IntelliJ serve)

## Como rodar

**Desktop (JavaFX)**

```bash
mvn javafx:run
```

**Terminal e servidor**: roda o `main` do `CrudExecutor` ou do `CrudServer` direto pelo IntelliJ.

## API

O servidor sobe em `http://localhost:8081`. O body é form (`nome=...&idade=...`) e a resposta é texto puro.

| Método | Rota | O que faz |
|---|---|---|
| GET | `/pessoas` | lista todo mundo |
| GET | `/pessoas/{id}` | busca uma pessoa |
| POST | `/pessoas` | cria |
| PUT | `/pessoas/{id}` | atualiza |
| DELETE | `/pessoas/{id}` | remove |

O `id` é a posição na lista, começando em 1.

```bash
curl http://localhost:8081/pessoas
curl -X POST http://localhost:8081/pessoas -d "nome=Maria&idade=25"
curl -X PUT http://localhost:8081/pessoas/1 -d "nome=Maria&idade=26"
curl -X DELETE http://localhost:8081/pessoas/1
```

> No PowerShell use `curl.exe` em vez de `curl`.

Status de retorno: `200`/`201` ok, `400` dado inválido, `404` id não existe, `405` método não suportado, `500` erro ao salvar.
