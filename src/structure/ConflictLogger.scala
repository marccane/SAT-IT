package structure

import scala.collection.mutable.ArrayBuffer

class ConflictLogger {
  private var log = new ArrayBuffer[ConflictLog]()

  def log(conflictLog: ConflictLog): Unit = {log += conflictLog}

  def getLog(logIdx: Int): ConflictLog = log(logIdx)
}

