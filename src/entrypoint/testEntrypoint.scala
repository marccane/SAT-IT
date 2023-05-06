package entrypoint

import java.io.File
import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.Instance

object testEntrypoint {

  def main(args: Array[String]): Unit = {
    solveAll("test_cnf")
  }

  def solveAll(carpeta: String): Unit = {
    val testFiles = new File(System.getProperty("user.dir") + java.io.File.separator + carpeta).listFiles()
      .filter(x => x.isFile && x.getName.endsWith(".test")).sortBy(_.length())

    for (testFile <- testFiles) {

      val lines = readFile(testFile.getPath)
      for (line <- lines; if line != "") {

        val Array(filename, solver_type, solvingState, numDecisions, numConflicts, numPropagations, trailStr) = line.split(';').map(_.trim)
        println(filename)

        var solver: Solver = null;
        if (solver_type == "DPLL") {
          solver = new DPLL
        }
        else if (solver_type == "BK") {
          solver = new Backtracking
        }
        else {
          solver = new CDCL
        }

        if(new File(testFile.getPath).exists) {

          try {
            val instance = new Instance
            instance.readDimacs(filename)
            solver.solve(instance)
            val statics = solver.getStatics()
            val content = s"${filename};${solver_type};${statics._1.toString};${statics._2._1};${statics._2._2};${statics._2._3};${statics._3.mkString(",")}"

            if (line != content) {
              println(s"The solution for the file ${filename} was expected:")
              println(line)
              println(s"Found:${content}")
            }
          }
          catch {
            case e: Exception => e.printStackTrace()
          }
        } else {
          println(s"The file ${filename} was not found")
        }
      }
    }
  }

  def readFile(filename: String): Seq[String] = {
    val bufferedSource = io.Source.fromFile(filename)
    val lines = (for (line <- bufferedSource.getLines()) yield line).toList
    bufferedSource.close
    lines
  }

}
