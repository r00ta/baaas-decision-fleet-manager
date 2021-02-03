create table DECISION
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    customer_id        varchar(255) NOT NULL,
    name               varchar(255) NOT NULL,
    description        text,
    lock_version       integer      NOT NULL default 0,
    current_version_id varchar(255),
    next_version_id    varchar(255),
    unique (customer_id, name)
);

create table DECISION_VERSION
(
    id                 varchar(255) NOT NULL PRIMARY KEY,
    version            integer      NOT NULL,
    submitted_at       timestamp    NOT NULL,
    published_at       timestamp,
    dmn_location       varchar(255) NOT NULL,
    dmn_md5            varchar(255) NOT NULL,
    kafka_source_topic varchar(255),
    kafka_sink_topic   varchar(255),
    status             varchar(255) NOT NULL,
    status_message     text,
    url                varchar(255),
    decision_id        varchar(255),
    lock_version       integer      NOT NULL DEFAULT 0,
    unique (version, decision_id),
    constraint fk_decision foreign key (decision_id) references DECISION (id)
);

create table DECISION_VERSION_TAG
(
    name                varchar(255) not null,
    value               varchar(255) not null,
    decision_version_id varchar(255) not null,
    constraint fk_decision_version_tag foreign key (decision_version_id) references DECISION_VERSION (id)
);

create table DECISION_VERSION_CONFIG
(
    name                varchar(255) not null,
    value               varchar(255) not null,
    decision_version_id varchar(255) not null,
    constraint fk_decision_version_config foreign key (decision_version_id) references DECISION_VERSION (id)
);


alter table DECISION
    add constraint fk_current_version
        foreign key (current_version_id)
            references DECISION_VERSION (id);

alter table DECISION
    add constraint fk_next_version
        foreign key (next_version_id)
            references DECISION_VERSION (id);
