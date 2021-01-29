create table CLUSTER_CONTROL_PLANE (
    id INT NOT NULL PRIMARY KEY,
    kubernetes_api_url VARCHAR(255) NOT NULL,
    dmn_jit_url VARCHAR(255) NOT NULL
);