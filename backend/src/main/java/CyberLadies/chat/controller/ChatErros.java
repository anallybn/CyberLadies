package CyberLadies.chat.controller;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(assignableTypes=ChatController.class)
public class ChatErros {
 @ExceptionHandler(DataAccessException.class)
 public ResponseEntity<ProblemDetail> conflito(){
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Os dados do contexto mudaram ou a operação entrou em conflito. Atualize e tente novamente.");
  return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(CacheControl.noStore()).body(p);
 }
}
