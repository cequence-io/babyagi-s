package io.cequence.babyagis.next

import scala.collection.mutable.{Buffer => MutableBuffer}

// original ported SingleTaskListStorage
class SingleTaskListStorage {
  type Dict = Map[String, Any]

  var tasks: MutableBuffer[Dict] = MutableBuffer() // TODO: deque?

  var task_id_counter: Int = 0

  def append(task: Dict): Unit =
    tasks.append(task)

  def replace(tasks: Seq[Dict]): Unit = {
    this.tasks = MutableBuffer(tasks: _*) // TODO: deque?
  }

  def popleft: Dict =
    tasks.remove(0)

  def is_empty: Boolean =
    tasks.isEmpty

  def next_task_id: Int = {
    task_id_counter += 1
    task_id_counter
  }

  def get_task_names: Seq[String] =
    tasks.map(t => t("task_name").toString).toSeq
}
