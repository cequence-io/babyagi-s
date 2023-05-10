package io.cequence.babyagis.port

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Properties

class BabyAGIPromptSpec extends AnyFlatSpec with Matchers {

  // set anything so we can test the prompts
  Properties.setProp("OPENAI_API_KEY", "x")
  Properties.setProp("PINECONE_API_KEY", "x")
  Properties.setProp("PINECONE_ENVIRONMENT", "x")
  Properties.setProp("RESULTS_STORE_NAME", "x")
  Properties.setProp("OBJECTIVE", "x")
  Properties.setProp("INITIAL_TASK", "x")

  private val space = " "

  /////////////////////////
  // TASK CREATION AGENT //
  /////////////////////////

  case class TaskCreationPromptInfo(
    objective: String,
    result: Map[String, String],
    task_description: String,
    task_list: Seq[String],
    prompt: String
  )

  val taskCreationAgentTestData = Seq(
    TaskCreationPromptInfo(
      objective = "Save the planet Earth from socio-economic collapse",
      result = Map("data" ->
        """1. Conduct research on the current state of the planet's socio-economic systems.
          |2. Identify key areas of concern and potential risks for collapse.
          |3. Develop a comprehensive plan for addressing these concerns and mitigating risks.
          |4. Engage with stakeholders from various sectors, including government, business, and civil society, to gain support and input for the plan.
          |5. Implement the plan through targeted interventions and policy changes.
          |6. Monitor progress and adjust the plan as needed.
          |7. Educate the public on the importance of sustainable practices and responsible consumption.
          |8. Foster international cooperation and collaboration to address global challenges.
          |9. Advocate for policies and practices that prioritize the long-term health of the planet and its inhabitants.
          |10. Continuously evaluate and improve efforts to ensure the sustainability of the planet's socio-economic systems.""".stripMargin),

      task_description = "Develop a task list",
      task_list = Nil,

      // note a starting new line here (should be removed)
      prompt =
        s"""
           |You are to use the result from an execution agent to create new tasks with the following objective: Save the planet Earth from socio-economic collapse.
           |The last completed task has the result:${space}
           |1. Conduct research on the current state of the planet's socio-economic systems.
           |2. Identify key areas of concern and potential risks for collapse.
           |3. Develop a comprehensive plan for addressing these concerns and mitigating risks.
           |4. Engage with stakeholders from various sectors, including government, business, and civil society, to gain support and input for the plan.
           |5. Implement the plan through targeted interventions and policy changes.
           |6. Monitor progress and adjust the plan as needed.
           |7. Educate the public on the importance of sustainable practices and responsible consumption.
           |8. Foster international cooperation and collaboration to address global challenges.
           |9. Advocate for policies and practices that prioritize the long-term health of the planet and its inhabitants.
           |10. Continuously evaluate and improve efforts to ensure the sustainability of the planet's socio-economic systems.
           |This result was based on this task description: Develop a task list.
           |Based on the result, create a list of new tasks to be completed in order to meet the objective.${space}
           |Return all the new tasks, with one task per line in your response. The result must be a numbered list in the format:
           |
           |#. First task
           |#. Second task
           |
           |The number of each entry must be followed by a period.
           |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    ),

    TaskCreationPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",
      result = Map("data" ->
        """1. Review market research study findings to identify key trends and opportunities in the contract management industry.
          |2. Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.
          |3. Review survey results to gather feedback from potential beta testers on the features and usability of the CLM software.
          |4. Prioritize identified features based on feedback and market research.
          |5. Develop a list of potential target industries for the CLM software based on market analysis.
          |6. Develop a marketing strategy for each identified target industry.
          |7. Identify potential target customers and their needs based on market analysis.
          |8. Develop a list of marketing tactics to reach potential target customers.
          |9. Determine pricing strategy for the CLM software based on market analysis and competitive research.
          |10. Develop a sales strategy and identify potential sales channels for the CLM software.""".stripMargin),

      task_description = "Develop a task list",
      task_list = Nil,

      // note a starting new line here (should be removed)
      prompt = s"""
       |You are to use the result from an execution agent to create new tasks with the following objective: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
       |The last completed task has the result:${space}
       |1. Review market research study findings to identify key trends and opportunities in the contract management industry.
       |2. Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.
       |3. Review survey results to gather feedback from potential beta testers on the features and usability of the CLM software.
       |4. Prioritize identified features based on feedback and market research.
       |5. Develop a list of potential target industries for the CLM software based on market analysis.
       |6. Develop a marketing strategy for each identified target industry.
       |7. Identify potential target customers and their needs based on market analysis.
       |8. Develop a list of marketing tactics to reach potential target customers.
       |9. Determine pricing strategy for the CLM software based on market analysis and competitive research.
       |10. Develop a sales strategy and identify potential sales channels for the CLM software.
       |This result was based on this task description: Develop a task list.
       |Based on the result, create a list of new tasks to be completed in order to meet the objective.${space}
       |Return all the new tasks, with one task per line in your response. The result must be a numbered list in the format:
       |
       |#. First task
       |#. Second task
       |
       |The number of each entry must be followed by a period.
       |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    ),

    TaskCreationPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",
      result = Map("data" ->
        """To conduct a competitive analysis, the following steps can be taken:
          |
          |1. Identify the top competitors in the CLM software market.
          |2. Gather information on each competitor's product offerings, pricing, target market, and marketing strategies.
          |3. Analyze the strengths and weaknesses of each competitor's product offerings, including features, usability, and customer support.
          |4. Compare the pricing of each competitor's product offerings to determine how the new CLM software can be competitively priced.
          |5. Identify any gaps in the market that the new CLM software can fill based on the strengths and weaknesses of the competitors.
          |6. Use the information gathered to develop a marketing strategy that highlights the unique features and benefits of the new CLM software and positions it as a strong competitor in the market.""".stripMargin),
      task_description = "Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market",
      task_list = Seq(
        "Develop a unique value proposition for the CLM software based on identified highpriority features and target industries",
        "Develop a pricing model for the CLM software that aligns with the value proposition and target industries",
        "Create a branding strategy for the CLM software including logo design and brand messaging",
        "Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry",
        "Identify key influencers in the contract management industry and develop a plan to engage with them",
        "Conduct user testing to ensure the usability and effectiveness of the CLM software",
        "Create a sales pitch and sales collateral for the CLM software",
        "Identify potential reseller partners and develop a strategy to engage with them",
        "Develop a customer support plan to ensure customer satisfaction and retention"
    ),
    prompt = s"""
       |You are to use the result from an execution agent to create new tasks with the following objective: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
       |The last completed task has the result:${space}
       |To conduct a competitive analysis, the following steps can be taken:
       |
       |1. Identify the top competitors in the CLM software market.
       |2. Gather information on each competitor's product offerings, pricing, target market, and marketing strategies.
       |3. Analyze the strengths and weaknesses of each competitor's product offerings, including features, usability, and customer support.
       |4. Compare the pricing of each competitor's product offerings to determine how the new CLM software can be competitively priced.
       |5. Identify any gaps in the market that the new CLM software can fill based on the strengths and weaknesses of the competitors.
       |6. Use the information gathered to develop a marketing strategy that highlights the unique features and benefits of the new CLM software and positions it as a strong competitor in the market.
       |This result was based on this task description: Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market.
       |These are incomplete tasks: Develop a unique value proposition for the CLM software based on identified highpriority features and target industries, Develop a pricing model for the CLM software that aligns with the value proposition and target industries, Create a branding strategy for the CLM software including logo design and brand messaging, Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry, Identify key influencers in the contract management industry and develop a plan to engage with them, Conduct user testing to ensure the usability and effectiveness of the CLM software, Create a sales pitch and sales collateral for the CLM software, Identify potential reseller partners and develop a strategy to engage with them, Develop a customer support plan to ensure customer satisfaction and retention
       |Based on the result, create a list of new tasks to be completed in order to meet the objective. These new tasks must not overlap with incomplete tasks.${space}
       |Return all the new tasks, with one task per line in your response. The result must be a numbered list in the format:
       |
       |#. First task
       |#. Second task
       |
       |The number of each entry must be followed by a period.
       |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    )
  )

  /////////////////////
  // EXECUTION AGENT //
  /////////////////////

  case class ExecutionAgentPromptInfo(
    objective: String,
    task: String,
    context: Seq[String],
    prompt: String
  )

  private val executionAgentTestData = Seq(
    ExecutionAgentPromptInfo(
      objective = "Save the planet Earth from socio-economic collapse",

      task = "Develop a task list",

      context = Nil,

      prompt =
        """Perform one task based on the following objective: Save the planet Earth from socio-economic collapse.
          |
          |Your task: Develop a task list
          |Response:""".stripMargin
    ),

    ExecutionAgentPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",

      task = "Develop a task list",

      context = Seq(
        "Conduct a market research study to gather insights on emerging trends and opportunities in the contract management industry.",
        "Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.",
        "Conduct a survey to gather feedback from potential beta testers on the features and usability of the CLM software.",
        "Conduct a market analysis to identify potential target industries for the CLM software and develop a marketing strategy for each industry.",
        "Conduct a market analysis to identify potential target customers and their needs."
      ),

      prompt = """Perform one task based on the following objective: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
        |Take into account these previously completed tasks:Conduct a market research study to gather insights on emerging trends and opportunities in the contract management industry.
        |Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.
        |Conduct a survey to gather feedback from potential beta testers on the features and usability of the CLM software.
        |Conduct a market analysis to identify potential target industries for the CLM software and develop a marketing strategy for each industry.
        |Conduct a market analysis to identify potential target customers and their needs.
        |Your task: Develop a task list
        |Response:""".stripMargin
    ),

    ExecutionAgentPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",

      task = "Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market",

      context = Seq(
        "Develop a task list",
        "Conduct a market research study to gather insights on emerging trends and opportunities in the contract management industry.",
        "Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.",
        "Conduct a survey to gather feedback from potential beta testers on the features and usability of the CLM software.",
        "Conduct a market analysis to identify potential target industries for the CLM software and develop a marketing strategy for each industry."
      ),

      prompt = """Perform one task based on the following objective: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
        |Take into account these previously completed tasks:Develop a task list
        |Conduct a market research study to gather insights on emerging trends and opportunities in the contract management industry.
        |Analyze industry research and feedback from potential beta testers to identify high-priority features for the CLM software.
        |Conduct a survey to gather feedback from potential beta testers on the features and usability of the CLM software.
        |Conduct a market analysis to identify potential target industries for the CLM software and develop a marketing strategy for each industry.
        |Your task: Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market
        |Response:""".stripMargin
    )
  )

  //////////////////////////
  // PRIORITIZATION AGENT //
  //////////////////////////

  case class PrioritizationAgentPromptInfo(
    objective: String,
    task_names: Seq[String],
    prompt: String
  )

  private val prioritizationAgentTestData = Seq(
    PrioritizationAgentPromptInfo(
      objective = "Save the planet Earth from socio-economic collapse",

      task_names = Seq(
        "Conduct a comprehensive analysis of the current state of the planets natural resources and their impact on socioeconomic systems",
        "Develop strategies for sustainable resource management and conservation",
        "Identify and address inequalities and disparities in access to resources and opportunities",
        "Implement policies and programs to promote sustainable and equitable economic growth",
        "Foster innovation and entrepreneurship in sustainable industries",
        "Develop and implement education and awareness campaigns on sustainable practices and responsible consumption",
        "Strengthen international cooperation and collaboration on sustainable development goals",
        "Advocate for policies and practices that prioritize the wellbeing of future generations",
        "Monitor and evaluate the impact of interventions and adjust strategies as needed",
        "Continuously engage with stakeholders and communities to ensure their participation and ownership in sustainable development efforts"
      ),

      // note a starting new line here (should be removed)
      prompt =
        """
          |You are tasked with cleaning the format and re-prioritizing the following tasks: Conduct a comprehensive analysis of the current state of the planets natural resources and their impact on socioeconomic systems, Develop strategies for sustainable resource management and conservation, Identify and address inequalities and disparities in access to resources and opportunities, Implement policies and programs to promote sustainable and equitable economic growth, Foster innovation and entrepreneurship in sustainable industries, Develop and implement education and awareness campaigns on sustainable practices and responsible consumption, Strengthen international cooperation and collaboration on sustainable development goals, Advocate for policies and practices that prioritize the wellbeing of future generations, Monitor and evaluate the impact of interventions and adjust strategies as needed, Continuously engage with stakeholders and communities to ensure their participation and ownership in sustainable development efforts.
          |Consider the ultimate objective of your team: Save the planet Earth from socio-economic collapse.
          |Tasks should be sorted from highest to lowest priority.
          |Higher-priority tasks are those that act as pre-requisites or are more essential for meeting the objective.
          |Do not remove any tasks. Return the result as a numbered list in the format:
          |
          |#. First task
          |#. Second task
          |
          |The entries are consecutively numbered, starting with 1. The number of each entry must be followed by a period.
          |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    ),

    PrioritizationAgentPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",

      task_names = Seq(
        "Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market",
        "Develop a unique value proposition for the CLM software based on identified highpriority features and target industries",
        "Create a branding strategy for the CLM software including logo design and brand messaging",
        "Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry",
        "Identify key influencers in the contract management industry and develop a plan to engage with them",
        "Conduct user testing to ensure the usability and effectiveness of the CLM software",
        "Develop a pricing model for the CLM software that aligns with the value proposition and target industries",
        "Create a sales pitch and sales collateral for the CLM software",
        "Identify potential reseller partners and develop a strategy to engage with them",
        "Develop a customer support plan to ensure customer satisfaction and retention"
      ),

      // note a starting new line here (should be removed)
      prompt = """
        |You are tasked with cleaning the format and re-prioritizing the following tasks: Conduct a competitive analysis to identify strengths and weaknesses of existing CLM software in the market, Develop a unique value proposition for the CLM software based on identified highpriority features and target industries, Create a branding strategy for the CLM software including logo design and brand messaging, Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry, Identify key influencers in the contract management industry and develop a plan to engage with them, Conduct user testing to ensure the usability and effectiveness of the CLM software, Develop a pricing model for the CLM software that aligns with the value proposition and target industries, Create a sales pitch and sales collateral for the CLM software, Identify potential reseller partners and develop a strategy to engage with them, Develop a customer support plan to ensure customer satisfaction and retention.
        |Consider the ultimate objective of your team: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
        |Tasks should be sorted from highest to lowest priority.
        |Higher-priority tasks are those that act as pre-requisites or are more essential for meeting the objective.
        |Do not remove any tasks. Return the result as a numbered list in the format:
        |
        |#. First task
        |#. Second task
        |
        |The entries are consecutively numbered, starting with 1. The number of each entry must be followed by a period.
        |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    ),

    PrioritizationAgentPromptInfo(
      objective = "Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have.",

      task_names = Seq(
        "Develop a unique value proposition for the CLM software based on identified highpriority features and target industries",
        "Develop a pricing model for the CLM software that aligns with the value proposition and target industries",
        "Create a branding strategy for the CLM software including logo design and brand messaging",
        "Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry",
        "Identify key influencers in the contract management industry and develop a plan to engage with them",
        "Conduct user testing to ensure the usability and effectiveness of the CLM software",
        "Create a sales pitch and sales collateral for the CLM software",
        "Identify potential reseller partners and develop a strategy to engage with them",
        "Develop a customer support plan to ensure customer satisfaction and retention",
        "Conduct market research to identify target industries and potential customers for the CLM software",
        "Analyze the highpriority features identified in the market research to develop a unique value proposition for the CLM software",
        "Develop a pricing strategy that aligns with the unique value proposition and target industries",
        "Create a branding strategy for the CLM software including logo design and brand messaging that aligns with the unique value proposition and target industries",
        "Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry based on the unique value proposition and target industries",
        "Identify key influencers in the target industries and develop a plan to engage with them based on the unique value proposition and target industries",
        "Conduct user testing to ensure the usability and effectiveness of the CLM software based on the unique value proposition and target industries",
        "Create a sales pitch and sales collateral for the CLM software based on the unique value proposition and target industries",
        "Identify potential reseller partners in the target industries and develop a strategy to engage with them based on the unique value proposition and target industries",
        "Develop a customer support plan to ensure customer satisfaction and retention based on the unique value proposition and target industries"
      ),

      // note a starting new line here (should be removed)
      prompt = """
        |You are tasked with cleaning the format and re-prioritizing the following tasks: Develop a unique value proposition for the CLM software based on identified highpriority features and target industries, Develop a pricing model for the CLM software that aligns with the value proposition and target industries, Create a branding strategy for the CLM software including logo design and brand messaging, Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry, Identify key influencers in the contract management industry and develop a plan to engage with them, Conduct user testing to ensure the usability and effectiveness of the CLM software, Create a sales pitch and sales collateral for the CLM software, Identify potential reseller partners and develop a strategy to engage with them, Develop a customer support plan to ensure customer satisfaction and retention, Conduct market research to identify target industries and potential customers for the CLM software, Analyze the highpriority features identified in the market research to develop a unique value proposition for the CLM software, Develop a pricing strategy that aligns with the unique value proposition and target industries, Create a branding strategy for the CLM software including logo design and brand messaging that aligns with the unique value proposition and target industries, Develop a content marketing plan to educate potential customers on the benefits of the CLM software and establish thought leadership in the industry based on the unique value proposition and target industries, Identify key influencers in the target industries and develop a plan to engage with them based on the unique value proposition and target industries, Conduct user testing to ensure the usability and effectiveness of the CLM software based on the unique value proposition and target industries, Create a sales pitch and sales collateral for the CLM software based on the unique value proposition and target industries, Identify potential reseller partners in the target industries and develop a strategy to engage with them based on the unique value proposition and target industries, Develop a customer support plan to ensure customer satisfaction and retention based on the unique value proposition and target industries.
        |Consider the ultimate objective of your team: Find best strategies to market a new contract life cycle management (CLM) software and identify a set of high-priority features this software should have..
        |Tasks should be sorted from highest to lowest priority.
        |Higher-priority tasks are those that act as pre-requisites or are more essential for meeting the objective.
        |Do not remove any tasks. Return the result as a numbered list in the format:
        |
        |#. First task
        |#. Second task
        |
        |The entries are consecutively numbered, starting with 1. The number of each entry must be followed by a period.
        |Do not include any headers before your numbered list. Do not follow your numbered list with any other output.""".stripMargin
    )
  )

  "Task creation agent prompt" should "match the original prompt produced by Baby AGI" in {
    taskCreationAgentTestData.map(data =>
      BabyAGI.task_creation_agent_prompt(
        objective = data.objective,
        result = data.result,
        task_description = data.task_description,
        task_list = data.task_list
      ) shouldEqual data.prompt
    )
  }

  "Execution agent prompt" should "match the original prompt produced by Baby AGI" in {
    executionAgentTestData.map(data =>
      BabyAGI.execution_agent_prompt(
        objective = data.objective,
        task = data.task,
        context = data.context
      ) shouldEqual data.prompt
    )
  }

  "Prioritization agent prompt" should "match the original prompt produced by Baby AGI" in {
    prioritizationAgentTestData.map(data =>
      BabyAGI.prioritization_agent_prompt(
        data.task_names,
        data.objective
      ) shouldEqual data.prompt
    )
  }
}