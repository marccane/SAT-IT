package solver

import structure.Instance

abstract class ViewableSolver extends Solver with Viewable {

  //Overrides Viewable
  override def initSolverForGUI(instance: Instance, doInitialUnitProp: Boolean): Unit ={
    guiMode = true
    solverState = init(instance, doInitialUnitProp)
  }

}
