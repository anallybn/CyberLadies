# Publicação gratuita do CyberLadies — preparação, sem deploy

Verificação das condições públicas: 17/11/2026. Nenhum recurso externo foi criado.
Este guia não autoriza criação de serviços, migrações, commit, push ou deploy.
Os nomes dos botões podem mudar; conferir a indicação Free e US$ 0 antes de confirmar.

## Histórico e estado inicial

Branch main, HEAD ff6bfd1 (merge do PR #22). O commit 4dfaba7 é ancestral de HEAD.
Permanecem no histórico b3c30be (CyberChat), 8c3f019 (dashboard) e 4dfaba7
(Enviar Material). Merges posteriores explicam o novo HEAD. Não houve troca de
branch nem reescrita do histórico durante esta preparação.

## Combinação recomendada, sem cartão

- API: Render Web Service Free, Docker, 512 MB, HTTPS gerenciado, 750 horas
  gratuitas compartilhadas por workspace/mês. Suspende após 15 minutos sem tráfego;
  a próxima visita pode aguardar aproximadamente um minuto para a retomada.
- Banco: Neon Free, PostgreSQL 16, separado de qualquer banco local. A página atual
  informa US$ 0, sem cartão e sem prazo fixo de expiração: 100 CU-h/mês/projeto,
  1 GB de PostgreSQL/projeto e 5 GB de transferência pública/mês/projeto.
  Ao esgotar computação/transferência, suspende até o próximo período; ao esgotar
  armazenamento, operações que aumentem o espaço falham. Não habilitar plano pago,
  AI Gateway, créditos, adicionais ou outro serviço.
- Neon reduz computação a zero após 5 minutos sem atividade; preserva os dados.
  O histórico de recuperação gratuito é limitado (consultar os limites no painel),
  não substitui uma cópia independente. Não contar com backups pagos/automáticos.
- Render informa suspensão dos serviços/builds quando cotas são excedidas sem
  método de pagamento. Não cadastrar cartão. Não é possível garantir previamente
  que a verificação antifraude de uma conta específica não solicitará pagamento:
  se isso ocorrer, interromper e avaliar outra hospedagem antes de criar recursos.
- Render pode suspender serviços gratuitos com tráfego externo excepcionalmente
  alto, inclusive consultas a banco externo. Não há SLA nem garantia de disponibilidade.
- Alternativa: PostgreSQL Free do próprio Render tem 1 GB, expira 30 dias após a
  criação e não tem backups. Após a expiração fica inacessível, com prazo de 14 dias
  para upgrade antes da exclusão. Como não haverá upgrade pago, não é a opção
  principal. Não existe data de expiração calculável antes de criar o recurso.

Fontes oficiais:
- https://render.com/docs/free
- https://render.com/docs/docker
- https://neon.com/pricing
- https://neon.com/docs/introduction/plans
- https://neon.com/docs/guides/java

## Preparação local implementada

Sete controllers legados recebem @Profile("!production"): agenda, avaliações,
certificados legados, cursos, empresas legadas, mentorias legadas e notificações.
No perfil production suas rotas /api/agenda, /api/avaliacoes, /api/certificados,
/api/cursos, /api/empresas, /api/mentorias e /api/notificacoes não são registradas
(404). Sem production continuam disponíveis, inclusive no perfil padrão.

A busca no frontend não identificou chamadas a essas sete famílias. Não confundir
com /api/academy/me/certificados, /api/academy/agendamentos e cadastro empresarial
/api/auth/cadastro/empresa: essas rotas persistentes permanecem disponíveis.
Os módulos legados em memória não devem ser apresentados como CRUD persistente.

CORS aceita Authorization, Content-Type e Idempotency-Key, sem cookies credenciados.
O perfil de produção usa https://cyber-ladies.vercel.app como origem padrão exata.
Não substituir por *, HTTP, URLs com caminho ou domínios de preview. Variáveis no
painel podem sobrescrever propriedades: revisar o valor efetivo antes do deploy.
CORS não substitui autenticação/autorização.

Datasource no perfil production exige DATABASE_URL, DATABASE_USER e DATABASE_PASSWORD,
sem fallback local. Não preencher com credenciais locais nem versionar valores reais.

GET /api/health retorna somente {"status":"UP"}. É liveness da aplicação, não prova
conectividade com PostgreSQL nem migrações concluídas. Não consulta banco, não expõe
versão, usuários, credenciais ou mensagens de erro internas.

O Dockerfile mantém Java 21 e usuário sem privilégios. Porta dinâmica via PORT.
Heap limitado a 50% da memória do container, com SerialGC. Em 512 MB isso reserva
aproximadamente 256 MB para heap e o restante para metaspace, threads, buffers,
bibliotecas nativas e demais necessidades. Hikari: máximo 4 conexões, mínimo ocioso
0, idle timeout de 60 s e keepalive desativado para não acordar o banco sem uso.
Tomcat: máximo 30 threads, mínimo 4. Essas medidas não comprovam que a aplicação
inteira caberá: medir RSS, picos de inicialização, login, upload e concorrência no
primeiro deploy autorizado. Se não couber, interromper e revisar; não contratar upgrade.

O computador desta preparação tem JDK 25; Maven compila com release 21. A simulação
com MaxRAM=512m mostrou heap estimado em 247,50 MB. Isso não mede a aplicação nem
substitui validação da imagem Java 21 sob limite real de 512 MB. Não foi iniciado
container, servidor Spring Boot ou datasource durante esta etapa.

Autenticação preservada: tokens aleatórios opacos, somente hash persistido, sessão
com expiração, senhas protegidas por PBKDF2 e consultas parametrizadas. Identidade e
papéis são recuperados do banco, não da URL. Não há proteção robusta contra abuso
ou limitação de tentativas identificada no código atual: monitorar limites do plano
na demonstração e tratar endurecimento adicional separadamente. Nenhuma senha ou
token deve aparecer em logs, capturas, documentação ou histórico Git.

## Revisão estática das migrações — não executadas

| Versão | Finalidade / atenção |
| --- | --- |
| V1 | Estrutura inicial de contas, sessões e CyberTalk, sem contas de exemplo. |
| V2 | Renomeia tabelas, transfere competências/participação e depois remove as duas colunas antigas; não é puramente aditiva. |
| V3 | Papéis e combinação permitida; atribui USUARIA às contas já existentes. |
| V4 | Educadora com status inicial PENDENTE. |
| V5 | Metadados de decisão administrativa. |
| V6 | Vagas, candidaturas, perfil, Academy e triggers de integridade/conclusão. |
| V7 | Autoria imutável e tema de mentoria; não atribui trilhas automaticamente. |
| V8 | Rascunhos privados, revisão e validação de trilha da proprietária. |
| V9 | Vínculo empresarial da conta. |
| V10 | Contextos, participantes, mensagens, leitura e triggers do CyberChat. |

CREATE OR REPLACE TRIGGER exige PostgreSQL 14 ou superior; gen_random_uuid é nativo
nas versões escolhidas. Preferir PostgreSQL 16, alinhado ao Compose existente.
PL/pgSQL, UUID, BYTEA e índices parciais não demandam extensão extra identificada.
Os INSERTs de V2/V3 migram registros existentes; inserts de certificados e participantes
em funções acontecem quando houver operações de negócio. Não há seed automático de
contas ou materiais. Em banco novo, começa sem dados de negócio; Flyway cria estrutura
e seu histórico. Não cria ADMIN automaticamente nem importa dados locais.

Não alterar V1–V10 nem seus checksums. clean-disabled=true, baseline-on-migrate=false,
validate-on-migrate=true e ddl-auto=none foram preservados. Alguns comentários em
V7/V8 registram o escopo histórico isolado, mas não são barreiras técnicas de execução.
Flyway permanece habilitado: iniciar o backend executa migrações pendentes. Isso exige
autorização específica para o banco novo. A compatibilidade foi revisada estaticamente;
a execução completa requer autorização para banco descartável ou para o novo banco.
Nenhuma migração deve ser executada sobre um banco existente nesta preparação.

## Validações sem banco

No diretório backend:

```powershell
.\mvnw.cmd '-Dtest=ModulosMainTests,ProducaoWebTests,AutenticacaoSemBancoTests' test -B -ntp
```

A seleção é obrigatória: não executar indiscriminadamente testes que usam PostgreSQL.
São testes MVC com contextos explícitos e repositórios simulados, sem auto-configuração
Spring Boot, Flyway ou datasource. Cobrem compatibilidade local, ausência dos sete
controllers em produção, health, preflight CORS, variáveis obrigatórias, formato Bearer,
falhas de login, emissão de token com hash e identidade obtida pela sessão.

## Próximas etapas — somente depois da autorização correspondente

### B: contas e conferência do gratuito

1. Acessar neon.com → Sign up; cadastrar conta ou usar GitHub. Confirmar plano Free,
   US$ 0, nenhum cartão. Acessar render.com → Get Started e conferir workspace gratuito.
2. Não conectar serviços extras nem aceitar trial pago ou verificação com cartão.
3. Confirmar no painel as cotas e as condições atuais antes de criar o banco.

### C: banco separado e preparação do serviço

1. Após autorização: Neon → New Project; nome exclusivo para produção acadêmica,
   PostgreSQL 16, plano Free, região próxima da API. Não importar banco local.
2. Connect → Java/JDBC; escolher conexão direta (sem pooler) para este primeiro
   serviço pequeno e suas migrações. Copiar informações somente entre os painéis.
   Converter para JDBC com host/porta/banco fornecidos: DATABASE_URL deve começar
   com jdbc:postgresql://, não postgres://. Não embutir usuário/senha na URL.
3. Manter TLS conforme o exemplo JDBC oficial do Neon (sslmode=require e channel
   binding quando indicado). require cifra, mas não valida sozinho a identidade do
   servidor; quando configurar verify-full, usar a cadeia de confiança apropriada.
   Não desativar TLS para resolver problemas de conexão.
4. Render → New → Web Service → conectar somente o repositório CyberLadies.
   Language: Docker; contexto raiz do repositório; Dockerfile: backend/Dockerfile;
   instância Free; health check /api/health; deploys automáticos desativados.
5. Preencher o formulário com as variáveis abaixo. Não clicar Create Web Service /
   Deploy Web Service ainda: esse botão normalmente inicia build e aplicação.
   A criação efetiva do serviço com primeiro deploy fica para autorização da etapa D.

| Variável | Valor esperado, preenchido somente no painel |
| --- | --- |
| SPRING_PROFILES_ACTIVE | production |
| DATABASE_URL | JDBC do banco novo no Neon, com TLS e sem credenciais embutidas |
| DATABASE_USER | Usuário exclusivo do banco novo |
| DATABASE_PASSWORD | Senha exclusiva, obtida no painel Neon |
| APP_CORS_ORIGINS | https://cyber-ladies.vercel.app |

PORT é fornecida pelo Render. Não sobrescrever JAVA_TOOL_OPTIONS com o valor de 75%
do Compose local. O Compose de servidor próprio não será executado nessa arquitetura.
Nenhuma credencial deve ser colada na conversa nem adicionada ao GitHub.

### D: autorização específica para primeiro deploy e Flyway

Antes de clicar no botão que inicia o serviço, conferir projeto/banco/host separados,
plano Free, ausência de cartão, variáveis e revisão exata do código. As correções
precisam estar na revisão usada pelo Render: como commit e push não foram autorizados,
isto é uma dependência pendente, não será contornada publicando a revisão antiga.

Após autorização explícita de inicialização e migrações no banco novo: iniciar o
primeiro deploy, acompanhar logs sem credenciais e medir memória. Anotar a URL HTTPS
real fornecida pelo Render; não inventar URL antecipada. Conferir GET /api/health,
GET /api/vagas e GET /api/academy/catalogo retornando JSON. O health não substitui
as consultas persistentes. Rota protegida sem Bearer deve retornar 401/403.

### E: integração Vercel com autorização separada

Configurar VITE_API_URL como a URL HTTPS real da API acrescida de /api, sem barra
final. O código atual já aceita essa configuração e bloqueia localhost no build de
produção. Bearer continua no cabeçalho Authorization; não introduzir cookies/JWT.
O vercel.json atual só tem fallback de SPA, não proxy de API. A chamada direta é o
caminho inicial e exige CORS já preparado. Um proxy /api seria alternativa com rewrite
externo antes do fallback, mas não será criado sem decisão/autorização específicas.

A variável VITE_ é incorporada no build, portanto mudar apenas o painel não muda o
JavaScript já publicado. Novo deploy do frontend e eventual alteração do Ignored
Build Step exigem autorização; não reativar builds nem modificar Vercel nesta etapa.

## Evidências para a apresentação

Após autorização, verificar HTTPS/JSON, cadastro/login, rota protegida e CRUD
persistente pelo CyberTalk: adesão à comunidade e GET/POST/PUT/DELETE /api/posts.
Criar apenas contas e registros de teste expressamente autorizados no banco novo.
Verificar os dados após reiniciar o serviço e a integração pela aba Network do
frontend público. Registrar URLs, versão publicada e data sem expor Bearer ou senhas.

Antes da apresentação, abrir a API e o frontend com antecedência para aguardar
retomada do Render/Neon. Não usar monitor artificial para burlar inatividade.
Conferir cotas, não considerar serviço gratuito como disponibilidade garantida.
A data da apresentação ainda não foi informada. Não há comprovação de backend
publicado enquanto estes passos não forem executados e validados.
