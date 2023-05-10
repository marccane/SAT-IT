package structure

import java.io.{File, FileNotFoundException}

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}

class Instance {

  val FORMAT_LINE = "p cnf number_of_variables number_of_clauses"
  val WARNING_FORMAT_LINE_START: Int => String = (l: Int) => "Invalid start character at line " + l
  val ERROR_CLAUSULA_BEFORE_DECLATATION: Int => String = (l: Int) => "The clauses declared at line " + l + " must be declared after the line: " + FORMAT_LINE
  val WARNING_LINE_DEFINITION: Int => String = (l: Int) => "The CNF DIMACS definition line " + l + " must have this form: " + FORMAT_LINE
  val WARNING_CLAUSULA_FORMAT: Int => String = (l: Int) => "The definition of the clause at the line "+ l +" must be: one_or_more_literal 0"
  val ERROR_NUM_CLAUSULES: (Int, Int) => String = (declarades: Int, llegides: Int) => s"The amount of read clauses, $llegides, is different from the number of clauses declared, $declarades"
  val ERROR_MAX_VARIABLES: Int => String = (l: Int) => "Variables must be declared from 1 to number of declared variables, at line " + l

  val ESPAI: String  = "( |\\t)"
  val ESPAIS: String = ESPAI + "+"
  val ESPAIS_OPT: String = ESPAI + "*"
  val LITERAL: String = "(\\+|\\-)?" + ESPAIS_OPT + "[1-9][0-9]*"
  val FINAL_CLAUSULA: String = "0" + ESPAIS_OPT + "$"
  val CLAUSULA: String =  "^" + ESPAIS_OPT +  "(" + LITERAL + ESPAIS + ")" +  "+" + FINAL_CLAUSULA
  val TOKEN_INICI_DEF_P = "p"
  val INICI_DEF: String = "^" + ESPAIS_OPT + TOKEN_INICI_DEF_P + ESPAIS + "cnf"
  val NUMBER =  "[0-9]+"

  var instance: Array[Array[Int]] = new Array[Array[Int]](0)
  var definicioModel: Boolean = false
  var numVar: Int = -1
  var numClausules: Int = -1
  var errors = new ListBuffer[String]()
  var warnings = new ListBuffer[String]()

  def numErrors(): Int = errors.length
  def numWarnings(): Int = warnings.length
  def getErrossText(): String = errors.foldLeft("")(_ + _ + "\n").dropRight(1)
  def getWarningsText(): String = warnings.foldLeft("")(_ + _ + "\n").dropRight(1)

  //forcem les checked exceptions perque si no java no s'entera
  @throws(classOf[FileNotFoundException])
  @throws(classOf[NumberFormatException])
  def readDimacs(filename: String): Unit ={
    readDimacsAnalysis(scala.io.Source.fromFile(filename))
  }

  @throws(classOf[FileNotFoundException])
  @throws(classOf[NumberFormatException])
  def readDimacs(file: File): Unit ={
    readDimacsAnalysis(scala.io.Source.fromFile(file))
  }

  @throws(classOf[FileNotFoundException])
  @throws(classOf[NumberFormatException])
  def readDimacsSecond(file: File): Unit ={
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
    if(numVar == -1) 0
    else numVar
  }

  def liniaCorrecta(str: String): Boolean ={
    var it = str.iterator
    var correcta = true
    var acabat = false
    while(!acabat && it.hasNext){
      var char = it.next()
      if(!Character.isWhitespace(char)){
        if(char != 'p' && char != 'c' && char != '-' && char != '+' && !Character.isDigit(char)){
          acabat   = true
          correcta = false
        } else
          acabat = true
      }
    }
    correcta
  }

  def liniaSenseInfo(str: String): Boolean ={
    val liniBuida       = ("^" + ESPAIS + "$").r
    val liniaComentari  = ("^" + ESPAIS_OPT + "c.*" + "$").r
    liniBuida.matches(str) || liniaComentari.matches(str)
  }

  def readDimacsAnalysis(bs: Source): Unit ={
    val liniaDef        = (INICI_DEF +  ESPAIS + NUMBER +  ESPAIS + NUMBER + ESPAIS_OPT + "$").r
    val liniaClausula   = CLAUSULA.r
    val numberDef = "[0-9]+".r
    val literalDef = LITERAL.r
    val liniaInitDef = ("^" + ESPAIS_OPT + TOKEN_INICI_DEF_P).r
    val patterns = List(liniaDef, liniaClausula)

    var i = 1
    var clausulasLlegides = 0
    for(l <- bs.getLines()){
      if(l.length != 0) {
        if(liniaCorrecta(l)) {
          if(!liniaSenseInfo(l)){
            var trobat = false
            val it = patterns.iterator

            while (!trobat && it.hasNext) {
              val pattern = it.next()
              if (pattern.matches(l)) {
                trobat = true

                if(pattern == liniaDef && !definicioModel){
                  val lSimpl =  numberDef.findAllIn(l.replaceAll(INICI_DEF + ESPAIS, "")).toList
                  if(lSimpl.size == 2) {
                    numVar = lSimpl.head.toInt
                    numClausules = lSimpl(1).toInt
                    definicioModel = true
                    instance = new Array[Array[Int]](numClausules)
                  }
                } else if(pattern == liniaClausula){
                  if(!definicioModel)
                    errors += ERROR_CLAUSULA_BEFORE_DECLATATION(i)
                  else{
                    val clausula: Array[Int] = literalDef.findAllIn(l.replaceAll(FINAL_CLAUSULA, "")).map(x => x.replaceAll(ESPAIS, "").toInt).toArray
                    if(clausulasLlegides < numClausules) instance(clausulasLlegides) = clausula
                    for(variable <- clausula){
                      if(numVar < math.abs(variable))
                        errors += ERROR_MAX_VARIABLES(i)
                    }
                    clausulasLlegides = clausulasLlegides + 1
                  }
                }
              }
            }
            if (!trobat){
              if(liniaInitDef.findAllIn(l).nonEmpty)
                warnings += WARNING_LINE_DEFINITION(i)
              else
                warnings += WARNING_CLAUSULA_FORMAT(i)
            }
          }
        }
        else
          warnings.append(WARNING_FORMAT_LINE_START(i) + " " + l)
      }
      i = i + 1
    }

    if(numClausules != clausulasLlegides)
      errors += ERROR_NUM_CLAUSULES(numClausules, clausulasLlegides)

    if(errors.nonEmpty) {
      instance = new Array[Array[Int]](0)
      numVar = 0
    }
  }

}
