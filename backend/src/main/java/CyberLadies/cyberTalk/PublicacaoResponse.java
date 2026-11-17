package CyberLadies.cyberTalk;

import java.time.OffsetDateTime;

record PublicacaoResponse(long id, String autora, String texto, OffsetDateTime criadoEm, boolean propria,
                          boolean curtido, long curtidas, long comentarios, String anexoNome, String anexoTipo) {}
