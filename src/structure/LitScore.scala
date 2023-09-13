package structure

import util.DecimalPrecision

class LitScore(var lit : Int, var score : BigDecimal, var sign : Int) extends Ordered[LitScore]{
  def setSign(s: Int): Unit = sign = s

  override def compare(that: LitScore): Int = {
    if(score == that.score)
      if(that.lit == lit) 0
      else if(that.lit - lit < 0) -1
      else 1
    else if(score == that.score) 0
    else if(score - that.score < 0) -1
    else 1
  }

  override def toString: String = {
    lit + " : " +  score + " (" + (if(sign < 0) "-" else "+") + ")"
  }

  def toString(maxLengthLit : Int, maxLengthScore : Int): String = {
    var newLit = lit.toString
    var newScore = DecimalPrecision.trunc(score,2).toString

    for(_ <- newLit.length until maxLengthLit) newLit = " ".concat(newLit)
    for(_ <- newScore.length until maxLengthScore) newScore = " ".concat(newScore)

    newLit + " : " +  newScore + " (" + (if(sign < 0) "-" else "+") + ")"
  }

  def getLiteral() : Int = lit

  def getScore() : BigDecimal = score

  def getLiteralSign() : Int = if(sign < 0) -lit else lit
}