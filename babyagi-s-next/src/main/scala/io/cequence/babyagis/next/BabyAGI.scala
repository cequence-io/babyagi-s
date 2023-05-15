package io.cequence.babyagis.next

import io.cequence.babyagis.next.providers.{CompletionProvider, EmbeddingsProvider, VectorStoreProvider}
import io.cequence.babyagis.next.SingleTaskListStorage

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class BabyAGI(
  objective: String,
  initialTask: String,
  vectorStore: VectorStoreProvider,
  completionProvider: CompletionProvider,
  embeddingsProvider: EmbeddingsProvider)(
  implicit ec: ExecutionContext
) {
  // Initialize tasks storage
  private val tasks_storage = new SingleTaskListStorage()

  private val instanceName = "BabyAGI"
  private val mode = "local"

  private def taskCreationAgent(
    objective: String,
    result: Map[String, String], // note: only "data" attribute is present in the result
    task_description: String,
    task_list: Seq[String]
  ): Future[Seq[Map[String, Any]]] = {
    val prompt = taskCreationAgentPrompt(objective, result, task_description, task_list)

    println(s"\n************** TASK CREATION AGENT PROMPT *************\n${prompt}\n")

    completionProvider(
      prompt,
      maxTokens = 2000
    ).map { response =>
      println(s"\n************* TASK CREATION AGENT RESPONSE ************\n${response}\n")

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

  protected[next] def taskCreationAgentPrompt(
    objective: String,
    result: Map[String, String], // note: only "data" attribute is present in the result
    task_description: String,
    task_list: Seq[String]
  ): String = {
    var prompt =
      s"""
         |You are to use the result from an execution agent to create new tasks with the following objective: ${objective}.
         |The last completed task has the result: \n${result("data")}
         |This result was based on this task description: ${task_description}.\n""".stripMargin

    if (task_list.nonEmpty)
      prompt += f"These are incomplete tasks: ${task_list.mkString(", ")}\n"

    prompt += "Based on the result, create a list of new tasks to be completed in order to meet the objective. "

    if (task_list.nonEmpty)
      prompt += "These new tasks must not overlap with incomplete tasks. "

    prompt +=
      """
        |Return all the new tasks, with one task per line in your response. The result must be a numbered list in the format:
        |
        |#. First task
        |#. Second task
        |
        |The number of each entry must be followed by a period.
        |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin

    prompt
  }

  private def prioritizationAgent: Future[Unit] = {
    val task_names = tasks_storage.get_task_names

    val prompt = prioritizationAgentPrompt(task_names, objective)

    println(s"\n************** TASK PRIORITIZATION AGENT PROMPT *************\n${prompt}\n")

    completionProvider(prompt, maxTokens = 2000).map { response =>
      println(s"\n************* TASK PRIORITIZATION AGENT RESPONSE ************\n${response}\n")

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

  protected[next] def prioritizationAgentPrompt(
    task_names: Seq[String],
    objective: String
  ): String =
    s"""
       |You are tasked with cleaning the format and re-prioritizing the following tasks: ${task_names.mkString(", ")}.
       |Consider the ultimate objective of your team: ${objective}.
       |Tasks should be sorted from highest to lowest priority.
       |Higher-priority tasks are those that act as pre-requisites or are more essential for meeting the objective.
       |Do not remove any tasks. Return the result as a numbered list in the format:
       |
       |#. First task
       |#. Second task
       |
       |The entries are consecutively numbered, starting with 1. The number of each entry must be followed by a period.
       |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin

  /**
   * Executes a task based on the given objective and previous context.
   *
   * @param objective The objective or goal for the AI to perform the task.
   * @param task The task to be executed by the AI.
   * @return The response generated by the AI for the given task.
   */
  private def executionAgent(
    objective: String,
    task: String
  ): Future[String] =
    for {
      context <- contextAgent(query = objective, topResultsNum = 5)

      response <- {
        // println("\n*******RELEVANT CONTEXT******\n")
        // println(context)

        val prompt = executionAgentPrompt(objective, task, context)

        completionProvider(prompt, maxTokens = 2000)
      }
    } yield
      response

  protected[next] def executionAgentPrompt(
    objective: String,
    task: String,
    context: Seq[String]
  ): String = {
    var prompt = s"Perform one task based on the following objective: $objective.\n"

    if (context.nonEmpty)
      prompt += s"Take into account these previously completed tasks:${context.mkString("\n")}"

    prompt += s"\nYour task: ${task}\nResponse:"

    prompt
  }

  /**
   * Retrieves context for a given query from an index of tasks.
   *
   * @param query The query or objective for retrieving context.
   * @param topResultsNum The number of top results to retrieve.
   * @return A list of tasks as context for the given query, sorted by relevance.
   */
  private def contextAgent(
    query: String,
    topResultsNum: Int
  ):  Future[Seq[String]] =
    for {
      queryEmbedding <- getEmbedding(query)

      results <- vectorStore.querySorted(
        queryEmbedding,
        topResultsNum,
        metadataFieldName = "task" // extract the "task" field
      )
    } yield
      results

  private def getEmbedding(text: String) = {
    val replacedText = text.replaceAll("\n", " ")
    embeddingsProvider(Seq(replacedText)).map(_.head)
  }

  def exec = {
    println("\033[95m\033[1m" + "\n*****CONFIGURATION*****\n" + "\033[0m\033[0m")
    println(s"Name             : ${instanceName}")
    println(s"Mode             : ${mode}")
    println(s"Completion Model : ${completionProvider.modelName}")
    println(s"Embeddings Model : ${embeddingsProvider.modelName}")

    if (completionProvider.modelName.startsWith("OpenAI: gpt-4"))
      println(
        "\033[91m\033[1m"
          + "\n*****USING GPT-4. POTENTIALLY EXPENSIVE. MONITOR YOUR COSTS*****"
          + "\033[0m\033[0m"
      )

    if (completionProvider.modelName.startsWith("human"))
      println(
        "\033[91m\033[1m"
          + "\n*****USING HUMAN INPUT*****"
          + "\033[0m\033[0m"
      )


    println("\033[94m\033[1m" + "\n*****OBJECTIVE*****\n" + "\033[0m\033[0m")
    println(objective)

    println(s"\033[93m\033[1m" + "\nInitial task:" + "\033[0m\033[0m" + f" ${initialTask}")

    val initial_task = Map(
      "task_id" -> tasks_storage.next_task_id.toString,
      "task_name" -> initialTask
    )

    tasks_storage.append(initial_task)

    var loop = true
    var iteration_id = 0
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
          result <- executionAgent(objective, task("task_name").toString)

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

          // result_id = s"result_${task("task_id")}"
          resultId = {
            iteration_id += 1
            s"result_${iteration_id}"
          }

          queryEmbedding <- getEmbedding(result)

          _ <- vectorStore.add(
            resultId,
            queryEmbedding,
            metadata = Map(
              "task" -> task("task_name").toString,
              "result" -> result
            )
          )

          // Step 3: Create new tasks and re-prioritize task list
          // only the main instance in cooperative mode does that
          new_tasks <- taskCreationAgent(
            objective,
            enrichedResult,
            task("task_name").toString,
            tasks_storage.get_task_names
          )

          _ = {
            println("Adding new tasks to task_storage")
            for (new_task <- new_tasks) {
              val newTaskWithID = new_task + ("task_id" -> tasks_storage.next_task_id)
              println(newTaskWithID.toString.stripPrefix("Map"))
              tasks_storage.append(newTaskWithID)
            }
          }

          _ <- prioritizationAgent
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