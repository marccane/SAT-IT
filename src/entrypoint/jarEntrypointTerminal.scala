package entrypoint

import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.Instance

object jarEntrypointTerminal {

  def main(args: Array[String]): Unit = {
    if(args.length>0) {
      if (args(0) == "-h" || args(0) == "--help")
        showHelp
      else {

        val filename = args(0)
        var events = false
        var solver: Solver = new CDCL
        var error = false

        var i = 1
        while (i < args.length) {
          val actualArg = args(i)
          if (actualArg == "-bt")
            solver = new Backtracking
          else if (actualArg == "-dpll")
            solver = new DPLL
          else if (actualArg == "-cdcl")
            solver = new CDCL
          else if (actualArg == "-events")
            events = true
          else error = true

          i += 1
        }

        if(error)
          showHelp
        else {
          val instance = new Instance
          instance.readDimacs(filename)
          solvePrint(instance, solver, events)
        }
      }
    }
    else showHelp
  }

  def solvePrint(instance: Instance, solver: Solver, mostrarEvents: Boolean = false): Unit ={
    val solved = solver.solve(instance)
    print("s ")
    println(if(solved)"SATISFIABLE" else "UNSATISFIABLE")
    if(mostrarEvents) solver.printEvents(true)
    solver.printSolution()
    solver.solutionCheck()
  }

  def showHelp: Unit ={
    println("Usage: java -jar SAT-IT-terminal.jar <input-file> [options]")
    println
    println("  where input-file is a DIMACS CNF file.")
    println
    println("OPTIONS:")
    println
    println("  -bt         Backtracking solver")
    println("  -dpll       DPLL solver")
    println("  -cdcl       CDCL solver (default)")
    println
    println("  -events     Show all the solver steps when finished")
    println("  -h, --help  Show help message")
  }

}
