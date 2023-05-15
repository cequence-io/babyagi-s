package io.cequence.babyagis.port

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Properties

class BabyAGITaskStorageSpec extends AnyFlatSpec with Matchers {

  val newTasks = Seq(
    Map("task_name" -> "Conduct a comprehensive analysis of the current state of the planets natural resources and their impact on socioeconomic systems"),
    Map("task_name" -> "Develop strategies for sustainable resource management and conservation"),
    Map("task_name" -> "Identify and address inequalities and disparities in access to resources and opportunities"),
    Map("task_name" -> "Implement policies and programs to promote sustainable and equitable economic growth"),
    Map("task_name" -> "Foster innovation and entrepreneurship in sustainable industries"),
    Map("task_name" -> "Develop and implement education and awareness campaigns on sustainable practices and responsible consumption"),
    Map("task_name" -> "Strengthen international cooperation and collaboration on sustainable development goals"),
    Map("task_name" -> "Advocate for policies and practices that prioritize the wellbeing of future generations"),
    Map("task_name" -> "Monitor and evaluate the impact of interventions and adjust strategies as needed"),
    Map("task_name" -> "Continuously engage with stakeholders and communities to ensure their participation and ownership in sustainable development efforts")
  )

  val newTasks2 = Seq(
    Map("task_name" -> "Conduct a study on the impact of climate change on natural resources and socioeconomic systems including the effects of rising sea levels extreme weather events and changes in temperature and precipitation patterns", "task_id" -> 13),
    Map("task_name" -> "Develop policies and programs to promote sustainable and equitable access to resources and opportunities particularly for marginalized communities and vulnerable populations", "task_id" -> 14),
    Map("task_name" -> "Implement strategies for sustainable resource management and conservation including the use of renewable energy sources the reduction of waste and pollution and the protection of biodiversity", "task_id" -> 15),
    Map("task_name" -> "Foster innovation and entrepreneurship in sustainable industries such as green energy sustainable agriculture and ecotourism", "task_id" -> 16),
    Map("task_name" -> "Develop and implement education and awareness campaigns on sustainable practices and responsible consumption targeting both individuals and businesses", "task_id" -> 17),
    Map("task_name" -> "Strengthen international cooperation and collaboration on sustainable development goals including the sharing of best practices and the development of joint initiatives", "task_id" -> 18),
    Map("task_name" -> "Advocate for policies and practices that prioritize the wellbeing of future generations including the adoption of longterm planning and the consideration of intergenerational equity", "task_id" -> 19),
    Map("task_name" -> "Monitor and evaluate the impact of interventions and adjust strategies as needed using datadriven approaches and stakeholder feedback", "task_id" -> 20),
    Map("task_name" -> "Continuously engage with stakeholders and communities to ensure their participation and ownership in sustainable development efforts including the use of participatory decisionmaking processes and communitybased initiatives", "task_id" -> 21)
  )

  "Single task list storage" should "behave as the original Baby AGI" in {
    val taskStorage = new SingleTaskListStorage()

    assert(taskStorage.is_empty, "Must be empty")

    val firstTaskName = "Develop a task list"

    val initial_task = Map(
      "task_id" -> taskStorage.next_task_id,
      "task_name" -> firstTaskName
    )

    taskStorage.append(initial_task)

    assert(!taskStorage.is_empty, "Must not be empty")

    val taskNames = taskStorage.get_task_names

    taskNames.size shouldBe 1

    taskNames(0) shouldEqual firstTaskName

    val newTasksWithIds = newTasks.map { newTask =>
      newTask + ("task_id" -> taskStorage.next_task_id)
    }

    newTasksWithIds.foreach(taskStorage.append)

    val taskNames2 = taskStorage.get_task_names

    taskNames2.size shouldBe (newTasksWithIds.size + 1)

    taskNames2(0) shouldEqual firstTaskName

    taskNames2.drop(1).zip(newTasksWithIds).foreach { case (taskName, newTask) =>
      taskName shouldEqual newTask("task_name")
    }

    taskStorage.popleft shouldEqual initial_task

    taskStorage.get_task_names.size shouldBe newTasksWithIds.size

    taskStorage.popleft shouldEqual newTasksWithIds(0)

    newTasksWithIds(0)("task_id") shouldBe 2

    taskStorage.popleft shouldEqual newTasksWithIds(1)

    newTasksWithIds(1)("task_id") shouldBe 3

    taskStorage.popleft shouldEqual newTasksWithIds(2)

    newTasksWithIds(2)("task_id") shouldBe 4

    taskStorage.popleft shouldEqual newTasksWithIds(3)

    newTasksWithIds(3)("task_id") shouldBe 5

    val taskNames3 = taskStorage.get_task_names

    taskNames3.size shouldBe 6

    taskNames3.zip(newTasksWithIds.drop(4)).foreach { case (taskName, newTask) =>
      taskName shouldEqual newTask("task_name")
    }

    taskStorage.next_task_id shouldBe 12

    taskStorage.next_task_id shouldBe 13

    taskStorage.append(
      Map(
        "task_id" -> taskStorage.next_task_id,
        "task_name" -> "Test test"
      )
    )

    taskStorage.get_task_names.size shouldBe 7

    taskStorage.popleft shouldEqual newTasksWithIds(4)

    taskStorage.replace(newTasks2)

    taskStorage.get_task_names.size shouldBe 9

    taskStorage.popleft shouldEqual newTasks2(0)
  }
}