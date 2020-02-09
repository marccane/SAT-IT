package entrypoint

import java.io.File

import solver.{CDCL, Backtracking, DPLL, Solver}
import structure.Instance

object testEntrypoint {

  val runAll = true
  val mostrarEvents = false
  //val solver = new DPLL
  val solver = new CDCL
  //val solver = new Backtracking

  def main(args: Array[String]): Unit = {

    if(runAll) {
      solveAll("/cnf/easy")
      solveAll("/cnf/normal")
      solveAll("/cnf/hard")
    }
    else{
      val dimacs = new Instance
      //dimacs.readDimacs("cnf/hard/NQueens25quad.cnf")
      //dimacs.readDimacs("cnf/random_ksat.cnf")
      //dimacs.readDimacs("cnf/hole6_d15.cnf")
      //dimacs.readDimacs("cnf/easy/ex5.cnf")
      //dimacs.readDimacs("cnf/hanoi4.cnf")
      //dimacs.readDimacs("cnf/NQueens10quad.cnf")
      //dimacs.readDimacs("cnf/hard/php10-15.cnf")
      //dimacs.readDimacs("cnf/test.cnf")
      println("Fitxer llegit")
      println()

      jarEntrypoint.solvePrint(dimacs, solver, mostrarEvents)
    }
  }

  def solveAll(carpeta: String): Unit = {
    val cnfFiles = new File(System.getProperty("user.dir") + carpeta).listFiles()
      .filter(x => x.isFile && x.getName.endsWith(".cnf")).sortBy(_.length())

    val instance = new Instance
    for (file <- cnfFiles) {
      println("Executant " + file.getName)
      instance.readDimacs(file)
      try{
        jarEntrypoint.solvePrint(instance, solver, mostrarEvents)
      }
      catch{
        case e: Exception => e.printStackTrace()
      }
    }
  }

}
