alter table barbeiro add column slug varchar(120);

update barbeiro b
set slug = base.slug || case when base.rn > 1 then '-' || base.rn else '' end
from (
    select s.id,
           s.slug,
           row_number() over (partition by s.slug order by s.criado_em) as rn
    from (
        select b2.id,
               b2.criado_em,
               trim(both '-' from left(
                   trim(both '-' from regexp_replace(
                       translate(lower(u.nome),
                                 'áàâãäåçéèêëíìîïñóòôõöúùûüýÿ',
                                 'aaaaaaceeeeiiiinooooouuuuyy'),
                       '[^a-z0-9]+', '-', 'g')),
                   110)) as slug
        from barbeiro b2
        join usuario u on u.id = b2.usuario_id
    ) s
) base
where base.id = b.id;

alter table barbeiro alter column slug set not null;
alter table barbeiro add constraint uk_barbeiro_slug unique (slug);
