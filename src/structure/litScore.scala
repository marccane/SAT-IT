package structure

import util.decimalPrecision

class litScore(var lit : Int, var score : BigDecimal, var signe : Int) extends Ordered[litScore]{
  def setSign(s: Int): Unit = signe = s


  override def compare(that: litScore): Int = {
    if(score == that.score)
      if(that.lit == lit) 0
      else if(that.lit - lit < 0) -1
      else 1
    else if(score == that.score) 0
    else if(score - that.score < 0) -1
    else 1
  }

  override def toString: String = {
    lit + " : " +  score + " (" + (if(signe < 0) "-" else "+") + ")"
  }

  def toString(maxLengthLit : Int, maxLengthScore : Int): String = {
    var newLit = lit.toString
    var newScore = decimalPrecision.trunc(score,2).toString

    for(_ <- newLit.length until maxLengthLit) newLit = " ".concat(newLit)
    for(_ <- newScore.length until maxLengthScore) newScore = " ".concat(newScore)

    newLit + " : " +  newScore + " (" + (if(signe < 0) "-" else "+") + ")"
  }

  def getLiteral() : Int = lit

  def getScore() : BigDecimal = score

  def getLiteralSign() : Int = if(signe < 0) -lit else lit
}