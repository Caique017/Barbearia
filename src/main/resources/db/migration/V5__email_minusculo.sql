update usuario u
set email = lower(trim(u.email))
where u.email <> lower(trim(u.email))
  and not exists (
      select 1
      from usuario outro
      where outro.email = lower(trim(u.email))
        and outro.id <> u.id
  );
