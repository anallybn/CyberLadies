# CyberLadies
Projeto Integrador III

## CyberTalk

Comunidade do CyberLadies. Nesta primeira etapa, a usuária pode criar uma conta, entrar na comunidade, publicar textos de até 500 caracteres, editar e excluir as próprias publicações, curtir e comentar. As operações usam a API REST em Spring Boot e o PostgreSQL. A adesão é explícita e o servidor verifica a autoria de cada alteração.

### Executar com Docker Desktop
Na raiz do repositório, execute `docker compose up` e aguarde a mensagem do Vite. Abra http://localhost:5173/cadastro/usuaria. Para encerrar, use Ctrl+C e `docker compose down`. Posts, comentários e anexos ficam no volume PostgreSQL entre execuções.

O botão **Compartilhar** copia um link para a publicação. Quem abrir precisa entrar e participar da comunidade. Em desenvolvimento, um link localhost só funciona na máquina que hospeda o site. Cada post aceita um arquivo de até 5 MB, com ou sem texto. JPEG e PNG têm visualização e os demais arquivos podem ser baixados.

### Abrir o CyberTalk no Windows sem Docker

Use Git Bash no VS Code, JDK 21, PostgreSQL 16 e Node.js 22.12 ou superior. Cada computador usa seu próprio banco local.

**Preparação inicial.** Baixe o projeto com `git clone https://github.com/anallybn/CyberLadies.git` e abra a pasta CyberLadies no VS Code. Se já possui o projeto, abra a cópia atualizada. No Git Bash, confira `java -version`, `javac -version` e `node -v`. O Java e o compilador devem indicar 21. Se aparecer um Java antigo, defina `JAVA_HOME` para a pasta do JDK 21 instalado e coloque `$JAVA_HOME/bin` no início do `PATH`.

Abra **SQL Shell (psql)** pelo menu Iniciar. Aceite os valores padrão para localhost, banco postgres, porta 5432 e usuário postgres. Digite a senha escolhida na instalação. No prompt `postgres=#`, execute uma linha por vez, apenas na primeira configuração:

```sql
CREATE USER cyberladies;
\password cyberladies
CREATE DATABASE cyberladies OWNER cyberladies;
```

O comando `\password` pede a nova senha duas vezes. Guarde-a para o terminal do backend.

**Terminal 1 — backend.** Abra um Git Bash na pasta do projeto e execute:

```bash
cd "$(git rev-parse --show-toplevel)/backend" && export DATABASE_USER=cyberladies
```

Cole a próxima linha sozinha. Quando solicitado, digite a senha do usuário do banco e pressione Enter. A senha fica oculta.

```bash
read -rs -p "Senha do banco cyberladies: " DATABASE_PASSWORD && export DATABASE_PASSWORD && echo && bash mvnw spring-boot:run
```

Aguarde `Started CyberladiesApplication`. Mantenha o terminal aberto. O backend usa a porta 8080 e cria as tabelas. Para usar outro banco, configure `DATABASE_URL` com um endereço JDBC PostgreSQL.

**Terminal 2 — site.** Abra um segundo Git Bash no VS Code:

```bash
cd "$(git rev-parse --show-toplevel)/frontend" && npm ci && VITE_API_URL= npm run dev
```

Deixe o valor de `VITE_API_URL` vazio, como no comando. O frontend usa o caminho padrão `/api` e o Vite encaminha a requisição ao Spring Boot. No Git Bash do Windows, definir explicitamente a variável como `/api` pode transformá-la em um caminho de arquivo.

Abra o endereço **Local** exibido pelo Vite e acesse `/cadastro/usuaria`. Crie uma conta do site, entre e clique em **Participar da comunidade**. Publique com um anexo, comente e atualize a página para conferir a persistência. Teste também editar, compartilhar e excluir a publicação.

**Para reabrir depois de fechar o VS Code**, abra a mesma pasta e repita os dois terminais. O banco já estará criado. Pode omitir `npm ci` se as dependências continuarem instaladas e o arquivo de lock estiver igual.

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

Esta etapa cobre o perfil usuária e publicações de texto. Rascunhos e moderação ficam para as próximas etapas. Para publicar o projeto, sirva o frontend e encaminhe `/api` ao backend no mesmo domínio.
