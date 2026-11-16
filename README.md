# BabyAGI - Scala Port and Beyond
[![version](https://img.shields.io/badge/version-2.0.0--SNAPSHOT-green.svg)](https://cequence.io) [![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

This project provides a line-by-line port of [BabyAGI](https://github.com/yoheinakajima/babyagi) - the first prototype of LLM-driven AI agent - in Scala to serve as a starting point for further explorations and customizations in Scala or any other JVM language. This means we haven't aimed to optimize or refactor anything besides a few parts where a direct mapping from Python to Scala code wasn't possible. To validate consistency with the original Python version we also provide two test suites for [prompts](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGIPromptSpec.scala) and [the task storage](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGITaskStorageSpec.scala).

Note that this is a port of the original Python code as of 5.5.2023, and it is not guaranteed to be in sync with the latest version of the original project in the future.

The runnable app object `BabyAGI` ([here](./babyagi-s-port/src/main/scala/io/cequence/babyagis/port/BabyAGI.scala)) follows the original Python code as closely as possible with two exceptions:
- There is no LLAMA support, hence OpenAI API is mandatory (unless you run in `human` mode)
- The only supported vector database/provider is [Pinecone](https://www.pinecone.io/) (no Chroma / Weaviate)

To cover this functionality we rely on [OpenAI](https://github.com/cequence-io/openai-scala-client) and [Pinecone](https://github.com/cequence-io/pinecone-scala) Scala clients.

**✔️ Important**: We are working now on an improved, cleaner version of BabyAGI, which means we will be functionally and architecturally diverging from the "reference point" - the original ported Scala version. We also provide [Azure form recognizer support](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence) for processing of documents by the AI agent.

**2.0 (this branch, unreleased)** - the client modules (`azure-form-recognizer`, `mistral-client`, `cohere-client`, `jina-client` and their `-domain` modules) no longer depend on Akka: HTTP goes through a pluggable [ws-client](https://github.com/cequence-io/ws-client) engine (1.2.2), the JDK `java.net.http` engine by default, or any other ws-client engine on the classpath (e.g. `ws-client-play-akka` to stay on Play WS + Akka), or one passed to `withEngine`. Upgrading from 0.1.x: drop the implicit `Materializer` / `ActorSystem` (the factories take only an implicit `ExecutionContext`); uploads from a stream take a ws-client `AsyncStream[Bytes]` instead of an Akka `Source[ByteString, _]`; the Azure form recognizer builds its api-version-dependent URLs itself, so `AzurePlayWSClientEngine` is gone and any engine works; Cohere's default embed model is `embed-english-v3.0` (v2.0 was removed by Cohere). The 0.1.x line (Akka) stays on `master`.

## HTTP backend 🔌

The client modules bring the JDK engine (`ws-client-jdk`, no Akka), so they work out of the box. To run on another backend, pick one in any of these ways:

- **Classpath** - add an engine module, e.g. `"io.cequence" %% "ws-client-play-akka-stream" % "1.2.2"` (or `ws-client-play-pekko-stream`, `ws-client-pekko-http`, `ws-client-zio-http`, `ws-client-sttp`); the highest-priority engine on the classpath wins over the JDK default. Keep all ws-client artifacts on one version, and Akka and Pekko engines off the same classpath.
- **By id** - with several engines on the classpath, `-Dws-client.engine=play-akka-stream` (or the `ws-client.engine` config key) names one.
- **Explicitly** - build an engine and pass it to a factory's `withEngine`; one engine can be shared by several services, and a service never closes an engine it was given:

```scala
import io.cequence.wsclient.service.spi.{TransportSettings, WSClientEngineRegistry}
import scala.concurrent.ExecutionContext.Implicits.global

val engine = WSClientEngineRegistry(TransportSettings(), Some("jdk")) // or Some("play-akka-stream")
val mistral = MistralServiceFactory.withEngine(engine, apiKey = sys.env("MISTRAL_API_KEY"))
val cohere = CohereServiceFactory.withEngine(engine, apiKey = sys.env("COHERE_API_KEY"))
// ...
mistral.close(); cohere.close(); engine.close()
```

The factories without an engine discover one the same way and close it with the service.

**Note:** the JDK engine's HTTP client (like the global `ExecutionContext`) runs on daemon threads, so unlike Play WS + Akka it does not keep the JVM alive - a `main` / `App` that fires requests must wait for its futures (e.g. `Await.ready`) before it returns. The `matrix_jdk` / `matrix_akka` sbt projects (unpublished) run the demos and the engine smoke checks on each backend, e.g. `sbt "matrix_akka/runMain io.cequence.matrix.MistralEngineSmoke"`.

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
