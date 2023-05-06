package entrypoint

import solver.{Backtracking, CDCL, DPLL, Solver}
import structure.Instance
import java.io.{BufferedWriter, File, FileWriter}

object generatorTestEntrypoint {

  def main(args: Array[String]): Unit = {
    solveAll(System.getProperty("user.dir") + File.separator + "cnf", 1500)
  }

  //Obté tots els fitxers, recursivament, de path
  def obtenirTotsFitxers(path: String): Array[File] ={
    var fitxersResultants: Array[File] = Array();
    for(f <- new File(path).listFiles()){
      if(f.isFile && f.getName.endsWith(".cnf")){
        fitxersResultants = fitxersResultants :+ f
      } else if(f.isDirectory){
        fitxersResultants = fitxersResultants.concat(obtenirTotsFitxers(f.getPath))
      }
    }
    fitxersResultants
  }

  // Fa timeout a f
  //https://stackoverflow.com/questions/51790448/get-partial-result-on-scala-time-limited-best-effort-computation
  @throws(classOf[java.util.concurrent.TimeoutException])
  def timedRun[F](timeout: Long)(f: => F): F = {
    import java.util.concurrent.{Callable, FutureTask, TimeUnit}
    val task = new FutureTask(new Callable[F]() {
      def call() = f
    })

    new Thread(task).start()
    task.get(timeout, TimeUnit.MILLISECONDS)
  }


  def solveAll(carpeta: String, timeout: Long): Unit = {
    val cnfFiles = obtenirTotsFitxers(carpeta)

    for (solver_type <- List("CDCL", "DPLL","BK")) {

      val file = new File(java.nio.file.Paths.get(".", "test_cnf", s"${solver_type}.test").toString)
      if(file.exists()){
        file.delete()
      }
      val bw = new BufferedWriter(new FileWriter(file, true))

      for(file <- cnfFiles){
        println(s"${solver_type} : ${file.getName}")
        try {
          timedRun(timeout) {
            val instance = new Instance
            instance.readDimacs(file)

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

            solver.solve(instance)
            val statics = solver.getStatics()
            val content = s".${file.getPath.substring(System.getProperty("user.dir").length)};${solver_type};${statics._1.toString};${statics._2._1};${statics._2._2};${statics._2._3};${statics._3.mkString(",")}"
            bw.append(content + System.lineSeparator())
          }
        }
        catch{
          case e: Exception => println(s"${solver_type} : ${file.getName} (no solution)")
        }
      }
      bw.close()
    }
  }
}
