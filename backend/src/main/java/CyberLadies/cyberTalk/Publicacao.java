package CyberLadies.cyberTalk;

/** Modelo de domínio independente do HTTP e da projeção do feed. Persistido via JDBC. */
record Publicacao(long id, long autoraId, String texto) {
    boolean podeSerAlteradaPor(long contaId) { return autoraId == contaId; }
}
