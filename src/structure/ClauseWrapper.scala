package structure

import scala.collection.mutable.ArrayBuffer

//Les clausules comencen per 0, els literals comencen per 1
class ClauseWrapper {

  var numVariables = 0
  var numInitialClauses = 0

  var initialClauses = new ClauseList(ArrayBuffer[Clause]())
  val learnt = new ClauseList(ArrayBuffer[Clause]())

  def init(clausesArg: Array[Array[Int]], numVars: Int){
    numVariables = numVars
    numInitialClauses = clausesArg.length
    initialClauses = new ClauseList(clausesArg)
  }

  def init(instance: Instance){
    init(instance.instance, instance.numVariables)
  }

  def getNumClauses: Int = initialClauses.numClauses + learnt.numClauses

  def getNumVariables: Int = numVariables

  def getClauseList: Array[Clause] = initialClauses.getClauseList ++ learnt.getClauseList

  def getClause(index: Int): Clause = {
    if(index<numInitialClauses) initialClauses.getClause(index)
    else learnt.getClause(index-numInitialClauses)
  }

  //Post: Retorna l'index de la nova clausula
  def addClause(clause: Clause): Int ={
    learnt.addClause(clause)
    numInitialClauses + learnt.numClauses -1
  }

}
