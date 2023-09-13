package entrypoint

import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.Instance
import java.io.{BufferedWriter, File, FileWriter}

object generatorTestEntrypoint {

  def main(args: Array[String]): Unit = {
    solveAll(System.getProperty("user.dir") + File.separator + "cnf")
  }

  //Obté tots els fitxers, recursivament, de path
  def getAllCnfFilesFromPath(path: String): Array[File] ={
    var fitxersResultants: Array[File] = Array()
    for(f <- new File(path).listFiles()){
      if(f.isFile && f.getName.endsWith(".cnf")){
        fitxersResultants = fitxersResultants :+ f
      } else if(f.isDirectory){
        fitxersResultants = fitxersResultants.concat(getAllCnfFilesFromPath(f.getPath))
      }
    }
    fitxersResultants
  }

  def solveAll(cnfFolderPath: String): Unit = {
    val cnfEasyFiles = getAllCnfFilesFromPath(cnfFolderPath + File.separator + "easy")
    val cnfNormalFiles = getAllCnfFilesFromPath(cnfFolderPath + File.separator + "normal")
    val cnfHardFiles = getAllCnfFilesFromPath(cnfFolderPath + File.separator + "hard")

    val cnfForBK = cnfEasyFiles
    val cnfForDPLL = cnfEasyFiles ++ cnfNormalFiles
    val cnfForCDCL = cnfEasyFiles ++ cnfNormalFiles ++ cnfHardFiles

    for ((solver_type, filesForSolver) <- List(("CDCL", cnfForCDCL), ("DPLL", cnfForDPLL),("BK", cnfForBK))) {

      val file = new File(java.nio.file.Paths.get(".", "test_cnf", s"$solver_type.test").toString)
      if(file.exists()){
        file.delete()
      }
      val bw = new BufferedWriter(new FileWriter(file, true))

      for(file <- filesForSolver){
        println(s"$solver_type : ${file.getName}")
        val instance = new Instance
        instance.readDimacs(file)

        var solver: Solver = null
        if (solver_type == "DPLL") {
          solver = new DPLL
        }
        else if (solver_type == "BK") {
          solver = new Backtracking
        }
        else {
          solver = new CDCL
        }

        solver.solve(instance)
        val statics = solver.getStatistics()
        val content = s".${file.getPath.substring(System.getProperty("user.dir").length)};$solver_type;${statics._1.toString};${statics._2._1};${statics._2._2};${statics._2._3};${statics._3.mkString(",")}"
        bw.append(content + System.lineSeparator())
      }
      bw.close()
    }
  }
}
