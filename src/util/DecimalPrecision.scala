package util

import scala.math.BigDecimal.RoundingMode

object DecimalPrecision {

  def trunc(num: Double, numD : Int): Double ={
    if(numD < 0)
      num
    else{
      val div = Math.pow(10, numD)
      if(num < 0)
        -Math.floor(Math.abs(num)*div)/div
      else
        Math.floor(num*div)/div
    }
  }

  def trunc(num: BigDecimal, numD : Int): BigDecimal ={
    num.setScale(2, RoundingMode.DOWN)
  }

}
