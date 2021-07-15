# DaaS Fleet Manager

This repository stores the code for the DaaS Fleet Manager. The Fleet Manager exposes the API
for DaaS, allow a user to create Decisions from DMN.

# Running the Fleet Manager Locally

The Fleet Manager can be run locally for integration testing with a combination of `quarkus:dev` mode and some local
dependencies managed by Docker and MiniKube.

## Pre-requisites

Local development is supported on Unix or Mac environments. Windows is not supported.

You will need the following installed:

* Docker
* Docker-Compose
* MiniKube  
* Maven

The latest versions of these dependencies is fine.

### MiniKube Configuration

It is recommended to give Minikube a little more resource than the default. You can do this with the following
commands on your machine:

```shell
minikube config set cpu <num_cpus>
minikube config set memory <memory_in_mb>
```

## Install Fleet Shard CRDs and Namespace into MiniKube

Ensure that you have minikube running with `minikube start`. This may take a few minutes if this is your first install.

The Fleet Manager will be configured with a single Fleet Shard which will be represented by the Minikube install on your machine.

We need to install the CRDs for the Fleet Shard and create the namespace. To do that, use the following:

```shell
kubectl apply -f dev/integration-resources.yml
```

To verify, use `kubectl get namespace baaas-fleetshard`

## Create a Proxy to MiniKube Kubernetes API

_In a new terminal_, create a proxy to the Kubernetes API for your running Minikube install. In the terminal issue the following
command

```shell
kubectl proxy --port=8443
```

The port is important as the `application.properties` for the `dev` profile are configured to expect a Kubernetes API
at this location on your local machine.

To verify that the proxy is working as expected, using the following `curl http://localhost:8443/apis`. This should list you all
of the known Kinds within your MiniKube cluster.

## Create Supporting Resources with Docker-Compose

The Fleet Manager has a dependency on the following other resources:

* Postgres Database for state storage
* Amazon AWS S3 for DMN storage
* Amazon AWS Secrets Manager  
* DMN JIT instance (exposed by the Fleet Shard)
* Mock API for the Kafka Managed Services

We provide local versions of all these resources using Docker.

_In another terminal_, use the following command from the root of the Git repository:

```shell
docker-compose -f dev/docker-compose.yml up
```

This should boot all dependencies. Watch the logs to ensure everything boots OK.

To verify, once the boot sequence has finished, a `docker ps` should show you something similar to this:

```shell
rblake-mac:baaas-decision-fleet-manager robpblake$ docker ps
CONTAINER ID   IMAGE                                                         COMMAND                  CREATED          STATUS          PORTS                                             NAMES
60a5e4d11f4c   localstack/localstack:0.11.5                                  "docker-entrypoint.sh"   14 minutes ago   Up 14 minutes   4567-4597/tcp, 0.0.0.0:4566->4566/tcp, 8080/tcp   baaas-decision-fleet-manager_s3_1
c5b920da1d64   quay.io/kiegroup/kogito-jit-runner-nightly:1.2.x-2021-02-03   "/home/kogito/kogito…"   14 minutes ago   Up 14 minutes   0.0.0.0:9000->8080/tcp                            baaas-decision-fleet-manager_jit_1
8d736a5b1d91   postgres:13.1                                                 "docker-entrypoint.s…"   14 minutes ago   Up 14 minutes   0.0.0.0:5432->5432/tcp                            baaas-decision-fleet-manager_db_1
```

## Compile the Fleet Manager and launch quarkus:dev

### Compile and build

_In another terminal_, use the following to first compile the Fleet Manager and then run it in `quarkus:dev` mode

```shell
mvn clean install
cd baaas-decision-fleet-manager
mvn quarkus:dev
```

The Fleet Manager will now boot on your local machine in `quarkus:dev` mode. Once the boot sequence has finished, invoke the API
using the following to ensure it is working as expected:

```shell
curl http://localhost:8080/decisions/jit
```

You should get back the following response:

```json
{
  "kind":"DMNJITList",
  "items":[
    {
      "kind":"DMNJIT",
      "url":"http://localhost:9000/jitdmn"
    }
  ]
}
```

At this point, you are ready to start using the Fleet Manager API.

## Invoking the API

It is highly recommended to use a tool like [Postman](https://postman.com) to work with the API.

Please refer to the [API Schema](https://gitlab.cee.redhat.com/baaas/baaas-service-api-schema) to see the supported endpoints.

### Creating a Decision Version

`POST` the example payload in [dev/example-requests/create-decision.json](dev/example-requests/create-decision.json) to `http://localhost:8080/decisions`.

You may want to change the `name`, `description` and `model` fields to suit your needs.

This _should_ create a version of your Decision that you can now retrieve from the Fleet Manager API. 

### Mocking the Fleet Shard Callback

An instance of a `DecisionRequest` CR for your new Decision Version will be created in the namespace for the
Fleet Shard on Minikube. However, we do not deploy the Fleet Shard for local dev of the Fleet Manager, so nothing
will operate on that CR instance.

Instead, we can mock the callback from the Fleet Shard to set the outcome of the deploy operation to either
`CURRENT` or `FAILED`.

The Fleet Manager expects to be called back at a specific endpoint by the Fleet Shard for each Decision Version. The URL
for callback is `/callback/decisions/{id}/versions/{version}`. So to set the status for a decision with id `foo` at version `1`,
`POST` a callback to `http://localhost:8080/callback/decisions/foo/versions/1`

You can set the status of the deployment to either `CURRENT` or `FAILED` depending on what you want to test. See the
[dev/example-requests/create-callback-from-fleet-shard.json](dev/example-requests/create-callback-from-fleet-shard.json) for the
example payload. 

Be sure to update the `decision`, `version` and `phase` fields of the callback JSON payload you send to the API.


## Cleaning up

To clean up all resources, first terminate all terminal processes.

Next, use the following:

```shell
docker-compose -f dev/docker-compose.yml down -v 
minikube stop
```

# Development Practices

Please read the general team working principles: https://docs.google.com/document/d/1dbjDDX8nAxOLSMw-UP6ufqsIko0bIZagdG_wtD814aw/edit

Specifics relating to the Fleet Manager are documented below.

## Database Development

Our Production Database is Postgres. For testing we use H2 in-memory database.

### Database Configuration Variables

The database for the Fleet Manager is configured with the following ENV variables:

* `BAAAS_DFM_DB_HOST`: hostname of the database
* `BAAAS_DFM_DB_PORT`: Port of the database (defaults to 5432)
* `BAAAS_DFM_DB_SCHEMA`: Database schema to use (defaults to baaas-dfm)
* `BAAAS_DFM_DB_USERNAME`: The username to use to connect to the database
* `BAAAS_DFM_DB_PASSWORD`: The password to use to connect to the database

### Table and Column Naming Strategy

We use the following naming strategy when creating database tables:

* Table names must be all `UPPERCASE` with words separated with `_`. Tables names should be in the singular form e.g:
  * `DECISION_FLEET_SHARD` and not `DECISION_FLEET_SHARDS`
* Column names must be all lowercase with words separated with `_`. Column names should be in the singular form e.g:
  * `kubernetes_api_url` and not `kubernetes_api_urls`

### Flyway for Migrations

We use [Flyway](https://flywaydb.org) for our database migrations.

Any modification to our database schema __MUST__ be modelled as a Flyway Migration.

Flyway migrations should be written in `SQL` and placed into [this directory](baaas-decision-fleet-manager/src/main/resources/db/migration)

Within this directory you will find sub-directories related to major development efforts. For example `0.1`. Over time we will
create additional sub-directories as new development iterations occur.

When writing your migration, place it within the correct sub-directory and use the form:

* `V<version>__<clear_description_of_what_it_does>.sql`

The major/minor part of your migration should match the directory in which it is placed. The patch part of your version should increment
depending on any existing migrations e.g:

* `0.1/V0.1.1__create_another_table.sql`
* `0.1/V0.1.2__add_another_table.sql`

### Testing Migrations

Your migrations can be tested by executing the `org.kie.baaas.dfm.app.DBMigrationTest`. This will apply them to a clean in-memory
H2 database instance.

### CI Testing

Your migrations will also be tested as part of our CI pipeline. You will not be able to merge your Merge Request if your migrations
do not apply cleanly. 

# Continuous Delivery (CD)

Decision Fleet Manager is migrating to a model of continuous delivery in that any change merged to `main` branch
will be automatically proposed as a change to our demo environment.

## Controlling the Changes that Trigger the CD Pipeline

**NOTE: This is temporarily disabled pending our migration to GitHub**

It is possible to control which resources trigger the full continuous delivery pipeline. It is not
always desirable for the pipeline to run if only certain files have been changed e.g. the change only
impacts `README.md`.

To add a file to the list that causes the CD pipeline to execute, you need to ensure it is contained
in the `changes` directive within the [.cd-allowlist.yml](.cd-allowlist.yml).

So for example, to add the file `foo.txt` to the list of files that will trigger the CD pipeline, ensure
you have the following in `.cd-allowlist.yml`:

```yaml
.cd-allowlist:
  only:
    changes:
      - pom.xml
      - foo.txt     
```
