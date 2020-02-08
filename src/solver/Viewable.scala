package solver

import structure.Instance

trait Viewable {

  protected var guiMode = false
  protected var guiConflClause: Int = -1
  protected var guiDecisionLevel: Int = -1

  def initSolverForGUI(instance: Instance, doInitialUnitProp: Boolean): Unit

  //Post: tornem l'accio que ha realitzat el solver
  def guiSolveStep: Int

}
