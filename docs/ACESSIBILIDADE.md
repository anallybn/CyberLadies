# Recursos de acessibilidade

O botão circular **Acessibilidade** flutua no canto inferior esquerdo de todas as
páginas, sem reservar espaço no layout. No celular fica afastado da borda inferior.
O painel começa fechado e abre um popover de aproximadamente 280px, limitado à
largura e altura disponíveis, com rolagem interna quando necessário. Oferece alto contraste,
texto entre 100% e 200% em passos de 10% e restauração do padrão. As preferências
ficam no `localStorage` do navegador; restaurar remove a preferência salva.
Se o armazenamento estiver bloqueado, o painel informa que não conseguiu salvar.

Use Tab para alcançar **Pular para o conteúdo**, o painel e seus controles.
Escape e o botão de fechar retornam o foco ao botão de abertura. Clicar fora
também fecha, preservando o foco da ação escolhida. O painel não prende
o foco. O zoom do navegador permanece disponível. Fotos não são invertidas.
O visual original é mantido com as opções no padrão, além do botão flutuante
e do indicador de foco ao navegar pelo teclado.

## Arquivos

- `frontend/src/components/Acessibilidade.jsx`: painel compartilhado, teclado e foco.
- `frontend/src/services/acessibilidade.js`: preferências e escala das fontes.
- `frontend/src/styles/acessibilidade.css`: painel, contraste e refluxo do texto.
- `frontend/src/main.jsx`: inclui o componente uma vez para todas as rotas.
- `frontend/tests/e2e/acessibilidade.spec.js`: verificações de interface no Chrome.

As telas existentes combinam fontes em px, rem e clamp. O serviço envolve as
declarações locais de tamanho em uma escala CSS, preservando prioridades e media
queries. Tamanhos relativos em em e porcentagem continuam herdando a escala.
Folhas externas não são modificadas. Os ajustes de refluxo só atuam com ampliação.

## Validação e limites

Execute em `frontend`: `npm run lint`, `npm run build`, `npm test` e
`npm run test:e2e -- tests/e2e/acessibilidade.spec.js`.

Os treze cenários de navegador cobrem login, comunidade, dashboard de educadora e
administração em 1280 e 390 pixels: padrão, contraste, ampliação até 200%,
combinação, restauração, persistência, teclado, Escape, foco e salto ao conteúdo.
O popover é conferido aberto e fechado também em 320px, incluindo fechamento ao
clicar fora, foco pelo X e persistência sem abertura automática.
Conferem fontes calculadas, ausência de transbordamento horizontal da página e
cortes horizontais nos principais textos. Capturas ficam em `frontend/test-results`
(geradas e ignoradas pelo Git).

As APIs desses testes são interceptadas com fixtures; o login verificado é o fluxo
do frontend, sem autenticar contas reais nem escrever no banco. Não substituem
testes de autorização ou integração do backend. Nenhum backend, migração ou regra
de negócio foi alterado.

Tabelas largas mantêm sua rolagem horizontal local. Campos de uma linha continuam
rolando o conteúdo digitado; texto dentro de imagens, PDFs e conteúdo externo não
é ampliado por estes controles. A verificação foi feita no Chrome, sem auditoria
completa de leitor de tela ou outros navegadores. Estes recursos não representam
certificação de acessibilidade nem conformidade integral com WCAG.

O ambiente de uso continua em http://127.0.0.1:5178. Os comandos operacionais
existentes estão em [Ambiente local](AMBIENTE_LOCAL.md).
