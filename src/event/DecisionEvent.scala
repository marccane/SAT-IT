package event

case class DecisionEvent(literal: Int, decisionLevel: Int = -2) extends Event {

  override def print: Unit = {
    println(toString)
  }

  override def toString: String = {
    "D: " + literal + (if (decisionLevel != -2) " (DL = " + decisionLevel + ")" else "")
  }
}
