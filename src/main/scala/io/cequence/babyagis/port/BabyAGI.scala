package io.cequence.babyagis.port

import akka.actor.ActorSystem
import akka.stream.Materializer
import io.cequence.openaiscala.OpenAIScalaClientException
import io.cequence.openaiscala.domain.{ChatRole, MessageSpec}
import io.cequence.openaiscala.domain.settings.{CreateChatCompletionSettings, CreateCompletionSettings}
import io.cequence.openaiscala.service.OpenAIServiceFactory

import scala.concurrent.duration.DurationInt
import scala.util.Properties
import scala.concurrent.{Await, ExecutionContext, Future}

object BabyAGI {

  protected implicit val ec: ExecutionContext = ExecutionContext.global

  private val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: Materializer = Materializer(actorSystem)

  // Engine configuration

  // Model: GPT, LLAMA (not supported), HUMAN, etc
  val LLM_MODEL = Properties.envOrElse("LLM_MODEL",
    Properties.envOrElse("OPENAI_API_MODEL", "gpt-3.5-turbo")
  ).toLowerCase

  // API Keys
  // also additional OPENAI_API_ORG_ID is supported here
  val OPENAI_API_KEY = Properties.envOrNone("OPENAI_API_KEY")

  val openAIService = if (!(LLM_MODEL.startsWith("llama") || LLM_MODEL.startsWith("human")) || OPENAI_API_KEY.isDefined) {
    val OPENAI_API_ORG_ID = Properties.envOrNone("OPENAI_API_ORG_ID")
    assert(OPENAI_API_KEY.isDefined, "\033[91m\033[1m" + "OPENAI_API_KEY environment variable is missing" + "\033[0m\033[0m")

    Some(OpenAIServiceFactory(OPENAI_API_KEY.get, OPENAI_API_ORG_ID))
  } else {
    None
  }

  // Table config
  val RESULTS_STORE_NAME = Properties.envOrSome("RESULTS_STORE_NAME", Properties.envOrNone("TABLE_NAME"))
  assert(RESULTS_STORE_NAME.isDefined, "\033[91m\033[1m" + "RESULTS_STORE_NAME environment variable is missing" + "\033[0m\033[0m")

  // Run configuration
  val INSTANCE_NAME = Properties.envOrElse("INSTANCE_NAME", Properties.envOrElse("BABY_NAME", "BabyAGI"))
  val COOPERATIVE_MODE = "none"
  val JOIN_EXISTING_OBJECTIVE = false

  // Goal configuration
  val OBJECTIVE = Properties.envOrElse("OBJECTIVE", "")
  val INITIAL_TASK = Properties.envOrElse("INITIAL_TASK", Properties.envOrElse("FIRST_TASK", ""))

  // Model configuration
  val OPENAI_TEMPERATURE = Properties.envOrElse("OPENAI_TEMPERATURE", "0.0").toDouble

  // Extensions support - skipped

  val MODE = if (Set("n", "none").contains(COOPERATIVE_MODE)) "alone"
  else if (Set("l", "local").contains(COOPERATIVE_MODE)) "local"
  else if (Set("d", "distributed").contains(COOPERATIVE_MODE)) "distributed"
  else "undefined"

  println("\033[95m\033[1m" + "\n*****CONFIGURATION*****\n" + "\033[0m\033[0m")
  println(s"Name  : ${INSTANCE_NAME}")
  println(s"Mode  : ${MODE}")
  println(s"LLM   : ${LLM_MODEL}")

  // Check if we know what we are doing
  assert(OBJECTIVE.nonEmpty, "\033[91m\033[1m" + "OBJECTIVE environment variable is missing from .env" + "\033[0m\033[0m")
  assert(INITIAL_TASK.nonEmpty, "\033[91m\033[1m" + "INITIAL_TASK environment variable is missing from .env" + "\033[0m\033[0m")

  if (LLM_MODEL.startsWith("llama"))
    throw new IllegalArgumentException("Llama not supported yet")

  if (LLM_MODEL.startsWith("gpt-4"))
    println(
      "\033[91m\033[1m"
        + "\n*****USING GPT-4. POTENTIALLY EXPENSIVE. MONITOR YOUR COSTS*****"
        + "\033[0m\033[0m"
    )

  if (LLM_MODEL.startsWith("human"))
    println(
      "\033[91m\033[1m"
        + "\n*****USING HUMAN INPUT*****"
        + "\033[0m\033[0m"
    )


  println("\033[94m\033[1m" + "\n*****OBJECTIVE*****\n" + "\033[0m\033[0m")
  println(OBJECTIVE)

  if (!JOIN_EXISTING_OBJECTIVE)
    println(s"\033[93m\033[1m" + "\nInitial task:" + "\033[0m\033[0m" + f" ${INITIAL_TASK}")
  else
    println("\033[93m\033[1m" + f"\nJoining to help the objective" + "\033[0m\033[0m")

  // Initialize results storage
  val PINECONE_API_KEY = Properties.envOrElse("PINECONE_API_KEY", "")

  val results_storage = if (PINECONE_API_KEY.nonEmpty) {
    val PINECONE_ENVIRONMENT = Properties.envOrElse("PINECONE_ENVIRONMENT", "")
    assert(PINECONE_ENVIRONMENT.nonEmpty, "\033[91m\033[1m" + "PINECONE_ENVIRONMENT environment variable is missing" + "\033[0m\033[0m")

    println("\nReplacing results storage: " + "\033[93m\033[1m" + "Pinecone" + "\033[0m\033[0m")

    new PineconeResultsStorage(
      PINECONE_API_KEY,
      PINECONE_ENVIRONMENT,
      openAIService.getOrElse(
        throw new IllegalArgumentException("Pinecone expects OpenAI API to retrieve embeddings.")
      ),
      LLM_MODEL,
      RESULTS_STORE_NAME.get,
      OBJECTIVE
    )
  } else {
    throw new IllegalArgumentException("Only Pinecone storage is supported. Requires PINECONE_API_KEY and PINECONE_ENVIRONMENT environment variables.")
  }

  // Initialize tasks storage
  val tasks_storage = new SingleTaskListStorage()

  private def openai_call(
    prompt: String,
    model: String = LLM_MODEL,
    temperature: Double = OPENAI_TEMPERATURE,
    max_tokens: Int = 1000 // originally 100 (too small)
  ): Future[String] = {
    println("prompt")
    println(prompt)

    retryOnOpenAIException(
      failureMessage = "OpenAI API error occurred.",
      log = println(_),
      maxAttemptNum = Int.MaxValue, // loop forever if failing
      sleepOnFailureMs = 10000 // wait 10 seconds and try again
    )(
      if (model.toLowerCase().startsWith("llama")) {
        throw new IllegalArgumentException("Llama not supported yet")
      } else if (model.toLowerCase().startsWith("human")) {
        Future(user_input_await(prompt))
      } else if (!model.toLowerCase.startsWith("gpt-")) {
        // Use completion API
        openAIService.get.createCompletion(
          prompt = prompt,
          settings = CreateCompletionSettings(
            model = model,
            temperature = Some(temperature),
            max_tokens = Some(max_tokens),
            top_p = Some(1),
            frequency_penalty = Some(0),
            presence_penalty = Some(0)
          )
        ).map { response =>
          response.choices.head.text.strip()
        }
      } else {
        // Use chat completion API

        // TODO
        // trimmed_prompt = limit_tokens_from_string(prompt, model, 4000 - max_tokens)

        val messages = Seq(
          MessageSpec(ChatRole.System, prompt)
        )

        openAIService.get.createChatCompletion(
          messages = messages,
          settings = CreateChatCompletionSettings(
            model = model,
            temperature = Some(temperature),
            max_tokens = Some(max_tokens),
            n = Some(1),
            stop = Nil
          )
        ).map { response =>
          response.choices.head.message.content.strip()
        }
      }
    )
  }

  private def retryOnOpenAIException[T](
    failureMessage: String,
    log: String => Unit,
    maxAttemptNum: Int,
    sleepOnFailureMs: Int)(
    f: => Future[T])(
    implicit ec: ExecutionContext
  ): Future[T] = {
    def retryAux(attempt: Int): Future[T] =
      f.recoverWith {
        case e: OpenAIScalaClientException =>
          if (attempt < maxAttemptNum) {
            val errorMessage = e.getMessage.split("\n").find(_.contains("message")).map(
              _.trim.stripPrefix("\"message\": \"").stripSuffix("\",")
            ).getOrElse("")

            log(s"${failureMessage} ${errorMessage}. Attempt ${attempt}. Waiting ${sleepOnFailureMs / 1000} seconds")
            Thread.sleep(sleepOnFailureMs)
            retryAux(attempt + 1)
          } else
            throw e
      }

    retryAux(1)
  }

  private def task_creation_agent(
    objective: String,
    result: Map[String, String],
    task_description: String,
    task_list: Seq[String]
  ): Future[Seq[Map[String, String]]] = {
    // note: only "data" attribute is present in the result

    var prompt_new =
      s"""
        |You are to use the result from an execution agent to create new tasks with the following objective: ${objective}.
        |The last completed task has the result: \n${result("data")}
        |This result was based on this task description: ${task_description}.\n""".stripMargin

    if (task_list.nonEmpty)
      prompt_new += f"These are incomplete tasks: ${task_list.mkString(", ")}\n"

    prompt_new += "Based on the result, create a list of new tasks to be completed in order to meet the objective."

    if (task_list.nonEmpty)
      prompt_new += "These new tasks must not overlap with incomplete tasks."

    prompt_new += """
        |Return all the new tasks, with one task per line in your response. The result must be a numbered list in the format:
        |
        |#. First task
        |#. Second task
        |
        |The number of each entry must be followed by a period.
        |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin

//    val prompt_old =
//      s"""
//        |You are a task creation AI that uses the result of an execution agent to create new tasks with the following objective: $objective,
//        |The last completed task has the result: {$result_items_string}.
//        |This result was based on this task description: $task_description. These are incomplete tasks: ${task_list.mkString(", ")}.
//        |Based on the result, create new tasks to be completed by the AI system that do not overlap with incomplete tasks.
//        |Return the tasks as an array.""".stripMargin

    println(s"\n************** TASK CREATION AGENT PROMPT *************\n${prompt_new}")

    openai_call(
      prompt_new,
      max_tokens = 2000
    ).map { response =>
      println(s"\n************* TASK CREATION AGENT RESPONSE ************\n${response}")

      val new_tasks = response.split("\n").flatMap { task_string =>
        val task_parts = task_string.strip().split("\\.", 2)
        if (task_parts.size == 2) {
          val task_id = task_parts(0).filter(_.isDigit)
          val task_name = task_parts(1).replaceAll("[^\\w\\s_]+", "").strip()
          if (task_name.nonEmpty && task_id.nonEmpty) {
            Some(task_name)
          } else
            None
        } else
          None
      }

      new_tasks.map(task_name => Map("task_name" -> task_name))
    }
  }

  private def prioritization_agent: Future[Unit] = {
    val task_names = tasks_storage.get_task_names
    val next_task_id = tasks_storage.next_task_id

    val prompt =
      s"""
      |You are tasked with cleaning the format and re-prioritizing the following tasks: ${task_names.mkString(", ")}.
      |Consider the ultimate objective of your team: ${OBJECTIVE}.
      |Tasks should be sorted from highest to lowest priority.
      |Higher-priority tasks are those that act as pre-requisites or are more essential for meeting the objective.
      |Do not remove any tasks. Return the result as a numbered list in the format:
      |
      |#. First task
      |#. Second task
      |
      |The entries are consecutively numbered, starting with 1. The number of each entry must be followed by a period.
      |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin

    println(s"\n************** TASK PRIORITIZATION AGENT PROMPT *************\n${prompt}")

    openai_call(prompt, max_tokens = 2000).map { response =>
      println(s"\n************* TASK PRIORITIZATION AGENT RESPONSE ************\n${response}")

      val new_tasks = if (response.contains("\n")) response.split("\n").toSeq else Seq(response)

      val new_tasks_list = new_tasks.flatMap { task_string =>
        val task_parts = task_string.strip().split("\\.", 2)
        if (task_parts.size == 2) {
          val task_id = task_parts(0).filter(_.isDigit)
          val task_name = task_parts(1).replaceAll("[^\\w\\s_]+", "").strip()
          if (task_name.nonEmpty)
            Some(Map("task_id" -> task_id, "task_name" -> task_name))
          else None
        } else
          None
      }

      tasks_storage.replace(new_tasks_list)
    }
  }

  /**
   * Executes a task based on the given objective and previous context.
   *
   * @param objective The objective or goal for the AI to perform the task.
   * @param task The task to be executed by the AI.
   * @return The response generated by the AI for the given task.
   */
  private def execution_agent(
    objective: String,
    task: String
  ): Future[String] =
    for {
      context <- context_agent(query = objective, top_results_num = 5)

      response <- {
        // println("\n*******RELEVANT CONTEXT******\n")
        // println(context)

        var prompt = s"Perform one task based on the following objective: $objective.\n"

        if (context.nonEmpty)
           prompt += s"Take into account these previously completed tasks:${context.mkString("\n")}"

        prompt += s"\nYour task: ${task}\nResponse:"

        openai_call(prompt, max_tokens = 2000)
      }
    } yield
      response

  /**
   * Retrieves context for a given query from an index of tasks.
   *
   * @param query The query or objective for retrieving context.
   * @param top_results_num The number of top results to retrieve.
   * @return A list of tasks as context for the given query, sorted by relevance.
   */
  private def context_agent(
    query: String,
    top_results_num: Int
  ):  Future[Seq[String]] =
    results_storage.query(query, top_results_num).map { results =>
      // println("***** RESULTS *****")
      // println(results)
      results
    }

  // Note: to be consistent with the original Python-based impl, we need to wrap it like this
  @Deprecated
  private def toPythonSeqString[T](seq: Seq[String]): String = {
    val itemsString = seq.map(s => s"'$s'").mkString(", ")
    s"[$itemsString]"
  }

  def user_input_await(prompt: String): String = {
    println("\033[94m\033[1m" + "\n> COPY FOLLOWING TEXT TO CHATBOT\n" + "\033[0m\033[0m")
    println(prompt)
    println("\033[91m\033[1m" + "\n AFTER PASTING, PRESS: (ENTER) TO FINISH\n" + "\033[0m\033[0m")
    println("\033[96m\033[1m" + "\n> PASTE YOUR RESPONSE:\n" + "\033[0m\033[0m")
    // TODO: add while
    val input_text = scala.io.StdIn.readLine()
    input_text.strip()
  }

  // Add the initial task if starting new objective
  if (!JOIN_EXISTING_OBJECTIVE) {
    val initial_task = Map(
      "task_id" -> tasks_storage.next_task_id.toString,
      "task_name" -> INITIAL_TASK
    )
    tasks_storage.append(initial_task)
  }

  //////////
  // MAIN //
  //////////
  def main(args: Array[String]) {
    var loop = true
    while (loop) {
      // As long as there are tasks in the storage...
      if (!tasks_storage.is_empty) {
        // Print the task list
        println("\033[95m\033[1m" + "\n*****TASK LIST*****\n" + "\033[0m\033[0m")
        for (t <- tasks_storage.get_task_names) {
          println(" • " + t)
        }

        // Step 1: Pull the first incomplete task
        val task = tasks_storage.popleft
        println("\033[92m\033[1m" + "\n*****NEXT TASK*****\n" + "\033[0m\033[0m")
        println(task("task_name"))

        val processFuture = for {
          // Send to execution function to complete the task based on the context
          result <- execution_agent(OBJECTIVE, task("task_name"))

          _ = {
            println("\033[93m\033[1m" + "\n*****TASK RESULT*****\n" + "\033[0m\033[0m")
            println(result)
          }

          // Step 2: Enrich result and store in the results storage
          // This is where you should enrich the result if needed
          enrichedResult = Map("data" -> result)

          // extract the actual result from the dictionary
          // since we don't do enrichment currently
          // vector = enrichedResult("data") // don't needed

          result_id = s"result_${task("task_id")}"

          _ <- results_storage.add(task, result, result_id)

          // Step 3: Create new tasks and re-prioritize task list
          // only the main instance in cooperative mode does that
          new_tasks <- task_creation_agent(
            OBJECTIVE,
            enrichedResult,
            task("task_name"),
            tasks_storage.get_task_names
          )

          _ = for (new_task <- new_tasks) {
            val newTaskWithID = new_task + ("task_id" -> tasks_storage.next_task_id.toString)
            println(new_task)
            tasks_storage.append(newTaskWithID)
          }

          _ <- if (!JOIN_EXISTING_OBJECTIVE) {
            prioritization_agent
          } else
            Future(())
        } yield
          ()

        Await.result(processFuture, 10 minutes)

        // Sleep a bit before checking the task list again
        Thread.sleep(5000)
      } else {
        println("Done.")
        loop = false
      }
    }
  }
}
