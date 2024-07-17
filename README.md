# BabyAGI - Scala Port and Beyond
[![version](https://img.shields.io/badge/version-0.1.3-green.svg)](https://cequence.io) [![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

This project provides a line-by-line port of [BabyAGI](https://github.com/yoheinakajima/babyagi) - the first prototype of LLM-driven AI agent - in Scala to serve as a starting point for further explorations and customizations in Scala or any other JVM language. This means we haven't aimed to optimize or refactor anything besides a few parts where a direct mapping from Python to Scala code wasn't possible. To validate consistency with the original Python version we also provide two test suites for [prompts](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGIPromptSpec.scala) and [the task storage](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGITaskStorageSpec.scala).

Note that this is a port of the original Python code as of 5.5.2023, and it is not guaranteed to be in sync with the latest version of the original project in the future.

The runnable app object `BabyAGI` ([here](./babyagi-s-port/src/main/scala/io/cequence/babyagis/port/BabyAGI.scala)) follows the original Python code as closely as possible with two exceptions:
- There is no LLAMA support, hence OpenAI API is mandatory (unless you run in `human` mode)
- The only supported vector database/provider is [Pinecone](https://www.pinecone.io/) (no Chroma / Weaviate)

To cover this functionality we rely on [OpenAI](https://github.com/cequence-io/openai-scala-client) and [Pinecone](https://github.com/cequence-io/pinecone-scala) Scala clients.

**✔️ Important**: We are working now on an improved, cleaner version of BabyAGI, which means we will be functionally and architecturally diverging from the "reference point" - the original ported Scala version. We also provide [Azure form recognizer support](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence) for processing of documents by the AI agent.

## Config ⚙️

The following set of environmental variables (as in the Python version) is expected

- `OPENAI_API_KEY`
- `OPENAI_API_ORG_ID` (optional)
- `PINECONE_API_KEY`
- `PINECONE_ENVIRONMENT`
- `RESULTS_STORE_NAME` (e.g. `baby-agi-test-table`)
- `OBJECTIVE` (e.g. `Save the planet Earth from the socio-economic collapse`)
- `INITIAL_TASK` (e.g. `Develop a task list`)

## Execution 🚀

Simply run `BabyAGI` in the `port` package - [here](./babyagi-s-port/src/main/scala/io/cequence/babyagis/port/BabyAGI.scala).

## License ⚖️

This library is available and published as open source under the terms of the [MIT License](https://opensource.org/licenses/MIT).

## Contributors 🙏

This project is open-source and welcomes any contribution or feedback ([here](https://github.com/cequence-io/babyagi-s/issues)).

Development of this library has been supported by  [<img src="https://cequence.io/favicon-16x16.png"> - Cequence.io](https://cequence.io) - `The future of contracting`

Created and maintained by [Peter Banda](https://peterbanda.net).
