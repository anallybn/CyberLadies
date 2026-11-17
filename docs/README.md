# Guias do projeto

| Pasta | Responsabilidade |
| --- | --- |
| `backend/src/main/java/CyberLadies` | API organizada por funcionalidade; autenticação, educadoras, CyberTalk, Academy, perfil, vagas e Chat |
| `backend/src/main/resources/db/migration` | Migrações versionadas do Flyway, preservadas sem reescrever versões aplicadas |
| `backend/src/test` | Testes e fixtures exclusivos dos bancos automatizados |
| `frontend/src` | Páginas, componentes, estilos e clientes de API |
| `frontend/tests` | Testes unitários e cenários Playwright |
| `frontend/scripts` | Verificadores de navegador trazidos da origem; não inicializam o ambiente persistente |
| `scripts` | Comandos operacionais de iniciar, parar e administrar a conta existente |
| `docs` | Guias, contratos, validações e registros das integrações |

## Operação e revisão

- [Resultado e verificações do fechamento](FECHAMENTO.md).

- [Ambiente local persistente: iniciar e parar](AMBIENTE_LOCAL.md).
- [ADMIN: criação operacional e troca protegida de senha](ADMINISTRADOR.md).
- [Migrações, backup, adoção e recuperação](MIGRACOES.md).
- [Fluxo de análise de educadoras](ANALISE_EDUCADORAS.md).
- [Mapa de telas, integração e diferenças de contrato](INTEGRACAO_APRESENTACAO.md).
- [Validação do frontend](VALIDACAO_FRONTEND.md).
- [Recursos de acessibilidade e limites da validação](ACESSIBILIDADE.md).
- [Perfis de autoras no CyberTalk e regras de privacidade](PERFIS_CYBERTALK.md).
- [Guia do frontend](FRONTEND.md).
- [Registro histórico da recuperação dos módulos](CORRECAO_INTEGRACAO.md).

As páginas da colega e os módulos foram preservados. Os guias de funcionalidades
importados da origem continuam disponíveis nesta pasta; quando houver diferença
de autenticação ou inicialização, prevalecem os guias atuais acima. Nenhum pacote
Java foi movido nesta organização.

Arquivos gerados em `target`, `dist` e `test-results` não são fontes nem dados
persistentes. O script pontual `backend/target/organizar-docs.cjs` já foi aplicado,
foi inspecionado na retomada e não deve ser repetido ou versionado.
Dados, backups, acessos locais e configuração persistente ficam fora do
repositório, em `%LOCALAPPDATA%\CyberLadies\apresentacao`.
