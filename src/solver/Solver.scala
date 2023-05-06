package solver

import gui.DecisionCallback
import javafx.util.Pair
import structure.enumeration.Reason.Reason
import structure.enumeration.{Reason, SolvingState, State}
import structure.enumeration.SolvingState.SolvingState
import structure.enumeration.State.State
import structure.{ClauseWrapper, EventManager, Instance, LitScoreSet, VSIDSPropiety}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class Solver {

  var solverState: SolvingState.Value = SolvingState.UNSOLVED

  protected var eventManager: EventManager = new EventManager
  protected var correctSolution = false

  var clauses: ClauseWrapper = _
  var trail: ArrayBuffer[Int] = _
  var varValue: ArrayBuffer[State] = _
  var assignmentReason: ArrayBuffer[Reason] = _
  var vsids: Boolean = false
  var vsidsPropiety: VSIDSPropiety = _
  var historySolver: HistorySolver = _
  var detectedVarBreakpoints: mutable.Queue[Int] = _
  var varBreakpoints: mutable.HashSet[Int] = _
  protected var isCancel = false

  protected var numVariables: Int = -1 //per evitar recalcular


  //Metodes comuns solver
  def init(instance: Instance, doInitialUnitProp: Boolean = true): SolvingState = {
    numVariables = instance.numVariables
    trail = new ArrayBuffer[Int](numVariables)
    varBreakpoints = new mutable.HashSet[Int]()
    detectedVarBreakpoints = new mutable.Queue[Int]()
    isCancel = false
    var solvingStateAfterUP: SolvingState = SolvingState.UNSOLVED

    varValue = new ArrayBuffer[State](numVariables+1)
    for(_ <- 0 to numVariables) //variables van de 1 a n
      varValue.append(State.UNDEF)

    assignmentReason = new ArrayBuffer[Reason](numVariables+1)
    for(_ <- 0 to numVariables) //variables van de 1 a n
      assignmentReason.append(Reason.UNITPROP)

    clauses = new ClauseWrapper()
    if(doInitialUnitProp) {
      //println("clausules inicials = " + instance.instance.length)
      val unitPropResult = initialUnitProp(instance)
      solvingStateAfterUP = unitPropResult._1
      val unitPropClauses = unitPropResult._2.map(_.toArray).toArray
      //println("clausules despres de UP = " + unitPropClauses.length)
      clauses.init(unitPropClauses, numVariables)
    }
    else{
      clauses.init(instance.instance, numVariables)
      solvingStateAfterUP = if(instance.instance.isEmpty) SolvingState.SAT
      else if(instance.instance.exists(_.isEmpty)) SolvingState.UNSAT
      else SolvingState.UNSOLVED
    }

    solvingStateAfterUP
  }


  //Post: retorna cert si la instancia es satisfactible, fals en c.c.
  def solve(instance: Instance, print: Boolean = true): Boolean = {

    val solutionFound = time(i_solve(instance), print)

    if(solutionFound) solverState = SolvingState.SAT
    else solverState = SolvingState.UNSAT

    correctSolution = checkSolution()

    solutionFound
  }

  protected def i_solve(instance: Instance): Boolean

  protected def assign(literal: Int, reason: Reason, propagator: Int = -3): Unit

  private var callbackDecision: () => Int = ()=>initialMakeDecision


  def setDecisionCallback(decisionCallback: DecisionCallback): Unit ={
    callbackDecision = decisionCallback.makeDecisionCallback
  }

  def resetDecisionCallback(): Unit ={
    callbackDecision = ()=>initialMakeDecision
  }

  protected def makeDecision: Int = callbackDecision()

  def initialMakeDecision: Int ={
    for(i <- 1 to varValue.length) {
      if(varValue(i) == State.UNDEF)
        return i
    }
    -1
  }

  def initialMakeDecisionVSIDS: Int={
    -1
  }

  //Post: Retorna cert si es un literal correcte, fals en c.c.
  def validDecisionLiteral(literal: Int): Boolean ={
    val atom = math.abs(literal)
    atom>0 && atom<=numVariables && varValue(atom) == State.UNDEF
  }

  //Pre: -
  //Post: retorna el nivell de decisió del literal si aquest està assignat, -1 en cas contrari
  protected def decisionLevel(literal: Int): Int ={
    val atom = math.abs(literal)
    var dl = 0

    val it = trail.iterator
    while(it.hasNext){
      val actAtom = math.abs(it.next())
      if(assignmentReason(actAtom)==Reason.DECISION)
        dl += 1
      if(actAtom==atom)
        return dl
    }

    -1
  }

  //Post: retorna cert si el literal esta satisfet
  protected def trueLiteral(literal: Int): Boolean ={
    val positive = literal>0
    val state = varValue(math.abs(literal))
    state == State.TRUE && positive || state == State.FALSE && !positive
  }

  //Post: retorna cert si el literal esta falsificat
  protected def falseLiteral(literal: Int): Boolean={
    val positive = literal>0
    val state = varValue(math.abs(literal))
    !positive && state==State.TRUE || positive && state==State.FALSE
  }

  //Pot: retorna si el literal esta indefinit
  protected def undifinedLiteral(literal : Int): Boolean ={
    varValue(math.abs(literal)) == State.UNDEF
  }

  //Post: retorna cert si la clausula 'indexClause' esta satisfeta
  protected def isSatisfied(clauseIndex: Int): Boolean ={
    val clause = clauses.getClause(clauseIndex).getClause
    for(lit <- clause){
      if(trueLiteral(lit))
        return true
    }
    false
  }

  def checkSolution(): Boolean ={
    val sol = trail.toSet
    val allClausesSat = clauses.initialClauses.getClauseList.map(_.getClause).
      map(_.foldLeft(false)(_ || sol.contains(_))).forall(_==true)
    val allVarsAssigned = trail.size == clauses.numVariables
    allClausesSat && allVarsAssigned
  }

  //Pre: instance inicialitzada
  //Post: Retorna l'estat del problema i l'estat de les clausules un cop fet la propgacio unitaria inicial (SolvingState, clausules)
  protected def initialUnitProp(instance: Instance): (SolvingState, ArrayBuffer[ArrayBuffer[Int]]) ={

    var initClauses = instance.instance
      .map(_.toSet) //eliminem literals repetits
      .map(_.to(ArrayBuffer)).to(ArrayBuffer)
    val checkEmptyClause = (x: ArrayBuffer[ArrayBuffer[Int]]) => x.exists(_.isEmpty)

    var unitClauseOption = initClauses.find(_.length==1)
    while(unitClauseOption.isDefined) {

      val unitLit = unitClauseOption.get.head

      assign(unitLit, Reason.UNITPROP, -2)

      //unit subsumption
      initClauses = initClauses.filter(!_.contains(unitLit))

      //unit resolution
      for(clause <- initClauses){
        clause -= -unitLit
        if(clause.isEmpty)
          return (SolvingState.UNSAT, initClauses)
      }

      unitClauseOption = initClauses.find(_.length==1)
    }

    var res: SolvingState.Value = SolvingState.UNSOLVED
    if(initClauses.isEmpty)
      res = SolvingState.SAT
    else if(checkEmptyClause(initClauses))
      res = SolvingState.UNSAT

    (res, initClauses)
  }

  //Getters
  def variablesState: Array[State] = varValue.toArray

  def getTrailIndex(idx: Int): Int = trail(idx)

  def resetEventManager() ={
    eventManager.reset()
  }

  def getStatics() = {
    (solverState, eventManager.getStatistics, trail.sortWith(math.abs(_) < math.abs(_)))
  }

  //Prints
  def printEvents(compliantFormat: Boolean): Unit = eventManager.printEvents(compliantFormat)

  def printSolution(): Unit = {
    if(solverState==SolvingState.SAT) {
      println("Decisions: " + eventManager.getStatistics._1)
      println("Propagations: " + eventManager.getStatistics._2)
      println("Conflicts: " + eventManager.getStatistics._3)
      print("v ")
      trail.sortWith(math.abs(_) < math.abs(_)).foreach(x => print(x + " "))
      println
    }
  }

  def solutionCheck(): Unit ={
    if(solverState==SolvingState.SAT && !correctSolution)
      throw new Exception("Internal error: incorrect or incomplete solution")
  }

  //https://biercoff.com/easily-measuring-code-execution-time-in-scala/
  private def time[R](block: => R, print: Boolean): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    if(print) println("c Solved in: " + (t1 - t0) + "ms")
    result
  }

  def setCancel(cancel : Boolean) ={
    this.isCancel = cancel
  }

  def getCancel() : Boolean ={
    this.isCancel
  }

  def getInitialClausules() : Int ={
    this.clauses.initialClauses.numClauses;
  }

  def setVsids(c : Boolean): Unit = {
    vsids = c;
  }

  def setVSIDSPropiety(v : VSIDSPropiety): Unit = {
    vsidsPropiety = v
  }

  def setHistorySolver(h: HistorySolver): Unit ={
    historySolver = h
  }

  def getVariablesBreakpoint: mutable.HashSet[Int] = varBreakpoints

  def setVariablesBreakpoint(v : mutable.HashSet[Int]): Unit = varBreakpoints = v

  def getAllVariablesBreakpoints: IndexedSeq[Pair[Int, Boolean]] = {
    if(numVariables != 0)
      for(i <- 1 to numVariables)
        yield new Pair(i, if (varBreakpoints.contains(i)) true else false)
    else
      IndexedSeq()
  }

  def addBreakpoints(variable: Int): Boolean ={
    varBreakpoints.add(variable)
  }

  def removeBreakpoints(variable: Int): Boolean ={
    varBreakpoints.remove(variable)
  }

  def isBreakpoint(variable: Int): Boolean = varBreakpoints.contains(variable)

  def removeAllBreakpoints(): Unit = {
    varBreakpoints = new mutable.HashSet[Int]()
    detectedVarBreakpoints = new mutable.Queue[Int]()
  }

  def addDetectedBreakpoint(variable: Int): Unit = {
    if(varBreakpoints.contains(variable))
      detectedVarBreakpoints += variable
  }

  def foundBreakpoint: Boolean = detectedVarBreakpoints.nonEmpty

  def getDetectedBreakpoints: mutable.Queue[Int] = detectedVarBreakpoints

  def numDetectedBreakpoints: Int = detectedVarBreakpoints.size

  def removeDetectedBreakpoints: Unit = {
    detectedVarBreakpoints = new mutable.Queue[Int]()
  }

}
