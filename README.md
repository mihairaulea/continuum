# Continuum

Neo4j framework for querying both space and time. This is a POC right now.
The framework exposes an API to insert objects that are searchable by both life time and space. It is built on top of Spatial and TimeTree.

# Example use-case

Given a set of data about music composers, writers and painters, the framework can be used to answer questions such as:
- retrieve all painters that lived in London during 1800 - 1850
- give me the city with the most painters in the 1850s
- compute the total number of plays written during the 1920s

Things to do:

- write more tests
- write tests for querying space with other structures than the Envelope
- write benchmarks
- build example website: import Wikipedia data, create angular client
