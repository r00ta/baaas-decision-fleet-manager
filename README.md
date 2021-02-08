# BAaaS Master Control Plane - Initial Development

# Endpoints

The supported endpoints until now are:

## /decisions

- Verb: POST

Creates a new Decision with the given DMN XML String literal as the model.
The name uniquely identifies this Decision within the customer account. If the name already exists, then the POST
will create a new Version of an existing Decision. If the name does not exist, then this will create a new Decision.
Note: Creation of a Decision is a long-running process. The user should poll/watch the API for updates to the status
of the Decision Resource. When it enters the READY state, then the user knows that their Decision Service is ready to be
consumed.

## **Status Codes**

- 201 - Request was accepted and is being processed
- 400 - Bad Request if:
  - The payload is missing required parameters
  - The model is not a valid XML document
  - We are already in the process of deploying or rolling back a Version of this Decision


The supported schema for registering a new decision is:

## **Schema**:

### JSON payload

```json
{
  "kind": "Decision",
  "name": "my-brilliant-decision",
  "description": "A human readable description of my decision",
  "model": {
    "dmn" : "plain text xml properly escaped"
  },
  "eventing" : {
    "kafka": {
      "source": "Some Kafka Endpoint",
      "sink": "Some Kafka Endpoint"
    }
  },
  "configuration": {
    "key": "value"
  },
  "tags" : {
    "key": "value"
  }
}
```

### Responses

If the request payload contains anything wrong, a bad request would be returned and the explanation will be described
as the example below:

```json
[
  "Field: 'decisionsRequest.model.dmn' -> Provided value seems not to be valid, explanation: The element type \"dmn:definitions\" must be terminated by the matching end-tag \"</dmn:definitions>\"."
]
```
## Running the Master Control Plane Locally

### Pre-requisites

Local development is supported on Unix or Mac environments. Windows is not supported.

You will need the following installed:

* Docker
* Docker-Compose
* Maven

The latest versions of these dependencies is fine.

### Running the MCP

The MCP can be run locally using either `quarkus:dev` mode or through the `docker-compose` setup in the root of this repository.

Generally, the choice as to which one you want to use will be:

- `quarkus:dev` if you're doing iterative development on the MCP
- `docker-compose` if you want to test or integrate something against the MCP

#### quarkus:dev

To use `quarkus:dev` mode with MCP, we need to setup 2 services:

- PostgreSQL
- Localstack with S3 to provide a S3 Bucket implementation

There is a docker-compose file for this purpose: [docker-compse-quarkus-dev.yml](docker-compose-quarkus-dev.yml)
To start it use the command below:

```bash
$ podman|docker-compose -f docker-compose-quarkus-dev.yml up
```

After S3 is available, configure it as the following example:

```bash
$ aws configure --profile localstack
AWS Access Key ID [None]: test-key
AWS Secret Access Key [None]: test-secret
Default region name [None]: us-east-1
Default output format [None]:
```

And create the `decisions-bucket`:

```bash
$ aws s3 mb s3://decisions-bucket --profile localstack --endpoint-url=http://localhost:8008
make_bucket: decisions-bucket
```


```shell
mvn clean install quarkus:dev
```
This will start the MCP on `http://localhost:8080` and will support live-reload of the code as you develop.

And when you're done developing with dev mode:

```bash
$ podman|docker-compose -f docker-compose-quarkus-dev.yml down
```


#### docker-compose

You can run the MCP using docker-compose if you just want to test or develop against the API. You can start the MCP
(and the DMN JIT) using the following:

```shell
docker-compose up -d db jit
docker-compose build
docker-compose up mcp
```
This will connect you to the terminal of the MCP, so you can watch the logs to help you debug any issues you may be having.

## Database Development

Our Production Database is Postgres. For testing we use H2 in-memory database.

### Database Configuration Variables

The database for the Master Control Plane is configured with the following ENV variables:

* `BAAAS_MCP_DB_HOST`: hostname of the database
* `BAAAS_MCP_DB_PORT`: Port of the database (defaults to 5432)
* `BAAAS_MCP_DB_SCHEMA`: Database schema to use (defaults to baaas-mcp)
* `BAAAS_MCP_DB_USERNAME`: The username to use to connect to the database
* `BAAAS_MCP_DB_PASSWORD`: The password to use to connect to the database

### Table and Column Naming Strategy

We use the following naming strategy when creating database tables:

* Table names must be all `UPPERCASE` with words separated with `_`. Tables names should be in the singular form e.g:
  * `CLUSTER_CONTROL_PLANE` and not `CLUSTER_CONTROL_PLANES`
* Column names must be all lowercase with words separated with `_`. Column names should be in the singular form e.g:
  * `kubernetes_api_url` and not `kubernetes_api_urls`

### Flyway for Migrations

We use [Flyway](https://flywaydb.org) for our database migrations.

Any modification to our database schema __MUST__ be modelled as a Flyway Migration.

Flyway migrations should be written in `SQL` and placed into [this directory](baaas-master-control-plane/src/main/resources/db/migration)

Within this directory you will find sub-directories related to major development efforts. For example `0.1`. Over time we will
create additional sub-directories as new development iterations occur.

When writing your migration, place it within the correct sub-directory and use the form:

* `V<version>__<clear_description_of_what_it_does>.sql`

The major/minor part of your migration should match the directory in which it is placed. The patch part of your version should increment
depending on any existing migrations e.g:

* `0.1/V0.1.1__create_another_table.sql`
* `0.1/V0.1.2__add_another_table.sql`

### Testing Migrations

Your migrations can be tested by executing the `org.kie.baaas.mcp.app.DBMigrationTest`. This will apply them to a clean in-memory
H2 database instance.

### CI Testing

Your migrations will also be tested as part of our CI pipeline. You will not be able to merge your Merge Request if your migrations
do not apply cleanly. 