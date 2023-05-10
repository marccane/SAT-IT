package entrypoint

import java.io.File

import gui.{MainGUI, VsidsOptionsWindow}
import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.{Instance, VSIDSPropiety}
import util.Constants

object jarEntrypoint {

  var regexDouble: String = "(\\+|-)?[0-9]+(,[0-9]+)?"

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
      Console.err.println("Error: incorrect syntax")
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
    var errorVSIDS = false
    var filename: String = null
    var solver: Solver = new CDCL
    var events = false
    val vsidsPropietyDefault: VSIDSPropiety = new VSIDSPropiety(Constants.INITIAL_SCORE_VALUE, Constants.BONUS_SCORE_VALUE,Constants.INCREMENTED_BONUS_CONSTANT)

    if(argc == 1){
      error = true
      Console.err.println("Error: file is required for CLI mode")
      println
      showHelp
    }
    else if(!new java.io.File(args(1)).exists){ //assumim argc > 1
      error = true
      Console.err.println("Error: file " + args(1) + " doesn't exist")
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
          else if (actualArg == "-cdcl_VSIDS"){
            solver = new CDCL
            solver.setVsids(true)

            if(i + 1 == argc)
              solver.setVSIDSPropiety(vsidsPropietyDefault)
            else{
              var param: List[Double] = List[Double]()
              //Mirem si ens han entrat els tres paràmetres
              if(argc <= i + 3)
                errorVSIDS = true
              else{
                //Mirem si complexien han entrat els valors correctes
                val aux = i
                for(j <- 1 until 4; if !error){
                  val value = args(aux + j)
                  if(value.replaceAll(regexDouble,"") != "") {
                    error = true
                  } else {
                    param :+= value.replaceAll(",","\\.").toDouble
                  }
                  i = i + 1
                }
              }
              if(!error && !errorVSIDS)
                solver.setVSIDSPropiety(new VSIDSPropiety(param.head, param(1), param(2)))
              else
                solver.setVSIDSPropiety(vsidsPropietyDefault)
            }
          }
          else if (actualArg == "-cdcl") {
            solver = new CDCL
            solver.setVsids(false)
          } else if (actualArg == "-events")
            events = true
          else error = true

          i += 1
        }

        if(errorVSIDS)
          Console.err.println("Error: Wrong number of VSIDS parameters" )
        else if(error) {
          Console.err.println("Error: unknown parameter '" + args(i-1) + "'")
        }
      }
    }
    else{
      error = true
      Console.err.println("Error: unknown file error")
    }

    if(!error){
      val instance = new Instance
      try {
        instance.readDimacs(filename)
        if(instance.numWarnings() != 0)
          println("Warinings:\n" + instance.getWarningsText())
        if(instance.numErrors() == 0)
          solvePrint(instance, solver, events)
        else
          println("Errors:\n" + instance.getErrossText())
      }
      catch{
        case _: NumberFormatException   => Console.err.println("Error: Invalid file format. Please select a DIMACS CNF file")
        case e: Exception               => Console.err.println("Error:  " + e.getMessage);
      }
    }
  }


  //No obrim la gui si el fitxer que ens passen no existeix (si el fitxer te un format erroni si que s'obre...)
  def handleGUIArguments(args: Array[String]): Unit ={
    if(!new java.io.File(args(0)).exists){
      Console.err.println("Error: file " + args(0) + " doesn't exist")
    }
    else if(new java.io.File(args(0)).isFile){
      new MainGUI(args(0))
    }
    else{
      Console.err.println("Error: unknown file error")
    }
  }

  def solvePrint(instance: Instance, solver: Solver, mostrarEvents: Boolean = false): Unit ={
    val solved = solver.solve(instance)
    println(if(solved)"s SATISFIABLE" else "s UNSATISFIABLE")
    if(mostrarEvents) solver.printEvents(true)
    try {
      solver.solutionCheck()
      solver.printSolution()
    }
    catch{
      case e: Exception => Console.err.println(e.getMessage)
    }
  }

  def showHelp: Unit ={

    var vsidsName : Array[String] = Array(VsidsOptionsWindow.titleInitialValue, VsidsOptionsWindow.titleIncrementValue,
      VsidsOptionsWindow.titleProductValue)
    val maxLength = vsidsName.maxBy(_.length).length

    var regexInput = ": " + regexDouble + "\n"
    println("Usage:")
    println("   GUI mode:  java -jar SAT-IT.jar [input-file]")
    println("   CLI mode:  java -jar SAT-IT.jar -cli <input-file> [CLI-options]")
    println
    println("  where input-file is a DIMACS CNF file.")
    println
    println("CLI-options:")
    println
    println("  -bt         use the backtracking solver")
    println("  -dpll       use the DPLL solver")
    println("  -cdcl       use the CDCL solver (default)")
    println("  -cdcl_VSIDS use the CDCL solver with VSIDS (Variable State Independent Decaying Sum). The default values of VSIDS are:\n" +
      "              " + vsidsName(0) +  getWhiteSpace(vsidsName(0), maxLength) + ": " + Constants.INITIAL_SCORE_VALUE.toString.replaceAll("\\.",",") + "\n" +
      "              " + vsidsName(1) +  getWhiteSpace(vsidsName(1), maxLength) + ": " + Constants.BONUS_SCORE_VALUE.toString.replaceAll("\\.",",") + "\n"  +
      "              " + vsidsName(2) +  getWhiteSpace(vsidsName(2), maxLength) + ": " + Constants.INCREMENTED_BONUS_CONSTANT.toString.replaceAll("\\.",","))

    println("  -cdcl_VSIDS " + "<" + allName(vsidsName) + ">\n" +
      "              use the CDCL solver with VSIDS parametres. Remember that the types of parameters are: \n" +
      "              " + vsidsName(0) + getWhiteSpace(vsidsName(0), maxLength) +  regexInput +
      "              " + vsidsName(1) + getWhiteSpace(vsidsName(1), maxLength) +  regexInput +
      "              " + vsidsName(2) + getWhiteSpace(vsidsName(2), maxLength) +  regexInput )
    println("  -events     print all solver steps on exit")
    println
    println("If multiple solvers are selected the last one will be used.")
    //println("  -h, --help  Show help message")
  }

  def getWhiteSpace(name : String, max: Int) : String = {
    var string = ""
    for(i <- name.length to max)
      string = string.concat(" ")
    string
  }
  def allName(names: Array[String]) : String ={
    var string = ""
    var separador = " "
    for(s <- names) {
      if(names.last.equals(s))
        separador = ""
      string = string.concat(s.replaceAll("\\s","") + separador)
    }
    string
  }


}
