create table CLUSTER_CONTROL_PLANE
(
    id                 INT          NOT NULL PRIMARY KEY,
    namespace          VARCHAR(255) NOT NULL,
    kubernetes_api_url VARCHAR(255) NOT NULL,
    dmn_jit_url        VARCHAR(255) NOT NULL
);