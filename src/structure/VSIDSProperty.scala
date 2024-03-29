package structure

class VSIDSProperty(var startScore : BigDecimal, var addScore : BigDecimal, var productScore : BigDecimal) {

  val initialAddScore: BigDecimal = addScore

  def getStartScore: BigDecimal = startScore
  def getAddScore: BigDecimal = addScore
  def getProductScore: BigDecimal = productScore
  def setAddScore(newAddScore: BigDecimal): Unit ={
    addScore = newAddScore
  }
  def getInitialAddScore: BigDecimal = initialAddScore
  def resetAddScore(): Unit ={
    addScore = initialAddScore
  }
}
