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