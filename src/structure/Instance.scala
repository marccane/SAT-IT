package structure

import java.io.{File, FileNotFoundException}

import scala.io.BufferedSource

class Instance {

  var instance: Array[Array[Int]] = new Array[Array[Int]](0)

  //forcem les checked exceptions perque si no java no s'entera
  @throws(classOf[FileNotFoundException])
  @throws(classOf[NumberFormatException])
  def readDimacs(filename: String): Unit ={
    instance = readDimacsInternal(scala.io.Source.fromFile(filename))
  }

  @throws(classOf[FileNotFoundException])
  @throws(classOf[NumberFormatException])
  def readDimacs(file: File): Unit ={
    instance = readDimacsInternal(scala.io.Source.fromFile(file))
  }

  private def readDimacsInternal(bs: BufferedSource): Array[Array[Int]] ={
    bs.getLines
      .filter(_.length>0)
      .filterNot(x => x.charAt(0)=='p' || x.charAt(0)=='c')
      .map(_.replace('\t', ' '))
      .map(_.trim.replaceAll(" +"," "))
      .map(_.split(" "))
      .map(_.map(Integer.parseInt))
      .map(_.filter(_!=0))
      .toArray
  }

  def numVariables: Int = {
    val maxAbs = (x: Int, y: Int) => math.max(math.abs(x), math.abs(y))
    instance.map(_.fold(0)(maxAbs)).fold(0)(maxAbs)
  }

}
