package event

import scala.collection.mutable.ListBuffer

case class UnitPropEvent(var literals: ListBuffer[Int]) extends Event {

  def this(){
    this(new ListBuffer[Int])
  }

  def this(literal: Int){
    this(new ListBuffer[Int].+=(literal))
  }

  override def print: Unit = {
    println(toString)
  }

  def addLiteral(literal: Int): Unit = {
    literals.+=(literal)
  }

  override def toString: String = {
    val sb = new StringBuilder
    sb.append("UP: ")
    literals.foreach(x => sb.append(x + " "))
    sb.result()
  }
}
