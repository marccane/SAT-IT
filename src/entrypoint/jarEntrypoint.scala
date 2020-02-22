package entrypoint

import gui.MainGUI
import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.Instance

object jarEntrypoint {

  def main(args: Array[String]): Unit = {
    val argc = args.length
    if(argc == 0) //GUI mode sense fitxer
      new MainGUI()
    else if(args(0) == "-cli" || args(0) == "-c"){ //CLI mode
      handleCLIArguments(args)
    }
    else if(args(0) == "-h" || args(0) == "--help")
      showHelp
    else if(argc > 1 || args(0).startsWith("-")){ //seria raro que volguessin executar la GUI amb un fitxer que comenca per '-'
      Console.err.println("ERROR: incorrect syntax")
      println
      showHelp
    }
    else{ //GUI mode amb fitxer
      handleGUIArguments(args)
    }
  }

  def handleCLIArguments(args: Array[String]): Unit ={
    val argc = args.length
    var error = false
    var filename: String = null
    var solver: Solver = new CDCL
    var events = false

    if(argc == 1){
      error = true
      Console.err.println("ERROR: file is required for CLI mode")
      println
      showHelp
    }
    else if(!new java.io.File(args(1)).exists){ //assumim argc > 1
      error = true
      Console.err.println("ERROR: file " + args(1) + " doesn't exist")
    }
    else if(new java.io.File(args(1)).isFile){
      filename = args(1)
      if(argc > 2){
        var i = 2
        while (i < args.length && !error) {
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

        if(error) {
          Console.err.println("ERROR: unknown parameter '" + args(i-1) + "'")
        }
      }
    }
    else{
      error = true
      Console.err.println("ERROR: unknown file error")
    }

    if(!error){
      val instance = new Instance
      instance.readDimacs(filename)
      solvePrint(instance, solver, events)
    }
  }

  def handleGUIArguments(args: Array[String]): Unit ={
    if(!new java.io.File(args(0)).exists){
      Console.err.println("ERROR: file " + args(0) + " doesn't exist")
    }
    else if(new java.io.File(args(0)).isFile){
      new MainGUI(args(0))
    }
    else{
      Console.err.println("ERROR: unknown file error")
    }
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
    println("Usage:")
    println("   -GUI mode:  java -jar SAT-IT.jar [input-file]")
    println("   -CLI mode:  java -jar SAT-IT.jar -cli <input-file> [CLI-options]")
    println
    println("  where input-file is a DIMACS CNF file.")
    println
    println("CLI-options:")
    println
    println("  -bt         use the backtracking solver")
    println("  -dpll       use the DPLL solver")
    println("  -cdcl       use the CDCL solver (default)")
    println("  -events     print all solver steps on exit")
    println
    println("If multiple solvers are selected the last one will be used.")
    //println("  -h, --help  Show help message")
  }

}
