-- Assinatura estrutural de public. Não consulta valores das tabelas nem das sequências.
SELECT assinatura FROM (
    SELECT jsonb_build_array('relacao', c.relname, c.relkind, c.relpersistence,
        c.relrowsecurity, c.relforcerowsecurity)::text AS assinatura
    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
    UNION ALL
    SELECT jsonb_build_array('coluna', c.relname, a.attnum, a.attname,
        format_type(a.atttypid,a.atttypmod), a.attnotnull, a.attidentity, a.attgenerated,
        pg_get_expr(d.adbin,d.adrelid), co.collname)::text
    FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace
    LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum
    LEFT JOIN pg_collation co ON co.oid=a.attcollation
    WHERE n.nspname='public' AND c.relkind IN ('r','p','v','m','f') AND a.attnum>0 AND NOT a.attisdropped
    UNION ALL
    SELECT jsonb_build_array('restricao', c.relname, con.conname,
        pg_get_constraintdef(con.oid), con.convalidated)::text
    FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
    UNION ALL
    SELECT jsonb_build_array('indice', c.relname, pg_get_indexdef(i.indexrelid),
        i.indisvalid, i.indisready)::text
    FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
    UNION ALL
    SELECT jsonb_build_array('sequencia', sequencename, data_type, start_value,
        min_value, max_value, increment_by, cycle, cache_size)::text
    FROM pg_sequences WHERE schemaname='public'
    UNION ALL
    SELECT jsonb_build_array('dono_sequencia', s.relname, t.relname, a.attname, d.deptype)::text
    FROM pg_depend d JOIN pg_class s ON s.oid=d.objid AND s.relkind='S'
    JOIN pg_namespace n ON n.oid=s.relnamespace
    JOIN pg_class t ON t.oid=d.refobjid
    JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=d.refobjsubid
    WHERE n.nspname='public' AND d.classid='pg_class'::regclass AND d.refclassid='pg_class'::regclass
    UNION ALL
    SELECT jsonb_build_array('trigger', c.relname, t.tgname, pg_get_triggerdef(t.oid), t.tgenabled)::text
    FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND NOT t.tgisinternal
    UNION ALL
    SELECT jsonb_build_array('rotina', p.proname, pg_get_function_identity_arguments(p.oid))::text
    FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='public'
    UNION ALL
    SELECT jsonb_build_array('tipo', t.typname, t.typtype)::text
    FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE n.nspname='public' AND t.typrelid=0 AND t.typelem=0
    UNION ALL
    SELECT jsonb_build_array('schema', nspname)::text FROM pg_namespace
    WHERE nspname <> 'information_schema' AND nspname !~ '^pg_'
) estrutura ORDER BY assinatura COLLATE "C"
