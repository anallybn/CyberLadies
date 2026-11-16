package CyberLadies.candidatura.model;
import java.time.OffsetDateTime;
public record Candidatura(long id, long usuariaId, long vagaId, String status, OffsetDateTime criadoEm) {}