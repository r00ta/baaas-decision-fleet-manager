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

The supported schema for registering a new decision is:

**Schemas**:

| JSON     |      Yaml     |
|----------|:-------------:|
| [json schema](#json-payload) | [yaml schema](#yaml-payload)


### JSON payload

```json
{
  "kind": "Decision",
  "name": "my-brilliant-decision",
  "description": "A human readable description of my decision",
  "model": {
    "dmn" : "<xml></xml>"
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

### Yaml payload

```yaml
kind: Decision
name: my-brilliant-decision
description: A human readable description of my decision
model:
  dmn : "<xml></xml>"
eventing : 
  kafka: 
    source: Some Kafka Endpoint
    sink: Some Kafka Endpoint
configuration: 
  key: value
tags: 
  key: value
```

## Database

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