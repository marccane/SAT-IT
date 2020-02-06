package event

case class BacktrackEvent(val lit: Int, val decisionLevel: Int = -1) extends Event {

  //def this(){this(1)}

  override def print: Unit = {
    println(toString)
  }

  override def toString: String = {
    "BT: " + lit + (if(decisionLevel != -1) "DL = " + decisionLevel else "")
  }
}
