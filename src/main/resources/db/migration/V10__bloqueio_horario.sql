create table bloqueio_horario
(
    id          uuid,
    barbeiro_id uuid         not null,
    inicio      timestamp(6) not null,
    fim         timestamp(6) not null,
    motivo      varchar(120),
    criado_em   timestamptz  not null,
    primary key (id),
    constraint fk_bloqueio_horario_barbeiro
        foreign key (barbeiro_id)
            references barbeiro (id),
    constraint ck_bloqueio_horario_periodo check (inicio < fim)
);

create index idx_bloqueio_horario_barbeiro_inicio
    on bloqueio_horario (barbeiro_id, inicio);
