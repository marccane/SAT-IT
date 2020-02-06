package structure

import scala.collection.mutable.ArrayBuffer

class Clause(private var clause: ArrayBuffer[Int]) { //s'ha de poguer accedir per index per intercanviar els literals quan sigui necessari pel 2wl

  def getClauseArray: Array[Int] = clause.toArray

  def getClause: ArrayBuffer[Int] = clause

  override def toString: String = {
    val sb = StringBuilder.newBuilder
    clause.foreach(x => sb.appendAll(x.toString + " ")) //millorable
    sb.result()
  }

}
