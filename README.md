# BabyAGI - Scala Port and Beyond
[![version](https://img.shields.io/badge/version-2.0.0-green.svg)](https://cequence.io) [![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT)

This project provides a line-by-line port of [BabyAGI](https://github.com/yoheinakajima/babyagi) - the first prototype of LLM-driven AI agent - in Scala to serve as a starting point for further explorations and customizations in Scala or any other JVM language. This means we haven't aimed to optimize or refactor anything besides a few parts where a direct mapping from Python to Scala code wasn't possible. To validate consistency with the original Python version we also provide two test suites for [prompts](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGIPromptSpec.scala) and [the task storage](babyagi-s-port/src/test/scala/io/cequence/babyagis/port/BabyAGITaskStorageSpec.scala).

Note that this is a port of the original Python code as of 5.5.2023, and it is not guaranteed to be in sync with the latest version of the original project in the future.

The runnable app object `BabyAGI` ([here](./babyagi-s-port/src/main/scala/io/cequence/babyagis/port/BabyAGI.scala)) follows the original Python code as closely as possible with two exceptions:
- There is no LLAMA support, hence OpenAI API is mandatory (unless you run in `human` mode)
- The only supported vector database/provider is [Pinecone](https://www.pinecone.io/) (no Chroma / Weaviate)

To cover this functionality we rely on [OpenAI](https://github.com/cequence-io/openai-scala-client) and [Pinecone](https://github.com/cequence-io/pinecone-scala) Scala clients.

**✔️ Important**: We are working now on an improved, cleaner version of BabyAGI, which means we will be functionally and architecturally diverging from the "reference point" - the original ported Scala version. We also provide [Azure form recognizer support](https://learn.microsoft.com/en-us/azure/ai-services/document-intelligence) for processing of documents by the AI agent.

**2.0.0** - the client modules (`azure-form-recognizer`, `mistral-client`, `cohere-client`, `jina-client` and their `-domain` modules) no longer depend on Akka: HTTP goes through a pluggable [ws-client](https://github.com/cequence-io/ws-client) engine (1.2.2) that the caller chooses - e.g. the JDK `java.net.http` engine (`ws-client-jdk`), or `ws-client-play-akka-stream` to stay on Play WS + Akka - see [HTTP backend](#http-backend-). Upgrading from 0.1.x: drop the implicit `Materializer` / `ActorSystem` (the factories take only an implicit `ExecutionContext`); uploads from a stream take a ws-client `AsyncStream[Bytes]` instead of an Akka `Source[ByteString, _]`; the Azure form recognizer builds its api-version-dependent URLs itself, so `AzurePlayWSClientEngine` is gone and any engine works; Cohere's default embed model is `embed-english-v3.0` (v2.0 was removed by Cohere), which returns 1024-dim vectors (v2.0: 4096), and `embed` takes a required `inputType` (`search_document` for indexed texts, `search_query` for queries, `classification` or `clustering`) - `EmbedSettings.input_type` is gone; the clients no longer bring `logback-classic` or a `logback.xml` (add your own SLF4J binding), and the internal `AzureFormRecognizerEndPoint` enum is gone. The last Akka-based release is 0.1.22.

## HTTP backend 🔌

The client modules depend only on `ws-client-core` - **you choose the HTTP engine** by adding one to your dependencies (without one, creating a service fails with "No WS client engine found"):

```scala
"io.cequence" %% "ws-client-jdk" % "1.2.2" // java.net.http, no Akka, fewest dependencies
// or "io.cequence" %% "ws-client-play-akka-stream" % "1.2.2" // Play WS + Akka
```

Then pick it in any of these ways:

- **Classpath** - with a single engine module on the classpath (`ws-client-jdk`, `ws-client-play-akka-stream`, `ws-client-play-pekko-stream`, `ws-client-pekko-http`, `ws-client-zio-http`, `ws-client-sttp`, ...) it is used as is; with several, the highest priority wins. Keep all ws-client artifacts on one version, and Akka and Pekko engines off the same classpath.
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

**Note:** the JDK engine's HTTP client (like the global `ExecutionContext`) runs on daemon threads, so unlike Play WS + Akka it does not keep the JVM alive - a `main` / `App` that fires requests must wait for its futures (e.g. `Await.ready`) before it returns.

## Demos ▶️

The published client modules hold only library code - no engine, no demos, no logging config. The demos, the engine smoke checks and their `logback.xml` live in `matrix/shared` and run from one of the unpublished engine-matrix sbt projects, each of which depends on all the client modules plus exactly one engine (and logback):

| sbt project   | engine                                      | engine id          |
|---------------|---------------------------------------------|--------------------|
| `matrix_jdk`  | `ws-client-jdk` (`java.net.http`, no Akka)  | `jdk`              |
| `matrix_akka` | `ws-client-play-akka-stream` (Play WS + Akka) | `play-akka-stream` |

```sh
sbt "matrix_jdk/runMain io.cequence.cohereapi.CohereAPIDemo"
sbt "matrix_akka/runMain io.cequence.cohereapi.CohereAPIDemo"   # the same demo on Play WS + Akka
```

| demo (main class)                                | env variables                                                                                      |
|--------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `io.cequence.mistral.MistralAPIDemo`             | `MISTRAL_API_KEY`, `MISTRAL_TEST_FILE_NAME` (a PDF)                                                |
| `io.cequence.cohereapi.CohereAPIDemo`            | `COHERE_API_KEY`                                                                                   |
| `io.cequence.jinaapi.JinaAPIDemo`                | `JINA_API_KEY`, `JINA_TEST_HTML_FILE`, `JINA_TEST_PDF_FILE`                                        |
| `io.cequence.azureform.AzureFormRecognizerDemo`  | `AZURE_FORM_RECOGNIZER_ENDPOINT`, `AZURE_FORM_RECOGNIZER_API_KEY`, `AZURE_TEST_FILE_NAME`          |
| `io.cequence.matrix.MistralEngineSmoke`          | `MISTRAL_API_KEY`, `MISTRAL_TEST_FILE_NAME` (a PDF with 2+ pages), optionally `MISTRAL_OCR_MODEL` |
| `io.cequence.matrix.SharedEngineSmoke`           | `MISTRAL_API_KEY`, `COHERE_API_KEY`                                                                |

The two smoke checks exercise the clients end to end on the project's engine and fail on any mismatch: `MistralEngineSmoke` covers file and stream uploads, page groups, all OCR params and a batch job (it deletes everything it uploads); `SharedEngineSmoke` shares one engine between Mistral and Cohere as in the snippet above. Runs are forked with `-Dws-client.engine=<engine id>`, so the engine used is always the project's one - the smoke checks print it first.

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
