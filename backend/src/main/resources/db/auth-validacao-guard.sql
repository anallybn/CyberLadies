-- Impede aplicar automaticamente a migração de teste no banco principal.
DO '
BEGIN
 IF current_database() !~ ''^cyberladies_auth_[a-z0-9_]+$'' THEN
  RAISE EXCEPTION ''Perfil auth-validacao exige banco isolado cyberladies_auth_*.'';
 END IF;
END ';