package structure

import java.io.{File, FileNotFoundException}

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}

class Instance {

  val FORMAT_LINE = "p cnf number_of_variables number_of_clauses"
  val WARNING_FORMAT_LINE_START: Int => String = (l: Int) => "Invalid start character at line " + l
  val ERROR_CLAUSE_BEFORE_DECLATATION: Int => String = (l: Int) => "The clauses declared at line " + l + " must be declared after the line: " + FORMAT_LINE
  val WARNING_LINE_DEFINITION: Int => String = (l: Int) => "The CNF DIMACS definition line " + l + " must have this form: " + FORMAT_LINE
  val WARNING_CLAUSE_FORMAT: Int => String = (l: Int) => "The definition of the clause at the line "+ l +" must be: one_or_more_literal 0"
  val ERROR_NUM_CLAUSULES: (Int, Int) => String = (declared: Int, read: Int) => s"The amount of read clauses, $read, is different from the number of clauses declared, $declared"
  val ERROR_MAX_VARIABLES: Int => String = (l: Int) => "Variables must be declared from 1 to number of declared variables, at line " + l

  val SPACE: String  = "( |\\t)"
  val SPACES: String = SPACE + "+"
  val SPACES_OPT: String = SPACE + "*"
  val LITERAL: String = "(\\+|\\-)?" + SPACES_OPT + "[1-9][0-9]*"
  val ENDING_CLAUSE: String = "0" + SPACES_OPT + "$"
  val CLAUSE: String =  "^" + SPACES_OPT +  "(" + LITERAL + SPACES + ")" +  "+" + ENDING_CLAUSE
  val TOKEN_START_DEF_P = "p"
  val START_DEF: String = "^" + SPACES_OPT + TOKEN_START_DEF_P + SPACES + "cnf"
  val NUMBER =  "[0-9]+"

  var instance: Array[Array[Int]] = new Array[Array[Int]](0)
  var modelDefinition: Boolean = false
  var numVar: Int = -1
  var numClauses: Int = -1
  var errors = new ListBuffer[String]()
  var warnings = new ListBuffer[String]()

  def numErrors(): Int = errors.length
  def numWarnings(): Int = warnings.length
  def getErrorsText(): String = errors.foldLeft("")(_ + _ + "\n").dropRight(1)
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

  def correctLine(str: String): Boolean ={
    val it = str.iterator
    var correct = true
    var finished = false
    while(!finished && it.hasNext){
      val char = it.next()
      if(!Character.isWhitespace(char)){
        if(char != 'p' && char != 'c' && char != '-' && char != '+' && !Character.isDigit(char)){
          finished   = true
          correct = false
        } else
          finished = true
      }
    }
    correct
  }

  def lineWithoutInfo(str: String): Boolean ={
    val emptyLine       = ("^" + SPACES + "$").r
    val commentLine  = ("^" + SPACES_OPT + "c.*" + "$").r
    emptyLine.matches(str) || commentLine.matches(str)
  }

  def readDimacsAnalysis(bs: Source): Unit ={
    val liniaDef        = (START_DEF +  SPACES + NUMBER +  SPACES + NUMBER + SPACES_OPT + "$").r
    val clauseLine   = CLAUSE.r
    val numberDef = "[0-9]+".r
    val literalDef = LITERAL.r
    val liniaInitDef = ("^" + SPACES_OPT + TOKEN_START_DEF_P).r
    val patterns = List(liniaDef, clauseLine)

    var i = 1
    var numReadClauses = 0
    for(l <- bs.getLines()){
      if(l.length != 0) {
        if(correctLine(l)) {
          if(!lineWithoutInfo(l)){
            var found = false
            val it = patterns.iterator

            while (!found && it.hasNext) {
              val pattern = it.next()
              if (pattern.matches(l)) {
                found = true

                if(pattern == liniaDef && !modelDefinition){
                  val lSimpl =  numberDef.findAllIn(l.replaceAll(START_DEF + SPACES, "")).toList
                  if(lSimpl.size == 2) {
                    numVar = lSimpl.head.toInt
                    numClauses = lSimpl(1).toInt
                    modelDefinition = true
                    instance = new Array[Array[Int]](numClauses)
                  }
                } else if(pattern == clauseLine){
                  if(!modelDefinition)
                    errors += ERROR_CLAUSE_BEFORE_DECLATATION(i)
                  else{
                    val clausula: Array[Int] = literalDef.findAllIn(l.replaceAll(ENDING_CLAUSE, "")).map(x => x.replaceAll(SPACES, "").toInt).toArray
                    if(numReadClauses < numClauses) instance(numReadClauses) = clausula
                    for(variable <- clausula){
                      if(numVar < math.abs(variable))
                        errors += ERROR_MAX_VARIABLES(i)
                    }
                    numReadClauses = numReadClauses + 1
                  }
                }
              }
            }
            if (!found){
              if(liniaInitDef.findAllIn(l).nonEmpty)
                warnings += WARNING_LINE_DEFINITION(i)
              else
                warnings += WARNING_CLAUSE_FORMAT(i)
            }
          }
        }
        else
          warnings.append(WARNING_FORMAT_LINE_START(i) + " " + l)
      }
      i = i + 1
    }

    if(numClauses != numReadClauses)
      errors += ERROR_NUM_CLAUSULES(numClauses, numReadClauses)

    if(errors.nonEmpty) {
      instance = new Array[Array[Int]](0)
      numVar = 0
    }
  }

}
