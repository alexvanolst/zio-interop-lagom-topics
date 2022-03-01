# zio-lagom-interop

This is a small proof of concept to generate a simple `zio-kafka` client based off a Lagom API descriptor.

This repository is unlikely to be supported in the future as the extensions listed below will happen during my work time and therefore property of my employer

## Design decisions

We've decided to go with requiring a macro call per exposed topic rather than generating a full client based of of the descriptor. This is because

## Next steps

- Also implement Http Client (can reuse play formats and construct simple sttp clients) 
- Support for more modes consuming from kafka (currently the simple)
- Cross compile to 2.12 (Scala 3 not really necessary because Lagom will not be ported to scala 3)
- Potentially write an integration test with a Kafka testcontainer
- Publish artifacts
