package CyberLadies.academy.service;

import CyberLadies.academy.dto.AcademyDados.*;
import CyberLadies.academy.repository.AcademyRepository;
import CyberLadies.auth.service.AcessoConta;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MentoriaAcademyService {
    private final AcademyRepository repository;
    private final AcessoConta acesso;
    public MentoriaAcademyService(AcademyRepository repository,AcessoConta acesso) { this.repository=repository; this.acesso=acesso; }
    public List<Mentora> mentoras() { return repository.mentoras(); }
    public List<Horario> horarios(long mentora) {
        positivo(mentora);
        if (repository.mentoras().stream().noneMatch(m->m.id()==mentora)) throw erro(HttpStatus.NOT_FOUND,"Mentora não encontrada.");
        return repository.horarios(mentora);
    }
    public List<Agendamento> meus() { return repository.agendamentos(acesso.estudante().id()); }
    @Transactional
    public Agendamento agendar(long id) {
        long usuaria=acesso.estudante().id(); positivo(id);
        var horario=repository.horario(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Horário não encontrado."));
        repository.bloquearReserva(usuaria,horario.mentoraId(),id);
        horario=repository.horario(id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Horário não encontrado."));
        validarFuturo(horario.inicio());
        if (repository.conflito(usuaria,horario)) throw erro(HttpStatus.CONFLICT,"Horário ocupado ou sobreposto a outro agendamento seu.");
        try {
            long agendamento=repository.reservar(usuaria,id);
            return repository.agendamento(usuaria,agendamento).orElseThrow();
        } catch (DataIntegrityViolationException e) {
            throw erro(HttpStatus.CONFLICT,"O horário não está mais disponível. Atualize os horários.");
        }
    }
    @Transactional
    public void cancelar(long id) {
        long usuaria=acesso.estudante().id(); positivo(id);
        var reserva=repository.agendamento(usuaria,id).orElseThrow(()->erro(HttpStatus.NOT_FOUND,"Agendamento não encontrado."));
        if (reserva.status().equals("CANCELADO")) return;
        repository.bloquearReserva(usuaria,reserva.mentoraId(),reserva.disponibilidadeId());
        reserva=repository.agendamento(usuaria,id).orElseThrow();
        if (reserva.status().equals("CANCELADO")) return;
        validarFuturo(reserva.inicio());
        try { repository.cancelar(usuaria,id); }
        catch (DataIntegrityViolationException e) { throw erro(HttpStatus.CONFLICT,"Não é possível cancelar um horário já iniciado."); }
    }
    private void validarFuturo(OffsetDateTime inicio) {
        if (!inicio.isAfter(OffsetDateTime.now())) throw erro(HttpStatus.CONFLICT,"O horário já começou ou está no passado.");
    }
    private void positivo(long id) { if(id<=0) throw erro(HttpStatus.BAD_REQUEST,"Identificador inválido."); }
    private ResponseStatusException erro(HttpStatus status,String mensagem) { return new ResponseStatusException(status,mensagem); }
}