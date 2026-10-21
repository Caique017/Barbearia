create extension if not exists btree_gist;

create table barbeiro_horario
(
    id          uuid,
    barbeiro_id uuid     not null,
    dia_semana  smallint not null,
    abre        time     not null,
    fecha       time     not null,
    primary key (id),
    constraint fk_barbeiro_horario_barbeiro
        foreign key (barbeiro_id)
            references barbeiro (id),
    constraint ck_barbeiro_horario_dia check (dia_semana between 1 and 7),
    constraint ck_barbeiro_horario_faixa check (abre < fecha)
);

create index idx_barbeiro_horario_barbeiro
    on barbeiro_horario (barbeiro_id);

alter table agendamento
    add column termina_em   timestamp(6),
    add column preco        numeric(10, 2),
    add column custo_insumo numeric(10, 2);

update agendamento a
set termina_em   = a.data_hora + make_interval(mins => s.duracao_minutos),
    preco        = s.preco,
    custo_insumo = s.custo_insumo
from servico s
where s.id = a.servico_id;

alter table agendamento
    alter column termina_em set not null,
    alter column preco set not null,
    alter column custo_insumo set not null,
    add constraint ck_agendamento_periodo check (data_hora < termina_em),
    add constraint ex_agendamento_horario_ocupado
        exclude using gist (barbeiro_id with =, tsrange(data_hora, termina_em) with &&)
        where (status in ('CONFIRMADO', 'CONCLUIDO'));

create index idx_agendamento_barbeiro_data
    on agendamento (barbeiro_id, data_hora);

create index idx_agendamento_cliente
    on agendamento (cliente_id);
