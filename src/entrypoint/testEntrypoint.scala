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
    var results: List[(Int, Int)] = List()
    for (testFile <- testFiles) {
      var errors = 0
      var warning = 0

      val lines = readFile(testFile.getPath)
      for (line <- lines; if line != "") {

        val Array(filename, solver_type, solvingState, numDecisions, numConflicts, numPropagations, trailStr) = line.split(';').map(_.trim)

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
            solver.solve(instance, false)
            val statics = solver.getStatics()
            val content = s"${filename};${solver_type};${statics._1.toString};${statics._2._1};${statics._2._2};${statics._2._3};${statics._3.mkString(",")}"

            if (line != content) {
              var error = 0
              if(solvingState != statics._1.toString ){
                println(s"The state of solution for the file ${filename} with ${solver_type} was ${solvingState} and found ${statics._1}")
                error = 1
              }
              if (trailStr != statics._3.mkString(",")){
                println(s"The trail of solution for the file ${filename} with ${solver_type} was ${trailStr} and found ${statics._3.mkString(",")}")
                error = 1
              }
              if(numDecisions != statics._2._1.toString || numConflicts != statics._2._2.toString || numPropagations != statics._2._3.toString){
                warning += 1
                println(s"The decisions or conflicts or propagations for the file ${filename} with ${solver_type} and solver ${solver_type} was ${numDecisions} ${numConflicts} ${numPropagations} and found ${statics._2._1} ${statics._2._2} ${statics._2._3}")
              }
              errors += error
            }
          }
          catch {
            case e: Exception => {
              e.printStackTrace()
              errors += 1
            }
          }
        } else {
          println(s"The file ${filename} was not found")
        }
      }
      results = results.appended((warning, errors))
    }
    for (file_result <- testFiles zip results){
      println(s"${file_result._1}: warnings = ${file_result._2._1}, errors = ${file_result._2._2}")
    }
  }

  def readFile(filename: String): Seq[String] = {
    val bufferedSource = io.Source.fromFile(filename)
    val lines = (for (line <- bufferedSource.getLines()) yield line).toList
    bufferedSource.close
    lines
  }

}
