alter table cliente add column usuario_pendente_id uuid;

alter table cliente
    add constraint uk_cliente_usuario_pendente unique (usuario_pendente_id);

alter table cliente
    add constraint fk_cliente_usuario_pendente
        foreign key (usuario_pendente_id) references usuario (id);
