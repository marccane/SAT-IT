package solver

import event.{BackjumpEvent, DecisionEvent, UnitPropEvent}
import structure.enumeration.Reason.Reason
import structure.enumeration.SolvingState.SolvingState
import structure.enumeration.{Reason, SolvingState, State}
import structure._
import util.Constants._

import scala.collection.mutable.ArrayBuffer

//Invariant: El solver no esta inicialitzat o no te clausules unitaries insatisfetes o be hem trobat un conflicte
class CDCL extends ViewableSolver with TwoWatchedLiteralSolver{

  val conflictLogger = new ConflictLogger()
  private var backjumpDelay: BackjumpDelay = _
  private class BackjumpDelay(val lit: Int, val propClause: Int)

  override def init(instance: Instance, stub: Boolean): SolvingState ={
    val instanceNumVars = instance.numVariables
    initStructures(instanceNumVars)

    val solvingStateAfterUP = super.init(instance)
    toPropagate.clear()

    initWatchedLiterals(clauses)

    solvingStateAfterUP
  }

  //No cridar directament, cridar MainGUI.SolveStep()
  override def guiSolveStep: Int = {

    if(solverState == SolvingState.UNSOLVED) {

      if (toPropagate.nonEmpty) {
        guiConflClause = unitPropagation()
        if(guiConflClause != -1) //conflicte o UNSAT, no ho podem saber sense mirar el trail (es podria fer O(1))
          return SOLVER_CONFLICT
        else
          return SOLVER_UNITPROP
      }

      if (guiConflClause != -1) { //si hi ha hagut conflicte...
        guiDecisionLevel = conflictAnalysisAndLearning(guiConflClause)
        if (guiDecisionLevel < 0) { //UNSAT
          solverState = SolvingState.UNSAT
          return SOLVER_END
        }
        else {
          backjump(guiDecisionLevel)
          return SOLVER_BACKJUMP
        }
      }
      else if (trail.length < numVariables) {
        val lit = makeDecision
        assign(lit, Reason.DECISION)
        return SOLVER_DECISION
      }
      else{ //tenim totes les variables assignades i no hi ha hagut conflicte => SAT
        solverState = SolvingState.SAT
        return SOLVER_END
      }
    }
    SOLVER_END
  }

  override protected def i_solve(instance: Instance): Boolean = {
    val UPresult = init(instance)

    if(UPresult==SolvingState.SAT)
      return true
    else if(UPresult==SolvingState.UNSAT)
      return false

    while(trail.length<numVariables){
      val conflClause = unitPropagation()

      if(conflClause != -1) { //si hi ha hagut conflicte...
        val decisionLevel = conflictAnalysisAndLearning(conflClause)
        if (decisionLevel < 0)
          return false
        else
          backjump(decisionLevel)
      }
      else if (trail.length<numVariables){ //fer decisió
        val lit = makeDecision
        assign(lit, Reason.DECISION) //assignar i processar lit
      }
    }

    true
  }

  override def assign(literal: Int, reason: Reason, propagator: Int = -3): Unit ={
    eventManager.insertEvent(
      if(reason == Reason.DECISION) DecisionEvent(literal, if (trail.isEmpty) 0 else decisionLevel(trail.last)+1)
      else new UnitPropEvent(literal)
    )

    val atom = math.abs(literal)
    varValue(atom) = if(literal>0) State.TRUE else State.FALSE
    assignmentReason(atom) = reason
    trail.append(literal)
    whoPropagated(atom) = propagator
    toPropagate.push(literal)
  }

  //Pre: trailIndex < trail.length
  //Post: La longitud del trail es trailIndex
  private def backjump(trailIndex: Int): Unit = {
    eventManager.insertEvent(BackjumpEvent(decisionLevel(trail(trailIndex/*-1*/)), eventManager.getConflictIdx))

    while(trail.length>trailIndex){
      val lit = trail.last
      val atom = math.abs(lit)
      varValue(atom) = State.UNDEF
      whoPropagated(atom) = -4
      trail -= lit
    }

    toPropagate.clear()
    assign(backjumpDelay.lit, Reason.UNITPROP, backjumpDelay.propClause)
  }

  private def calcLastDecisionLevelLits: ArrayBuffer[Int] ={
    val buff = new ArrayBuffer[Int]()
    var i=trail.length-1
    while(i>=0 && assignmentReason(math.abs(trail(i))) == Reason.UNITPROP){
      buff.append(trail(i))
      i-=1
    }
    buff.append(trail(i)) //el literal decidit tambe pertany al nivell de decisio actual
    buff.reverse
  }

  //Post: si hi han variables propagades, retorna l'atom d'una, altrament retorna la variable de decisio
  private def getLastLevelLit(clause: Set[Int], lastDecisionLevelLits: ArrayBuffer[Int]): Int ={ //aixo s'haura de canviar i no podra ser un set si volem el mateix comportament que a classe
    val absClause = clause.map(math.abs)
    var it_ldll = lastDecisionLevelLits.length-1
    while(it_ldll>=0){
      val lit = lastDecisionLevelLits(it_ldll)
      if(absClause.contains(math.abs(lit))){
        return lit
      }
      it_ldll -= 1
    }
    throw new Exception
  }

  //Pre: El primer o segon literal de newClause conté el literal aprés
  //Post: Obté l'últim literal de newClause que s'ha assignat en el trail, o un indefinit, i fa swap amb la primera
  //      o segona posició de newClause
  private def secondWlearned(indexForcedLit : Int, newClause : Clause, newClauseIndex : Int) {

    //Mirem si hi ha una clàusula indefinida
    //Índex de la posició on potser estar el literal forcat a propagar-se
    var indexForcWatch = 0
    //Índex de la posició on anira el nou observat
    var indexNewWatch = 1
    //Si el literal forcat ja estava a la segona posició, hem de canviar els valors anteriors
    if(indexForcedLit == 1) {
      indexForcWatch = 1
      indexNewWatch = 0
    }

    var i = 2
    var undefinedFound = false
    var trobat = false
    val clause = newClause.getClause
    while(i < clause.length && !undefinedFound)
    {
      if (varValue(math.abs(clause(i))) == State.UNDEF) { //busquem un literal no assignat
        undefinedFound = true
      }
      else i += 1
    }

    if(!undefinedFound) {
      i = 0
      for (l <- trail.reverse; if !trobat) {
        if (clause.contains(-l) && -l != newClause.getClause(indexForcWatch)) {
          trobat = true
          i = newClause.getClause.indexOf(-l)
        }
      }
    }
    if (trobat || undefinedFound) {
      swapWL(newClause, newClauseIndex, indexNewWatch, i)
    }
  }


  private def calculateBackjumpLevel(clauseAct: Set[Int], newClause: Clause, newClauseIndex: Int): Int ={
    var trailIdx = 0
    var penultimateLiteral = 0
    var learntClause = clauseAct

    //mentre la nova clausula tingui mes dun literal sense valor...
    while (learntClause.size > 1) { //si es TLA no entrarem aqui
      val lit = trail(trailIdx)
      if (learntClause.contains(-lit)) {
        if(learntClause.size==2)
          penultimateLiteral = lit
        learntClause -= -lit
      }
      trailIdx += 1
    }
    //trailIdx apunta al literal que ens ha fet fer unitprop en la clausula apresa (no al que propaguem)

    //apuntem al primer literal del proxim nivell de decisio
    while(trailIdx<trail.length && assignmentReason(math.abs(trail(trailIdx))) == Reason.UNITPROP)
      trailIdx += 1

    val litToForceProp = learntClause.head

    backjumpDelay = new BackjumpDelay(litToForceProp,newClauseIndex)

    //FIXME: molt probablement el bug del 2wl sera aqui
    //actualitzar watched lits per tenir watched el literal que passarà a true
    val indexForcedLit = newClause.getClause.indexOf(litToForceProp)
    /*
    if(indexForcedLit != 0) //si l'ultim literal del nivell de decisio no esta watched a la primera posicio li posem
      swapWL(newClause,newClauseIndex,0,indexForcedLit)

    val indexPenultimateLit = newClause.getClause.indexOf(-penultimateLiteral)
    if(indexForcedLit != 1)
      swapWL(newClause,newClauseIndex,1,indexPenultimateLit)*/

    if(indexForcedLit>1) //si l'ultim literal del nivell de decisio no esta watched li posem
      swapWL(newClause,newClauseIndex,0,indexForcedLit)

    secondWlearned(indexForcedLit, newClause, newClauseIndex)

    trailIdx
  }

  private def conflictAnalysisAndLearning(failClauseIdx: Int): Int = {

    //si al trail només queden propagacions pleguem perquè no podem fer backjump enlloc
    if(trail.forall(b => assignmentReason(math.abs(b)) == Reason.UNITPROP)) //possible optimitzacio amb una variable numDecisions
      return -1

    val failClause = clauses.getClause(failClauseIdx)

    val lastDecisionLevelLits = calcLastDecisionLevelLits
    val lastDecisionLevelLitsSet = lastDecisionLevelLits.map(-_).toSet

    var resolvent = failClause.getClause.toSet
    var lastLevelLit = getLastLevelLit(resolvent, lastDecisionLevelLits)

    //logging
    var clausesLeft = new ArrayBuffer[Set[Int]]()
    var clausesRight = new ArrayBuffer[Set[Int]]()
    var resolutionLits = new ArrayBuffer[Int]()
    val conflictClauses = (failClauseIdx, whoPropagated(math.abs(lastLevelLit)))

    while(resolvent.intersect(lastDecisionLevelLitsSet).size>1){ //mentre resolvent tingui mes d'un literal de lultim nivell de decisio...
      val indexClauseToSolveWith = whoPropagated(math.abs(lastLevelLit))
      var clauseToSolveWith = clauses.getClause(indexClauseToSolveWith).getClause.toSet

      //logging
      clausesLeft += resolvent
      clausesRight += clauseToSolveWith
      resolutionLits += lastLevelLit

      //aplicar resolucio
      clauseToSolveWith -= lastLevelLit
      resolvent -= -lastLevelLit
      resolvent = resolvent.union(clauseToSolveWith)

      lastLevelLit = getLastLevelLit(resolvent, lastDecisionLevelLits)
    }

    //logging
    clausesLeft += resolvent
    conflictLogger.log(new ConflictLog(clausesLeft, clausesRight, resolutionLits, lastDecisionLevelLitsSet, conflictClauses))

    val newClauseAB = resolvent.to(ArrayBuffer)
    val newClause = new Clause(newClauseAB)

    //afegir nova clausula
    val newClauseIndex = clauses.addClause(newClause)

    //Afegir literals de la nova clausula a la watch list
    addToWatchList(newClauseAB(0),newClauseIndex)
    if(newClauseAB.length>1)
      addToWatchList(newClauseAB(1),newClauseIndex)

    calculateBackjumpLevel(resolvent, newClause, newClauseIndex)
  }

}
