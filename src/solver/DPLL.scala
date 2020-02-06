package solver

import event.{BacktrackEvent, DecisionEvent, UnitPropEvent}
import structure.enumeration.Reason.Reason
import structure.enumeration.SolvingState.SolvingState
import structure.enumeration.{Reason, SolvingState, State}
import structure._
import util.Constants._

//Invariant: El solver no esta inicialitzat o no te clausules unitaries insatisfetes o be hem trobat un conflicte
class DPLL extends ViewableSolver with TwoWatchedLiteralSolver{

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

    if(solverState == SolvingState.UNSOLVED) { //si no hem acabat...

      if (toPropagate.nonEmpty) {
        guiConflClause = unitPropagation()
        if(guiConflClause != -1) //conflicte o UNSAT, no ho podem saber sense mirar el trail (es podria fer O(1))
          return SOLVER_CONFLICT
        else
          return SOLVER_UNITPROP
      }

      if (guiConflClause != -1) { //si hi ha hagut conflicte...
        guiDecisionLevel = calculateBacktrackLevel()
        if (guiDecisionLevel < 0) { //UNSAT
          solverState = SolvingState.UNSAT
          return SOLVER_END
        }
        else {
          backtrack(guiDecisionLevel)
          return SOLVER_BACKTRACK
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
        val decisionLevel = calculateBacktrackLevel()
        if (decisionLevel < 0)
          return false
        else
          backtrack(decisionLevel)
      }
      else if (trail.length<numVariables){ //fer decisió
        val lit = makeDecision
        assign(lit, Reason.DECISION) //assignar i processar lit
      }
    }

    true
  }

  override def assign(literal: Int, reason: Reason, propagator: Int = -3): Unit ={
    if(reason != Reason.BACKTRACK)
      eventManager.insertEvent(
        if (reason == Reason.DECISION) DecisionEvent(literal, if (trail.isEmpty) 0 else decisionLevel(trail.last)+1)
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
  //Post: La longitud del trail es trailIndex (elimina elements del trail fins trailIndex, inclos)
  private def backtrack(trailIndex: Int): Unit = {
    val litBacktrack = trail(trailIndex)
    eventManager.insertEvent(BacktrackEvent(-litBacktrack)) //todo calcular nivell decisio

    while(trail.length>trailIndex) {
      val lit = trail.last
      val atom = math.abs(lit)
      varValue(atom) = State.UNDEF
      whoPropagated(atom) = -4
      trail -= lit
    }

    toPropagate.clear()
    assign(-litBacktrack, Reason.BACKTRACK, -5)
  }

  private def calculateBacktrackLevel(): Int ={ //es pot fer en temps constant
    var trailIdx = trail.length-1
    while(trailIdx>=0 && assignmentReason(math.abs(trail(trailIdx))) != Reason.DECISION)
      trailIdx -= 1
    trailIdx
  }

}
