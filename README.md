# CyberLadies
Projeto Integrador III

## CyberTalk

Comunidade do CyberLadies. Nesta primeira etapa, a usuária pode criar uma conta, entrar na comunidade, publicar textos de até 500 caracteres, editar e excluir as próprias publicações, curtir e comentar. As operações usam a API REST em Spring Boot e o PostgreSQL. A adesão é explícita e o servidor verifica a autoria de cada alteração.

### Executar localmente

Use Java 21, PostgreSQL e Node.js 22.12 ou superior. No PostgreSQL, crie um banco chamado `cyberladies` e um usuário proprietário. No terminal do backend, defina `DATABASE_USER` e `DATABASE_PASSWORD` com as credenciais desse usuário. Para outro servidor ou banco, defina também `DATABASE_URL` no formato `jdbc:postgresql://host:5432/banco`.

1. Na pasta `backend`, execute `bash mvnw spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`). O aplicativo cria as tabelas e escuta na porta 8080.
2. Em outro terminal, na pasta `frontend`, execute `npm ci` e `npm run dev`. Abra a URL exibida pelo Vite. O proxy local encaminha `/api` ao backend. Remova configurações antigas de `VITE_API_URL` e deixe `VITE_MODO_TESTE=false` para usar dados reais.
3. Abra `/cadastro/usuaria`, crie a conta, faça login e clique em **Participar da comunidade**. Publique um texto, recarregue a página, edite e exclua. Para testar permissões, use uma segunda conta em uma janela privativa.

### Contrato da API

Prefixo `/api`. Após o login, envie o token em `Authorization: Bearer <token>`. Ele expira em oito horas. Cadastro e publicações recebem JSON.

| Método | Caminho | Operação |
| --- | --- | --- |
| POST | /auth/cadastro/usuaria | Cadastro com nome, email, senha e competencias opcional |
| POST | /auth/login | Recebe email e senha, devolve token |
| DELETE | /auth/sessao | Encerra a sessão |
| GET | /usuaria | Perfil e situação de adesão |
| PUT | /comunidade/adesao | Participar da comunidade |
| GET, POST | /posts | Listar ou publicar |
| GET, PUT, DELETE | /posts/{id} | Consultar, editar ou excluir |
| PUT, DELETE | /posts/{id}/curtida | Curtir ou retirar curtida |
| GET, POST | /posts/{id}/comentarios | Listar ou adicionar comentários |

Publicações e comentários recebem `{"texto":"Minha mensagem"}`. Criações retornam 201. A API diferencia dados inválidos (400), sessão inválida (401), falta de permissão (403), publicação ausente (404) e e-mail duplicado (409).

Esta etapa cobre o perfil usuária e publicações de texto. Upload de mídia, rascunhos, compartilhamento e moderação ficam para as próximas etapas. Para publicar o projeto, sirva o frontend e encaminhe `/api` ao backend no mesmo domínio.
