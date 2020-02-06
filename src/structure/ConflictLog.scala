package structure

import scala.collection.mutable.ArrayBuffer

class ConflictLog(val clausesLeft: ArrayBuffer[Set[Int]], val clausesRight: ArrayBuffer[Set[Int]],
                  val resolutionLits: ArrayBuffer[Int], val lastDecisionLevelLits: Set[Int])
