# Integração das telas para apresentação

## Estado inicial e origem

Checkpoint local `15796f5` em `correcao/preservar-modulos-main`: contém a
recuperação dos nove módulos, testes e documentação da revisão anterior.
Exclui configurações pessoais de `.vscode` e os artefatos de teste/demonstração.

Fetch realizado em 15/11/2026. A origem identificada é `origin/main`, em
`ca4cb29`. Foram comparadas as branches remotas: nenhuma outra contém versão
concorrente das páginas novas. A integração começou na branch própria
`integracao/telas-apresentacao`, sem alterar a main nem reescrever commits.

| Commit da origem | Responsabilidade |
| --- | --- |
| `08d2f7a` | Autenticação por três perfis, incompatível com nossos papéis acumuláveis |
| `6afbb0d` | CyberVagas e candidaturas persistentes |
| `c48bbb2` | Academy, trilhas, aulas, progresso, mentorias e certificados |
| `5c31e07` | Perfil privado, foto, experiências e certificações |
| `c4d6ce8` | API privada do dashboard de educadora |
| `7e6cbcf` | Persistência de rascunhos privados |
| `7b222a9` | API autorizada de alunas e progresso |
| `695fd1f` | Telas e navegação da educadora |
| `8fe4800` / `ca4cb29` | Testes e documentação da origem |

## Mapa inicial, antes da adaptação

“Parcialmente integrada” significa que há tela e API na origem, mas a
compatibilidade com nossa identidade e esquema de banco ainda precisa ser
implementada e validada. Não indica que o fluxo já funciona nesta branch.

| Tela | Ações | API necessária | Situação inicial |
| --- | --- | --- | --- |
| CyberVagas | Filtrar, consultar e candidatar-se | Vagas/candidaturas privadas | Parcialmente integrada; conflito de contrato |
| Academy, trilha, aula | Inscrever, estudar e concluir | Catálogo, inscrições e progresso | Parcialmente integrada |
| Mentorias | Consultar horários, agendar e cancelar | Disponibilidades e agendamentos | Parcialmente integrada |
| Certificado | Consultar certificado após conclusão | Certificados privados | Parcialmente integrada |
| Perfil | Editar dados/foto/experiências/certificações | Perfil da própria sessão | Parcialmente integrada |
| Dashboard educadora | Consultar indicadores e conteúdo próprio | Dashboard/autoria | Parcialmente integrada |
| Enviar material | Salvar/editar/excluir rascunho | Rascunhos privados | Parcialmente integrada; publicação indisponível |
| Meus materiais | Consultar conteúdos próprios | Autoria e catálogo | Parcialmente integrada |
| Minhas alunas | Consultar inscrições/progresso próprio | Autoria e aprovação | Parcialmente integrada; exige aprovação |
| Chat | Nenhuma operação disponível | Mensagens | Sem backend |
| CyberTalk e solicitação/análise | Adesão, posts e decisão ADMIN | APIs atuais preservadas | Funcionando no checkpoint; regressão pendente após integração |

## Conflitos que exigem definição antes da implementação

1. A origem permite dashboard e rascunhos privados para educadora pendente.
   Proposta: manter esse acesso privado; publicação permanece indisponível e
   acompanhamento de alunas exige aprovação. Isso não aprova uma educadora.
2. A origem admite candidatura somente para CYBERLADIE. Nossas educadoras também
   têm USUARIA. Proposta: autorizar pelo papel USUARIA, sempre obtendo o ID da
   sessão; empresas e ADMIN não se candidatam. A alternativa é impedir também
   educadoras de se candidatar.
3. Vagas/candidaturas na main atual substituem o CRUD em memória por catálogo e
   candidaturas persistentes. A origem bloqueia escrita pública de vagas e
   alteração de candidaturas. Proposta: incorporar esses contratos seguros,
   mantendo os demais sete módulos; registrar expressamente a mudança dos dois
   contratos da correção anterior. Não recriar um endpoint público que permita
   alterar candidatura de outra conta ou publicar vaga sem autorização.

Decisões recebidas: bloquear candidaturas de educadoras, inclusive quando possuem
USUARIA; incorporar os contratos seguros e persistentes de vagas/candidaturas da
main. O acesso privado de educadoras pendentes continua sem resposta. Portanto
dashboard e rascunhos exigem aprovação nesta integração; nenhuma ampliação de
acesso de pendentes foi implementada. A solicitação e a análise ADMIN continuam
funcionando normalmente.

## Resoluções conhecidas já preparadas

- Preservadas nossas versões de autenticação frontend, login, conclusão de
  cadastro e CyberTalk nos arquivos que tiveram conflito.
- Preservada a fixture original `schema-anterior.sql`: a origem alterou esse
  arquivo para seu esquema ampliado, o que descaracterizaria nosso teste de adoção.
- Não recuperados `talk/ContaApi` antigo ou CORS direto, que continuam substituídos
  pela autenticação atual e proxy. A configuração Flyway atual permanece.
- Páginas, assets e módulos novos da origem estão na árvore de integração para
  adaptação; nenhum backend completo foi escolhido como substituição do outro.

## Resultado integrado

As telas, estilos, assets e componentes novos da origem foram mantidos. As rotas
novas convivem com CyberTalk, solicitação e análise ADMIN. O menu mantém foco,
Escape, logout remoto, as cores da área da educadora e links aos novos percursos.
O retorno seguro ao destino depois do login funciona para vagas/Academy/perfil;
ADMIN mantém prioridade para sua área administrativa.

Os módulos novos usam a identidade de `AcessoAutenticado`, não outro mecanismo
de login. `/api/auth/conta` é uma projeção compatível com o visual da origem;
`/api/auth/me` continua sendo a fonte dos papéis acumuláveis. Nenhum papel é
aceito da URL ou do armazenamento do navegador. As chamadas novas passam pelo
cliente HTTP compartilhado, incluindo Bearer, 401/403 e idempotência de rascunhos.

Foram acrescentadas migrações aditivas V6–V9: vagas/Academy/perfil, autoria,
rascunhos e vínculo de empresa à conta. V1–V5 não foram alteradas. As referências
dos módulos novos apontam para `conta` e `educadora(conta_id)`, sem recriar
`talk_usuaria` ou o cadastro paralelo `auth_educadora`. Perfil mantém competências
em `usuaria` e dados pessoais em `conta`.

O cadastro de empresa da origem foi integrado à autenticação atual, criando
somente EMPRESA, validando CNPJ e armazenando hash de senha. Não concede acesso
pessoal, de estudante, a candidaturas ou administração. A educadora continua
usando conta pessoal + solicitação + decisão ADMIN, sem cadastro paralelo.

| Percurso | Situação final e limite |
| --- | --- |
| CyberTalk | Funcionando: adesão, publicação/anexo, comentários, curtidas, edição/exclusão e sessão |
| Solicitação e ADMIN | Funcionando: solicitação, consulta, listagem/filtros, aprovação/rejeição e conflitos |
| CyberVagas | Funcionando: catálogo/filtros/detalhe e candidatura salva da própria usuária; educadoras bloqueadas |
| Academy | Funcionando: catálogo, inscrição, conclusão manual salva e certificado da trilha |
| Mentorias | Funcionando: disponibilidade, reserva e cancelamento persistentes |
| Perfil | Funcionando para usuária sem EDUCADORA: dados/foto/experiências/certificações e certificados da plataforma |
| Dashboard | Funcionando para educadora aprovada, somente com dados e autoria próprios |
| Meus materiais | Funcionando: conteúdo público próprio + rascunhos privados, filtros e continuação da edição |
| Enviar material | Funcionando: criar/editar rascunho com persistência, revisão e idempotência; publicação indisponível |
| Minhas alunas | Funcionando para aprovada: inscrições/progresso das próprias trilhas e detalhe sem perfil privado |
| Empresa | Cadastro/login e consulta pública do catálogo; escrita de vagas não disponível na interface |
| Chat | Indisponível; botão identificado como Em breve |

Rascunhos não têm exclusão ou arquivamento nesta etapa. A tabela inicial acima
superestimava a ação ao mencionar excluir: a origem oferece criar e editar, e
isso foi mantido. Não há simulação de publicação, envio de arquivo, mensagem ou
gravação que não tenha API.

## Diferenças funcionais contra origin/main

Nenhum módulo de Academy, perfil, vagas ou área da educadora foi omitido. As
diferenças de comportamento foram registradas, inclusive as incompatibilidades:

| Diferença | Tratamento |
| --- | --- |
| Cadastro público separado de educadora | Substituído pela conta pessoal e solicitação/análise atuais; não há rota paralela que contorne aprovação |
| CRUD antigo `/api/educadoras` | Continua ausente: operações por ID sem autorização não foram recuperadas; proposta anterior permanece em CORRECAO_INTEGRACAO.md |
| Dashboard/rascunhos de pendentes e rejeitadas | Bloqueados até definição; o percurso aprovado está completo. A origem permitia acesso privado sem aprovação |
| Perfil de educadora | Continua indisponível, como na origem. O perfil privado é exclusivo da usuária sem EDUCADORA |
| Auth da origem com perfil único | Adaptado aos papéis atuais; login/sessão/cadastro pessoal/empresa preservados, EDUCADORA pode coexistir com USUARIA |
| Inicialização schema.sql e SQL avulso de auth | Substituídos por Flyway, sem baseline automático nem alteração de migrações antigas |
| CORS direto | Não recuperado: fluxos suportados continuam pelo proxy /api; acesso de outra origem não é um fluxo validado |
| CRUDs em memória de vagas/candidaturas da correção anterior | Substituídos pelos contratos seguros da main, conforme decisão recebida; não é perda contra a main atual |

Agenda, avaliações, certificados legados, cursos legados, empresas legadas,
mentorias legadas e notificações continuam presentes em memória, com contratos
iguais aos da main. Os catálogos novos de Academy e vagas são módulos separados;
criar um curso no CRUD legado não o publica no Academy. Os módulos legados ainda
não validam identidade/autoria e perdem estado ao reiniciar, limitações anteriores
que não foram ampliadas nem escondidas como novos percursos de apresentação.

Documentos e scripts de validação importados da origem foram preservados como
históricos, com aviso. Não executar o perfil antigo auth-validacao nem os SQLs
avulsos sobre esta integração. Os comandos atuais estão neste documento e em
frontend/VALIDACAO.md; `schema.sql` não voltou como inicializador.

## Ambiente local e preservação da demonstração

Abra **http://127.0.0.1:5178**. API em `127.0.0.1:18083`, proxy `/api` preservado.
Banco **existente e persistente** `cyberladies_apresentacao` em `55439`.
Seu diretório original foi movido de TEMP para
`%LOCALAPPDATA%\CyberLadies\apresentacao\postgresql\data`, após backup integral e
recuperação validada. Os comandos e evidências atuais estão em
[AMBIENTE_LOCAL.md](AMBIENTE_LOCAL.md).
O banco da demonstração anterior não foi conectado, migrado ou sobrescrito.
Como ele não foi usado, não houve necessidade de backup ou atualização dele.
As migrações e a adoção do esquema anterior foram ensaiadas em outros bancos
novos isolados pela suíte de migração, preservando contas/sessões/posts/anexos.

Há contas locais de usuária, educadora aprovada, ADMIN e empresa. Os acessos ficam
somente em `%LOCALAPPDATA%\CyberLadies\apresentacao\ACESSOS-LOCAIS.md`, fora do Git e
das pastas de limpeza. A aprovação da
educadora de apresentação foi realizada pela API ADMIN atual. O catálogo inclui
uma vaga identificada como demonstrativa, uma trilha com aula e um horário de
mentoria. Não correspondem a oportunidades ou atividades reais.

Para reabrir neste computador, na raiz do projeto:

```powershell
.\scripts\Iniciar-Apresentacao.ps1
```

Para parar, use `.\scripts\Parar-Apresentacao.ps1`. O comando de iniciar usa
sempre a instância persistente cadastrada e o JAR em `backend/target`. Não cria
ou restaura bancos nem recria contas. Logs ficam na pasta permanente descrita
em AMBIENTE_LOCAL.md. Depois de reiniciar o Windows, o mesmo comando reutiliza
esse armazenamento. Para recompilar o JAR, executar `mvn package` com os testes
apontados aos bancos exclusivos de teste; nunca ao banco de apresentação.

## Roteiro curto (16/11/2026)

1. Entrar como usuária. CyberTalk: participar, publicar uma mensagem, recarregar
   e mostrar que ela permanece; comentar/curtir e editar ou excluir.
2. CyberVagas: filtrar, abrir a vaga demonstrativa, candidatar-se e recarregar.
   Mostrar candidatura enviada. A educadora e a empresa não podem se candidatar.
3. Perfil: editar Sobre você, salvar e recarregar. Academy: abrir a trilha,
   inscrever-se, abrir a aula, marcar a conclusão e consultar o certificado no
   perfil. Conclusão é manual, sem comprovação automática de tempo de vídeo.
4. Mentorias: escolher mentora/horário, confirmar e recarregar; cancelar para
   demonstrar a transição persistida. O horário volta a ficar disponível.
5. Entrar como educadora aprovada. Dashboard mostra a inscrição/conclusão da
   usuária. Abrir Minhas Alunas e Ver progresso. Criar rascunho em Enviar Material,
   salvar, recarregar e encontrá-lo em Meus Materiais. Publicar continua desabilitado.
6. Entrar como ADMIN para mostrar solicitações e decisões. Para uma nova análise
   ao vivo, criar outra conta pessoal e solicitar atuação; decidir pelo ADMIN.
   Não mudar papéis pelo banco ou pela URL durante a apresentação.

Em nova execução, o botão de candidatura e conclusão pode já estar concluído:
isso demonstra persistência. Para repetir a criação, cadastrar outra usuária,
sem apagar os dados existentes. Não anunciar publicação editorial, chat, envio
de PDF/MP4, arquivamento/exclusão de rascunho ou moderação como disponíveis.

## Validação

Resultados finais: **110 testes backend**, **8 testes unitários frontend** e
**8 cenários Playwright/Chrome** aprovados, sem falhas, erros ou ignorados.
Build de produção, lint e empacotamento do backend aprovados. Capturas desktop
de vagas/certificado/dashboard e mobile de materiais foram inspecionadas; não
houve overflow horizontal no cenário mobile. Os testes usam somente
`cyberladies_auth_test`, os bancos próprios de migração e
`cyberladies_frontend_e2e` em `55439`, separados do banco da apresentação.
Fixtures das suítes portadas criam contas/papéis reais nas tabelas atuais, sem
views de compatibilidade ou desativação das restrições. O teste de autenticação
da origem foi reconciliado com cadastro pessoal + solicitação e cadastro EMPRESA.

Não houve ensaio Docker, Firefox ou Safari, nem validação de reprodução completa
de vídeo externo. Os testes de vídeo verificam IDs/provedores e o fluxo de estudo;
conclusão manual não comprova tempo assistido. As limitações da origem (sem fluxo
editorial, sem PDFs/MP4, sem arquivamento/exclusão de rascunho e sem chat) continuam
explícitas. Falha de gravação exibe erro e preserva edição, sem indicar sucesso.
