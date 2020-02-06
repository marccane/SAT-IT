package structure

import scala.collection.mutable.ArrayBuffer

class ClauseList(private var clauses: ArrayBuffer[Clause]) { //s'ha de poguer accedir per index

  def this(clauses: Array[Array[Int]]){
    this(clauses.map(x => new Clause(x.to(ArrayBuffer))).to(ArrayBuffer))
  }

  def this(instance: Instance){
    this(instance.instance)
  }

  def numClauses: Int = clauses.length

  def getClauseList: Array[Clause] = clauses.toArray

  def getClause(index: Int): Clause = clauses(index)

  def addClause(clause: Clause): Unit ={
    clauses.append(clause)
  }

}
