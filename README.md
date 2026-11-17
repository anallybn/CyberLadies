# CyberLadies — Projeto Integrador III

O CyberLadies reúne uma comunidade de mulheres em tecnologia, oportunidades de
estudo e atuação de educadoras. O objetivo é apoiar troca de conhecimento com
identidade autenticada, autoria das publicações e aprovação administrativa.

O **CRUD principal da entrega é o CyberTalk**: criar publicação, consultar lista
ou uma publicação por ID, editar e excluir a própria publicação, inclusive com
anexo. Recarregar consulta dados persistidos no PostgreSQL. Curtidas, comentários,
links compartilhados e adesão explícita à comunidade também estão disponíveis.

Outros percursos existentes: solicitação/análise de educadora, área de educadora
aprovada, rascunhos, catálogo Academy, inscrições/progresso, certificados elegíveis,
mentorias por disponibilidade, vagas/candidaturas da sessão, perfil privado e
CyberChat por vínculos. Há recursos de acessibilidade (contraste, texto 100–200%,
teclado e preferências persistentes). O perfil comunitário de autoras mostra só
identidade e publicações; dados privados não são compartilhados automaticamente.

**Estado da entrega:** preparação local validada; publicação e verificação remota
pendentes. Consulte a [auditoria com evidências e pendências](docs/ENTREGA_FINAL.md).
Não considere uma tela, documentação histórica ou configuração como prova de deploy.

## Integrantes

Nomes abaixo são os registrados no histórico Git; confirmar nomes completos e
lista oficial antes do envio. Os RMs ainda não foram informados.

| Integrante registrado | RM | Evidências de contribuição no Git |
| --- | --- | --- |
| Anally Noronha | A preencher | Telas e integração Academy/perfil/educadora, Chat (`b3c30be`, `695fd1f`) |
| Laura Macedo | A preencher | CRUDs originais em memória: agenda, avaliações, cursos e outros (`688fae4`, `1fe447d`, `69acec1`) |
| Maria Eduarda Silva | A preencher | Integração, autenticação/papéis, operação persistente, acessibilidade e fechamento (`315460c`, `4caf700`) |

Líder responsável pelo envio: **A preencher**. Há aliases e outros nomes no Git;
confirmar correspondências com a equipe. Commits são evidência de alterações, não
prova de trabalho exclusivo. Não se atribui à equipe uma autoria diferente do Git.

## Tecnologias e organização

Java 21, Spring Boot 4.1.1, Spring MVC, Bean Validation, JdbcClient, PostgreSQL 16 e
Flyway V1–V10; React 19, React Router, Vite 8, CSS e Lucide; JUnit/MockMvc e
Playwright/Chrome. O caminho principal usa SQL parametrizado, sem migração para JPA.

| Pasta/camada | Responsabilidade |
| --- | --- |
| `backend/src/main/java/CyberLadies` | Módulos por funcionalidade |
| Controllers / APIs | Entrada HTTP, validação de DTO, status e cabeçalhos |
| Services | Casos de uso, acesso/autoria e fronteiras transacionais |
| Repositories | Consultas e gravação parametrizada via JdbcClient |
| Domínio e DTOs | `Publicacao` contém a regra de autoria; `TextoPublicacao` é entrada e `PublicacaoResponse` é saída |
| `backend/src/main/resources/db/migration` | Evolução versionada, sem editar versões já aplicadas |
| `frontend/src/pages`, `components`, `services`, `styles` | Telas, componentes reutilizáveis, acesso à API e estilos |
| `backend/src/test`, `frontend/tests` | Testes; dados sintéticos em bancos isolados |
| `docs`, `scripts` | Guias e comandos operacionais |

No CyberTalk, `ComunidadeApi → ComunidadeService → PublicacaoRepository`; consultas
reaproveitam `PublicacaoConsultaService`. Modelo JDBC não exige `@Entity`. A
presença de dependências JPA no projeto não transforma os módulos em memória em
persistentes. Veja [banco e restrições](docs/BANCO_ENTREGA.md).

## Instalar após clonar em outra máquina

Clone o repositório e use o commit/branch indicado pela equipe. Não copie o banco
pessoal, diretórios de PostgreSQL, `.env` ou instruções privadas de acesso.

### Opção Docker para desenvolvimento novo

Requer Docker Engine/Desktop com Compose v2, internet e porta 5173 livre.

1. Copie `.env.example` para `.env` e defina uma senha própria em
   `DATABASE_PASSWORD`. O arquivo `.env` é ignorado pelo Git. `APP_DOMAIN` não é
   necessário nesta opção de desenvolvimento.
2. Execute `docker compose up --build` na raiz. Flyway cria o schema no banco
   vazio do volume nomeado `dados`; aguarde a API e o Vite.
3. Abra http://localhost:5173/cadastro/usuaria. API pelo proxy:
   http://localhost:5173/api. A API e o banco não expõem portas diretas ao host.
4. Pare com `docker compose down`. O volume permanece. Não use `down -v` se
   precisar preservar os dados. Não aplique essa instalação sobre volumes antigos
   sem seguir o [procedimento de backup/adoção](docs/MIGRACOES.md).

A senha definida na criação do volume deve continuar igual; mudar uma variável
não redefine a senha de um PostgreSQL já inicializado. Este Compose é de
**desenvolvimento**, não a publicação final. Os contêineres ainda precisam ser
ensaiados em máquina com Docker disponível; não foram executados neste fechamento.

### Opção sem Docker

Requer Java 21, Node.js 22.12+ e PostgreSQL 16; inclua os executáveis no PATH.
Crie banco vazio e usuário proprietário, usando entrada protegida para a senha:

```sh
createuser -h 127.0.0.1 -p 5432 -U postgres --pwprompt cyberladies
createdb -h 127.0.0.1 -p 5432 -U postgres -O cyberladies cyberladies
```

Defina no terminal da API `DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/cyberladies`,
`DATABASE_USER=cyberladies` e `DATABASE_PASSWORD` com a senha escolhida. PowerShell:

```powershell
$env:DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/cyberladies'
$env:DATABASE_USER='cyberladies'
$senhaBanco=Read-Host 'Senha do banco' -AsSecureString
$env:DATABASE_PASSWORD=[Net.NetworkCredential]::new('', $senhaBanco).Password
cd backend
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, exporte as mesmas variáveis e use `./mvnw spring-boot:run`.
A API local escuta em http://127.0.0.1:8080. Flyway cria as tabelas; não execute
`schema.sql`, seeds de demonstração ou scripts históricos como inicializadores.

Em outro terminal: `cd frontend`, `npm ci` e `npm run dev`. Abra o endereço que
Vite informar (normalmente http://127.0.0.1:5173). O proxy encaminha `/api` para
8080. Para mudar a API de desenvolvimento, defina `API_PROXY`; não use um endereço
local em `VITE_API_URL` no build publicado.

Crie uma conta em `/cadastro/usuaria`, faça login e participe da comunidade.
Para ADMIN, use o [comando operacional documentado](docs/ADMINISTRADOR.md) com o
banco correto; não existe criação automática de administrador na inicialização.

### Ambiente pessoal já existente — diferente da instalação acima

Na máquina da apresentação, o site continua em **http://127.0.0.1:5178**, API
interna em 18083 e banco **cyberladies_apresentacao em 55439**. O armazenamento
fica fora do repositório, em `%LOCALAPPDATA%\CyberLadies\apresentacao`.

Iniciar: `.\scripts\Iniciar-Apresentacao.ps1`. Parar:
`.\scripts\Parar-Apresentacao.ps1`. Esses comandos exigem a instância existente;
não criam banco alternativo, contas ou senhas. Não se destinam a uma instalação
nova em outra máquina. Veja [ambiente local](docs/AMBIENTE_LOCAL.md).

## Testes

Frontend: na pasta `frontend`, execute `npm ci`, `npm run lint`, `npm run build`
e `npm test`. Para navegador, instale Chrome ou execute `npx playwright install
chromium` e defina `E2E_BROWSER=chromium` (o padrão usa Chrome).

Os testes PostgreSQL têm guardas explícitas. Em um servidor de testes PostgreSQL
16 na porta 55439, crie bancos **isolados** `cyberladies_auth_test`,
`cyberladies_auth_chat_test` e `cyberladies_frontend_e2e`, com proprietário
`migration_test` e permissão CREATEDB para o ensaio efêmero de banco vazio.
Não altere o usuário nem os bancos pessoais para isso. Configure as credenciais
exclusivas de teste por ambiente e `.pgpass`/`pgpass.conf` protegido para os
subprocessos psql do Playwright, se a autenticação exigir senha.

Na pasta `backend`, com `DATABASE_URL=jdbc:postgresql://127.0.0.1:55439/cyberladies_auth_test`,
`DATABASE_USER=migration_test` e a senha de teste, execute `./mvnw test` (Windows:
`.\mvnw.cmd test`). `EntregaBancoVazioTest` cria seu próprio banco efêmero, migra,
valida e remove somente esse banco. Os quatro ensaios históricos de adoção exigem
preparação adicional e `MIGRATION_TESTS=true`; não são executados por padrão.

Para Playwright, inicie outra API na porta 18081 apontando exclusivamente para
`cyberladies_frontend_e2e`, com as mesmas credenciais de teste. Use
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18081` após definir
as variáveis desse banco no terminal da API. Em `frontend`, defina
`E2E_ISOLATED=true` e execute `npm run test:e2e`. O runner abre Vite 5175 com proxy
18081. Não aponte essa API para o banco de apresentação: esses cenários escrevem
fixtures e alguns as removem. [Resultados deste fechamento](docs/ENTREGA_FINAL.md).

## API REST principal

Prefixo `/api`. Sessão via `Authorization: Bearer <token>` com expiração de oito
horas. As operações recebem JSON, salvo upload multipart. IDs são da conta real;
o servidor não aceita autoria definida pelo nome ou pelo cliente.

| Método | Endpoint | Resultado / acesso |
| --- | --- | --- |
| POST | `/auth/cadastro/usuaria` | 201; cadastro público validado |
| POST | `/auth/login` | 200; token; senha incorreta não autentica |
| GET / DELETE | `/auth/me` / `/auth/sessao` | Identidade/papéis / logout |
| GET / PUT | `/usuaria` / `/comunidade/adesao` | Identidade comunitária / adesão explícita |
| POST | `/posts` | 201; texto até 500 caracteres ou multipart com anexo até 5 MB |
| GET | `/posts` | 200; lista da comunidade |
| GET | `/posts/{id}` | 200; leitura individual ou 404 |
| PUT | `/posts/{id}` | 200; edição somente pela autora |
| DELETE | `/posts/{id}` | 204; exclusão somente pela autora, com dependências em cascata |
| GET | `/posts/{id}/anexo` | Download autenticado, no-store e nosniff |
| PUT / DELETE | `/posts/{id}/curtida` | Curtir / retirar curtida |
| GET / POST | `/posts/{id}/comentarios` | Consultar / comentar |
| GET | `/comunidade/perfis/{id}` e `/{id}/publicacoes` | Projeção mínima e publicações paginadas |
| GET / POST | `/educadora/solicitacao` | Consultar / solicitar; PENDENTE não dá acesso à área aprovada |
| GET | `/admin/educadoras/solicitacoes` e `/{contaId}` | Consulta exclusiva ADMIN |
| POST | `/admin/educadoras/solicitacoes/{contaId}/decisao` | Aprovar/rejeitar solicitação pendente |
| GET | `/educadora/dashboard` | Papel EDUCADORA e aprovação obrigatórios |

O CRUD usa **PUT**; PATCH não está implementado. Diferencia 400, 401, 403, 404,
409 e 413 quando aplicáveis. Anexos permanecem no PostgreSQL; excluir um post
remove anexo, curtidas e comentários associados.

## Publicação e links

| Recurso | URL real verificada |
| --- | --- |
| Repositório | https://github.com/anallybn/CyberLadies |
| Frontend publicado | **A preencher — publicação não verificada** |
| API publicada | **A preencher — publicação não verificada** |
| Local pessoal (não é publicação) | http://127.0.0.1:5178 |

Existe configuração SPA da Vercel em `frontend/vercel.json`; nenhum projeto
vinculado ou URL real foi encontrado. Reaproveite-a com API HTTPS e CORS restrito,
ou utilize os Dockerfiles, Caddy e `compose.production.yaml` em servidor já
autorizado. Não foi contratado serviço nem copiado banco pessoal.
[Passos exatos de publicação e verificação](docs/PUBLICACAO.md).

## Limitações conhecidas

- Deploy, URLs reais, RMs e identificação do líder ainda impedem a entrega final.
- Agenda, avaliações, certificados legados, cursos, empresas e mentorias legadas
  usam listas em memória. Notificações só manipulam o objeto recebido; não há
  persistência. Isso é diferente dos certificados/mentorias/catálogo do Academy,
  empresas de vagas e candidaturas persistidos no PostgreSQL.
- Os endpoints legados em memória não têm a autorização integrada do CyberTalk;
  precisam de revisão de acesso antes de exposição pública irrestrita. Não são
  o CRUD persistente demonstrado. Não foram removidos ou convertidos neste trabalho.
- Não há publicação editorial, moderação, amizades ou sugestões. Aprovação de
  educadora não libera APIs editoriais bloqueadas. Não há redefinição pública de senha.
- Foto e apresentação privadas não são divulgadas no perfil comunitário.
- Acessibilidade não significa certificação WCAG; auditoria completa de leitores
  de tela e outros navegadores não foi executada.

[Roteiro de até 15 minutos e checklist do líder](docs/ROTEIRO_ENTREGA.md).
