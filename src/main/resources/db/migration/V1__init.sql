
    create table agendamento (
        criado_em timestamp(6) with time zone not null,
        data_hora timestamp(6) not null,
        barbeiro_id uuid not null,
        cliente_id uuid not null,
        id uuid not null,
        remarcado_para_id uuid unique,
        servico_id uuid not null,
        status varchar(255) not null check ((status in ('CONFIRMADO','CONCLUIDO','CANCELADO','NO_SHOW','REMARCADO'))),
        primary key (id)
    );

    create table barbeiro (
        criado_em timestamp(6) with time zone not null,
        id uuid not null,
        usuario_id uuid not null unique,
        horario_funcionamento varchar(255),
        primary key (id)
    );

    create table cliente (
        criado_em timestamp(6) with time zone not null,
        barbeiro_id uuid not null,
        id uuid not null,
        usuario_id uuid unique,
        nome varchar(255) not null,
        telefone varchar(255) not null,
        primary key (id),
        constraint uk_cliente_barbeiro_telefone unique (barbeiro_id, telefone)
    );

    create table movimento_financeiro (
        custo_insumo numeric(38,2) not null,
        data date not null,
        lucro_liquido numeric(10,2) not null,
        valor_bruto numeric(38,2) not null,
        agendamento_id uuid not null unique,
        id uuid not null,
        primary key (id)
    );

    create table servico (
        ativo boolean not null,
        custo_insumo numeric(38,2) not null,
        duracao_minutos integer not null,
        preco numeric(38,2) not null,
        barbeiro_id uuid not null,
        id uuid not null,
        nome varchar(255) not null,
        primary key (id)
    );

    create table usuario (
        criado_em timestamp(6) with time zone not null,
        id uuid not null,
        email varchar(255) not null unique,
        nome varchar(255) not null,
        role varchar(255) not null check ((role in ('BARBEIRO','CLIENTE'))),
        senha varchar(255) not null,
        primary key (id)
    );

    alter table if exists agendamento
       add constraint FKqxtgfgl3b80ib97ygbpq796yw
       foreign key (barbeiro_id)
       references barbeiro;

    alter table if exists agendamento
       add constraint FKsgdo4l8yts964f089m6ujyuef
       foreign key (cliente_id)
       references cliente;

    alter table if exists agendamento
       add constraint FKjnd2rfguvcx2c0qw8a8phd7r7
       foreign key (remarcado_para_id)
       references agendamento;

    alter table if exists agendamento
       add constraint FK917hu1kyw4thfpcdiwvy2t2ui
       foreign key (servico_id)
       references servico;

    alter table if exists barbeiro
       add constraint FKdlxkvxpw1sb35hh80mhyxa3f0
       foreign key (usuario_id)
       references usuario;

    alter table if exists cliente
       add constraint FKn184iyrg7cx8u21iyhh5w2616
       foreign key (barbeiro_id)
       references barbeiro;

    alter table if exists cliente
       add constraint FKc3u631ocxdrtm3ccpme0kjlmu
       foreign key (usuario_id)
       references usuario;

    alter table if exists movimento_financeiro
       add constraint FK1h4xm3rcrus31niani6e19iwa
       foreign key (agendamento_id)
       references agendamento;

    alter table if exists servico
       add constraint FKj7hn5g546yusuljswufk5il5
       foreign key (barbeiro_id)
       references barbeiro;
