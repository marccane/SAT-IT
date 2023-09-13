package solver

import event.{BacktrackEvent, DecisionEvent}
import structure._
import structure.enumeration.Reason.Reason
import structure.enumeration.SolvingState.SolvingState
import structure.enumeration.{Reason, SolvingState, State}
import util.Constants._

class Backtracking extends ViewableSolver {

  private var conflictFound = false

  override def init(instance: Instance, stub: Boolean): SolvingState ={
    val solvingStateAfterUP = super.init(instance, doInitialUnitProp = false)
    solvingStateAfterUP
  }

  //No cridar directament, cridar MainWindow.SolveStep()
  override def guiSolveStep: Int = {
    val candidatesExist = ()=> trail.length<numVariables || trail.length==numVariables && assignmentReason.contains(Reason.DECISION)
    val clausesArr = clauses.initialClauses.getClauseList

    if(solverState == SolvingState.UNSOLVED) {
      if(conflictFound){ //aixo es fa "DESPRES" del que hi ha a sota
        conflictFound = false
        val decisionLevel = calculateBacktrackLevel()
        if (decisionLevel < 0) {
          solverState = SolvingState.UNSAT
          return SOLVER_END
        }
        else {
          backtrack(decisionLevel)
          return SOLVER_BACKTRACK
        }
      }

      val unsatClause = clausesArr.find(_.getClause.forall(x => trail.contains(-x)))
      if (unsatClause.isDefined) {
        guiConflClause = clauses.initialClauses.getClauseList.zipWithIndex.find(_._1==unsatClause.get).get._2
        conflictFound = true
        return SOLVER_CONFLICT //conflicte trobat, no fem backtrack encara
      }
      else if(trail.length<numVariables){
        val lit = makeDecision
        if(!this.isCancel) assign(lit, Reason.DECISION) //Assignar i processar lit
        else return CANCEL_DECISION
        return SOLVER_DECISION
      }

      if(trail.length == numVariables && (1 until clauses.getNumClauses).forall(isSatisfied)) {
        solverState = SolvingState.SAT
        return SOLVER_END
      }
    }
    SOLVER_END
  }

  override protected def i_solve(instance: Instance): Boolean = {
    init(instance)

    val candidatesExist = ()=>trail.length<numVariables || trail.length==numVariables && assignmentReason.contains(Reason.DECISION)
    val clausesArr = clauses.initialClauses.getClauseList

    var solved = false
    while(!solved){

      val unsatClause = clausesArr.find(_.getClause.forall(x => trail.contains(-x)))
      if (unsatClause.isDefined) {
        val decisionLevel = calculateBacktrackLevel()
        if (decisionLevel < 0)
          return false
        else
          backtrack(decisionLevel)
      }
      else if(trail.length<numVariables){
        val lit = makeDecision
        assign(lit, Reason.DECISION) //Assignar i processar lit
      }

      if(trail.length == numVariables && (0 until clauses.getNumClauses).forall(isSatisfied))
        solved = true
    }

    true
  }

  override def assign(literal: Int, reason: Reason, stub: Int = -3): Unit ={
    if (reason == Reason.DECISION)
      eventManager.insertEvent(
       new DecisionEvent(literal, if (trail.isEmpty) 0 else decisionLevel(trail.last)+1)
    )

    val atom = math.abs(literal)
    varValue(atom) = if(literal>0) State.TRUE else State.FALSE
    assignmentReason(atom) = reason
    trail.append(literal)
    if(reason == Reason.DECISION && historySolver != null) historySolver.addLiteral(literal)
    addDetectedBreakpoint(atom)
  }

  //Pre: trailIndex < trail.length
  //Post: La longitud del trail es trailIndex
  private def backtrack(trailIndex: Int): Unit = {
    val litBacktrack = trail(trailIndex)
    eventManager.insertEvent(new BacktrackEvent(-litBacktrack))

    while(trail.length>trailIndex) {
      val lit = trail.last
      val atom = math.abs(lit)
      varValue(atom) = State.UNDEF
      trail -= lit
    }

    assign(-litBacktrack, Reason.BACKTRACK)
  }

  private def calculateBacktrackLevel(): Int ={ //es pot fer en temps constant
    var trailIdx = trail.length-1
    while(trailIdx>=0 && assignmentReason(math.abs(trail(trailIdx))) != Reason.DECISION)
      trailIdx -= 1
    trailIdx
  }

}
