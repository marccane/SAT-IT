package event

case class BackjumpEvent(decisionLevel: Int, conflictIdx: Int) extends Event {

  override def print: Unit = {
    println(toString)
  }

  override def toString: String = {
    "BKJMP: " + decisionLevel
  }
}