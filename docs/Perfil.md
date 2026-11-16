# Perfil da Usuária

## Arquitetura e regras confirmadas

- Perfil privado, acessível apenas à dona da conta. Não há perfil público nem rota
  que aceite o ID de outra usuária.
- Autenticação Bearer opaca existente, sem JWT e sem exigir adesão ao CyberTalk.
- Nome, sobre, formação e competências editáveis. E-mail somente para consulta;
  senha e token nunca fazem parte dos dados de perfil retornados.
- Nome: 1–120 caracteres não brancos; sobre: até 2000; formação: até 200;
  competências: até 500. Campos opcionais usam strings vazias.
- Certificações autodeclaradas: nome e instituição obrigatórios, até 160 caracteres;
  data de emissão obrigatória, sem data futura.
- Experiências: empresa/cargo obrigatórios, até 160 caracteres; início obrigatório,
  fim opcional, descrição até 2000 caracteres. Fim vazio significa experiência
  atual; as datas não podem estar no futuro nem fim anterior ao início.
- Datas de negócio usam America/Sao_Paulo no servidor.
- Indicadores reais: publicações próprias, comentários feitos, curtidas realizadas
  e candidaturas enviadas. Contam os registros atuais, não um histórico removido.
- Feedbacks/avaliações ficam em estado vazio. O módulo legado de avaliações refere-se
  a atividades e usa memória; não é uma fonte de avaliações recebidas pelo perfil.
- Certificações são separadas do módulo legado /api/certificados, que representa
  certificados emitidos por atividades. Nenhuma emissão ou validação de certificado
  é simulada por este perfil.
- Nenhum seed, dado fictício de produção, alteração de credencial ou exclusão de
  registros existentes acompanha a implementação.

O frontend usa Perfil.jsx, estilos locais Perfil.css e perfilService.js.
O backend segue Controller → Service → Repository com JdbcClient, reutilizando
AcessoTalk. DTOs explícitos limitam os campos editáveis. IDs recebidos em JSON
não definem a dona do registro. Toda alteração de certificação/experiência inclui
a usuária da sessão na condição SQL; registro de outra conta retorna 404.
O menu habilita Perfil. A mudança de nome também aparece nas consultas já existentes
do CyberTalk, pois elas usam a mesma tabela de contas.

O botão Sair do menu foi conectado ao DELETE /api/auth/sessao existente: revoga
o Bearer no servidor e limpa sessionStorage. Falhas de conexão mantêm o menu aberto
com mensagem para nova tentativa. Tokens do armazenamento legado também são limpos.

## PostgreSQL

Migração aditiva em backend/src/main/resources/schema.sql, executada na inicialização:
- talk_usuaria: acrescenta sobre VARCHAR(2000) e formacao VARCHAR(200), vazios por padrão.
  Reutiliza nome, email e competencias existentes.
- perfil_certificacao: id, usuaria_id FK, nome, instituicao, data; índice por usuária.
- perfil_experiencia: id, usuaria_id FK, empresa, cargo, inicio, fim, descricao;
  índice por usuária e CHECK para ordem das datas.
- perfil_foto: usuaria_id PK/FK, dados BYTEA, limite de 2 MB.

O volume dados do Compose mantém os registros entre reinicializações. Não usa
armazenamento em memória para dados do perfil nem altera as tabelas de vagas.

## API

Todos os endpoints abaixo exigem Authorization: Bearer da sessão existente.

| Método | Endpoint | Resposta |
| --- | --- | --- |
| GET | /api/perfil | 200: pessoal, certificacoes, experiencias e participacao |
| PUT | /api/perfil | 200: dados pessoais atualizados |
| POST | /api/perfil/certificacoes | 201: certificação cadastrada |
| PUT | /api/perfil/certificacoes/{id} | 200: certificação atualizada |
| DELETE | /api/perfil/certificacoes/{id} | 204 |
| POST | /api/perfil/experiencias | 201: experiência cadastrada |
| PUT | /api/perfil/experiencias/{id} | 200: experiência atualizada |
| DELETE | /api/perfil/experiencias/{id} | 204 |
| GET | /api/perfil/foto | 200: bytes PNG privados ou 404 |
| PUT | /api/perfil/foto | Multipart com campo arquivo; 204 |
| DELETE | /api/perfil/foto | 204, inclusive se já ausente |

JSON de edição:
~~~json
{"nome":"Seu nome","sobre":"","formacao":"","competencias":""}
~~~

JSON de certificação: nome, instituicao e data (AAAA-MM-DD).
JSON de experiência: empresa, cargo, inicio (AAAA-MM-DD), fim (data ou null), descricao.
Nome/instituição/empresa/cargo não aceitam conteúdo somente com espaços.

Respostas de erro: 400 validação; 401 sessão ausente/expirada; 404 registro
inexistente ou pertencente a outra conta; 413 foto acima do limite. Erros de conexão
e servidor aparecem na interface. Formulários têm estados ocupado/erro/sucesso e
bloqueio de envio repetido; exclusão de registros exige confirmação na interface.

## Foto e privacidade

A infraestrutura existente de multipart/PostgreSQL é reutilizada; o validador
de foto é específico, sem alterar anexos do CyberTalk.

- PNG/JPEG até 2 MB. Não aceita SVG, GIF, URL remota nem arquivo genérico.
- Verifica MIME declarado e formato detectado pelo decodificador.
- Valida dimensões antes de decodificar: até 4096 px por lado e 12 milhões de pixels.
- Reduz para no máximo 512 px no maior lado, preservando proporção e transparência.
- Recodifica apenas pixels em PNG novo; remove metadados e não guarda nome do arquivo.
- Conteúdo inválido é rejeitado, sem sobrescrever a foto anterior.
- Consulta privada com no-store e nosniff. GET do perfil também usa no-store.
- O frontend carrega o blob autenticado e revoga suas URLs temporárias ao desmontar.
- Não altera outras fotos ou adiciona a foto às publicações do CyberTalk.

Fotos com orientação EXIF podem precisar de rotação prévia: não há editor ou
correção automática de orientação nesta etapa.

## Arquivos desta funcionalidade

Criados:
- backend/src/main/java/CyberLadies/perfil/dto/PerfilDados.java
- backend/src/main/java/CyberLadies/perfil/repository/PerfilRepository.java
- backend/src/main/java/CyberLadies/perfil/service/PerfilService.java
- backend/src/main/java/CyberLadies/perfil/service/FotoPerfil.java
- backend/src/main/java/CyberLadies/perfil/controller/PerfilController.java
- backend/src/test/java/CyberLadies/talk/PerfilIntegrationTest.java
- frontend/src/pages/Perfil.jsx e Perfil.css
- frontend/src/services/perfilService.js
- frontend/scripts/verify-perfil.mjs
- docs/Perfil.md

Modificados:
- backend/src/main/resources/schema.sql
- frontend/src/App.jsx
- frontend/src/pages/Login.jsx
- frontend/src/components/MenuPopup.jsx
- frontend/src/services/authService.js

## Pendências e testes manuais

- Conferir teclado/leitor de tela, zoom alto, dispositivos físicos e apresentação
  visual com textos/fotos pessoais longos.
- Testar seleção de foto pelo seletor nativo e fotos pessoais com orientação EXIF;
  o teste Chrome enviou a foto pela API e conferiu a exibição após F5.
- Simular interrupção de rede e falha de servidor durante gravação, além de sessão
  expirada durante edição. O backend testa expiração; o navegador testa ausência.
- Certificações são autodeclaradas, sem anexos, download, verificação externa ou
  integração com certificados emitidos.
- Perfil público, alteração de email/senha e avaliações recebidas não fazem parte
  desta entrega, conforme as regras confirmadas.
- Anexos do CyberTalk não foram alterados; o fluxo de anexo pelo navegador não foi
  reexecutado nesta etapa.
- Listas sem paginação e edição sem controle de versão: duas abas podem sobrescrever
  dados pessoais. Melhorias para uma etapa futura.

Não foi realizado commit nem push.
## Integração posterior com CyberAcademy

O GET /api/perfil agora inclui certificadosPlataforma, provenientes de
academy_certificado. São certificados privados emitidos automaticamente ao concluir
uma trilha elegível e exibidos em uma seção própria, sem edição manual pelo Perfil.
A lista certificacoes continua autodeclarada e independente.
A limitação anterior sobre ausência de integração com certificados emitidos fica
substituída por esta implementação. Contratos, testes adicionais e limitações:
[CyberAcademy](CyberAcademy.md).