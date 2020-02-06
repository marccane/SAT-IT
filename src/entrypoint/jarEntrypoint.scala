package entrypoint

import gui.MainGUI

object jarEntrypoint {

  var guiMode = true
  def main(args: Array[String]): Unit = {
    if(args.length==0)
      new MainGUI()
    else if(args.length==1) {
      if (args(0) == "-h" || args(0) == "--help")
        showHelp
      else
        new MainGUI(args(0))
    }
    else showHelp
  }

  def showHelp: Unit ={
    println("Usage: java -jar SAT-IT.jar [input-file]")
    println
    println("  where input-file is a DIMACS CNF file.")
  }

}
