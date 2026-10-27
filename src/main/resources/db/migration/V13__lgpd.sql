alter table usuario
    add column termos_aceitos_em timestamptz,
    add column termos_versao     varchar(20);

alter table barbeiro
    drop constraint fkdlxkvxpw1sb35hh80mhyxa3f0,
    add constraint fk_barbeiro_usuario
        foreign key (usuario_id) references usuario (id) on delete cascade;

alter table cliente
    drop constraint fkn184iyrg7cx8u21iyhh5w2616,
    drop constraint fkc3u631ocxdrtm3ccpme0kjlmu,
    drop constraint fk_cliente_usuario_pendente,
    add constraint fk_cliente_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade,
    add constraint fk_cliente_usuario
        foreign key (usuario_id) references usuario (id) on delete set null,
    add constraint fk_cliente_usuario_pendente
        foreign key (usuario_pendente_id) references usuario (id) on delete set null;

alter table servico
    drop constraint fkj7hn5g546yusuljswufk5il5,
    add constraint fk_servico_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade;

alter table agendamento
    drop constraint fkqxtgfgl3b80ib97ygbpq796yw,
    drop constraint fksgdo4l8yts964f089m6ujyuef,
    drop constraint fkjnd2rfguvcx2c0qw8a8phd7r7,
    drop constraint fk917hu1kyw4thfpcdiwvy2t2ui,
    add constraint fk_agendamento_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade,
    add constraint fk_agendamento_cliente
        foreign key (cliente_id) references cliente (id) on delete cascade,
    add constraint fk_agendamento_remarcado_para
        foreign key (remarcado_para_id) references agendamento (id) on delete set null,
    add constraint fk_agendamento_servico
        foreign key (servico_id) references servico (id) on delete cascade;

alter table movimento_financeiro
    drop constraint fk1h4xm3rcrus31niani6e19iwa,
    add constraint fk_movimento_agendamento
        foreign key (agendamento_id) references agendamento (id) on delete cascade;

alter table assinatura
    drop constraint fk_assinatura_barbeiro,
    add constraint fk_assinatura_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade;

alter table barbeiro_horario
    drop constraint fk_barbeiro_horario_barbeiro,
    add constraint fk_barbeiro_horario_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade;

alter table bloqueio_horario
    drop constraint fk_bloqueio_horario_barbeiro,
    add constraint fk_bloqueio_horario_barbeiro
        foreign key (barbeiro_id) references barbeiro (id) on delete cascade;

alter table token_redefinicao_senha
    drop constraint fk_token_redefinicao_senha_usuario,
    add constraint fk_token_redefinicao_senha_usuario
        foreign key (usuario_id) references usuario (id) on delete cascade;
