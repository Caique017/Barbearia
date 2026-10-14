alter table barbeiro add column nome_barbearia varchar(120);

update barbeiro b
set nome_barbearia = left(u.nome, 120)
from usuario u
where u.id = b.usuario_id;

alter table barbeiro alter column nome_barbearia set not null;
