package solver

import structure.enumeration.{Reason, State}
import structure.{Clause, ClauseWrapper}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait  TwoWatchedLiteralSolver extends Solver{
  var watchListPos: ArrayBuffer[ArrayBuffer[Int]] = _ //Per cada literal positiu, llista de clausules on apareix watched
  var watchListNeg: ArrayBuffer[ArrayBuffer[Int]] = _ //Per cada literal negatiu, llista de clausules on apareix watched
  var whoPropagated: ArrayBuffer[Int] = _ //Per cada variable, quina clausula l'ha propagat (Valors especials -> no inicialitzat: -1, unitpropinicial: -2, decisio: -3, desassignat per backjump: -4)
  var toPropagate: mutable.Stack[Int] = _ //Stack de literals

  //Getters
  def getWhoPropagated(integer: Integer): Integer = whoPropagated(math.abs(integer))

  //Metodes
  protected def initStructures(instanceNumVars: Int): Unit = {
    whoPropagated = new ArrayBuffer[Int](instanceNumVars+1)
    for(_ <- 0 to instanceNumVars)
      whoPropagated.append(-1)

    toPropagate = new mutable.Stack[Int]()

    watchListPos = new ArrayBuffer[ArrayBuffer[Int]](instanceNumVars+1)
    watchListNeg = new ArrayBuffer[ArrayBuffer[Int]](instanceNumVars+1)
    for(_ <- 0 to instanceNumVars) { //variables van de 1 a n
      watchListPos.append(new ArrayBuffer[Int]())
      watchListNeg.append(new ArrayBuffer[Int]())
    }
  }

  protected def initWatchedLiterals(clauses: ClauseWrapper): Unit = {
    val clauseList = clauses.getClauseList
    for(clauseIndex <- clauseList.indices) {
      val clause = clauseList(clauseIndex).getClause

      var i = 0
      val lim = math.min(2, clause.length) //afegim com a maxim 2 elements (els wl)
      while(i<lim)
      {
        val lit = clause(i)
        if(lit>0)
          watchListPos(lit).append(clauseIndex) //hauriem de comprovar que cada clausula nomes s'afegeix una vegada?
        else
          watchListNeg(-lit).append(clauseIndex)

        i+=1
      }
    }
  }

  protected def addToWatchList(lit: Int, clauseIndex: Int): Unit ={
    val atom = math.abs(lit)
    if(lit<0)
      watchListNeg(atom).append(clauseIndex)
    else
      watchListPos(atom).append(clauseIndex)
  }

  protected def removeFromWatchList(lit: Int, clauseIndex: Int): Unit = {
    val atom = math.abs(lit)
    if(lit<0)
      watchListNeg(atom) -= clauseIndex
    else
      watchListPos(atom) -= clauseIndex
  }

  //Pre: 0 <= i1 < 2, i2 > 1 , i1 i i2 són index valids de clause
  //Post: literals swapejats i watchlist actualitzada
  protected def swapWL(clause: Clause, clauseIndex: Int, i1: Int, i2: Int): Unit ={
    val clauseAB = clause.getClause
    val notWatched = clauseAB(i1)
    val watched = clauseAB(i2)
    clauseAB(i1) = watched
    clauseAB(i2) = notWatched
    removeFromWatchList(notWatched,clauseIndex)
    addToWatchList(watched,clauseIndex)
  }

  //Pre: Les clausules tenen almenys dos literals o be en tenen un de satisfet
  //Post: Fa les propagacions unitaries necessaries retorna la clausula que ha generat el conflicte, -1 en cas de no conflicte
  protected def unitPropagation(): Int ={

    while(toPropagate.nonEmpty){

      val lit = toPropagate.pop()
      val falseLit = -lit
      val atom = math.abs(lit)

      //Buscar clausules on apareix -lit, s'hauran de comprovar
      val watchListFalse = if(lit<0) watchListPos(atom) else watchListNeg(atom)

      //val watchListFalseCopy = watchListFalse.toArray //aixo no podia afectar en res, nomes es va usar per saltar un bug
      for(clauseIndex <- watchListFalse.toArray){
        val clause = clauses.getClause(clauseIndex).getClause

        if (!isSatisfied(clauseIndex)) { //70% del temps del programa aqui

          //apartem el literal fals cap a la segona posicio
          if (clause(0) == falseLit) {
            clause(0) = clause(1)
            clause(1) = falseLit
          }

          var j = 2
          var undefinedFound = false
          val clLength = clause.length
          while (j < clLength && !undefinedFound) {
            if (varValue(math.abs(clause(j))) == State.UNDEF) { //busquem un literal no assignat
              swapWL(clauses.getClause(clauseIndex),clauseIndex,1,j)
              undefinedFound = true
            }
            j += 1
          }

          if (!undefinedFound) { //la clausula no conte cap altre literal undefined (excepte de clause(0))
            val firstLit = clause(0)
            if(varValue(math.abs(firstLit))==State.UNDEF){ //fer propagacio unitaria
              assign(firstLit,Reason.UNITPROP,clauseIndex) //s'afegeix al toPropagate
            }
            else{ //contradiccio
              toPropagate.clear()
              return clauseIndex
            }
          }
        }
      }
    }
    -1
  }

}
