# CyberLadies
Projeto Integrador III

A integração de sessão, solicitação de educadora e análise administrativa no
frontend está descrita em [frontend/VALIDACAO.md](frontend/VALIDACAO.md), incluindo
as rotas de tela e os testes com PostgreSQL isolado.

## CyberTalk

A criação operacional de uma conta dedicada ADMIN está documentada em
[backend/ADMINISTRADOR.md](backend/ADMINISTRADOR.md). Ela não faz parte do cadastro
público nem é executada ao iniciar a aplicação.

Comunidade do CyberLadies. Nesta primeira etapa, a usuária pode criar uma conta, entrar na comunidade, publicar textos de até 500 caracteres, editar e excluir as próprias publicações, curtir e comentar. As operações usam a API REST em Spring Boot e o PostgreSQL. A adesão é explícita e o servidor verifica a autoria de cada alteração.

### Executar com Docker Desktop
Banco novo é inicializado pelo Flyway. Para um volume com tabelas anteriores,
pare a API e siga primeiro o [procedimento de backup, conferência e adoção](backend/MIGRACOES.md).
Não habilite baseline automático para contornar a recusa de um banco existente.

Na raiz do repositório, execute `docker compose up` e aguarde a mensagem do Vite. Abra http://localhost:5173/cadastro/usuaria. Para encerrar, use Ctrl+C e `docker compose down`. Posts, comentários e anexos ficam no volume PostgreSQL entre execuções.

O botão **Compartilhar** copia um link para a publicação. Quem abrir precisa entrar e participar da comunidade. Em desenvolvimento, um link localhost só funciona na máquina que hospeda o site. Cada post aceita um arquivo de até 5 MB, com ou sem texto. JPEG e PNG têm visualização e os demais arquivos podem ser baixados.

### Executar localmente sem Docker

Use Java 21, PostgreSQL e Node.js 22.12 ou superior. No PostgreSQL, crie um banco chamado `cyberladies` e um usuário proprietário. No terminal do backend, defina `DATABASE_USER` e `DATABASE_PASSWORD` com as credenciais desse usuário. Para outro servidor ou banco, defina também `DATABASE_URL` no formato `jdbc:postgresql://host:5432/banco`.

1. Na pasta `backend`, execute `bash mvnw spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`). Flyway cria as tabelas em um banco vazio e o aplicativo escuta na porta 8080. Para um banco existente, siga antes o [procedimento de adoção](backend/MIGRACOES.md).
2. Em outro terminal, na pasta `frontend`, execute `npm ci` e `npm run dev`. Abra a URL exibida pelo Vite. O proxy local encaminha `/api` ao backend. Remova configurações antigas de `VITE_API_URL`.
3. Abra `/cadastro/usuaria`, crie a conta, faça login e clique em **Participar da comunidade**. Publique um texto, recarregue a página, edite e exclua. Para testar permissões, use uma segunda conta em uma janela privativa.

### Contrato da API

Prefixo `/api`. Após o login, envie o token em `Authorization: Bearer <token>`. Ele expira em oito horas. Cadastro e publicações recebem JSON.

| Método | Caminho | Operação |
| --- | --- | --- |
| POST | /auth/cadastro/usuaria | Cadastro com nome, email, senha e competencias opcional |
| POST | /auth/login | Recebe email e senha, devolve token |
| DELETE | /auth/sessao | Encerra a sessão |
| GET | /auth/me | ID e papéis da conta autenticada, sem credenciais |
| POST | /educadora/solicitacao | Solicita atuação com especialidade e instituição; inicia PENDENTE |
| GET | /educadora/solicitacao | Consulta a solicitação da própria conta e seu status |
| GET | /admin/educadoras/solicitacoes | ADMIN lista solicitações com filtro e paginação |
| GET | /admin/educadoras/solicitacoes/{contaId} | ADMIN consulta uma solicitação |
| POST | /admin/educadoras/solicitacoes/{contaId}/decisao | ADMIN aprova ou rejeita uma solicitação pendente |
| GET | /usuaria | Perfil e situação de adesão |
| PUT | /comunidade/adesao | Participar da comunidade |
| GET, POST | /posts | Listar ou publicar |
| GET, PUT, DELETE | /posts/{id} | Consultar, editar ou excluir |
| PUT, DELETE | /posts/{id}/curtida | Curtir ou retirar curtida |
| GET, POST | /posts/{id}/comentarios | Listar ou adicionar comentários |

Publicações e comentários recebem `{"texto":"Minha mensagem"}`. Criações retornam 201. A API diferencia dados inválidos (400), sessão inválida (401), falta de permissão (403), publicação ausente (404) e e-mail duplicado (409).

Esta etapa cobre o perfil usuária e publicações de texto. Rascunhos e moderação ficam para as próximas etapas. Para publicar o projeto, sirva o frontend e encaminhe `/api` ao backend no mesmo domínio.
