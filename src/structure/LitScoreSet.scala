package structure

import javax.swing.DefaultListModel
import scala.collection.mutable
import util.DecimalPrecision

class LitScoreSet {

  //Priority Queue
  var litScoreQueue : mutable.PriorityQueue[LitScore] = _
  var literalScoreAssigment : mutable.HashMap[Int, (BigDecimal, Int)] = _
  var maxScore : BigDecimal = _
  var maxLengthScore : Int = _
  var maxLengthLit : Int = _
  var vsidsProperty : VSIDSProperty = _
  var listModel : DefaultListModel[LitScore] = _
  var literalIndex : java.util.HashMap[Integer, Integer] = _
  val MAX_BOUND: BigDecimal = 10E100
  val RESCALE_MAX_BOUND: BigDecimal = 10E-100

  //Indica si s'ha escollit el literal negatiu, positiu o cap
  val LITERAL_POSITIU : Int = 1
  val LITERAL_NEGATIU : Int  = -1

  //Get
  def getOrderedLitScore:Seq[LitScore] ={
    litScoreQueue.clone().dequeueAll
  }

  def getOrderedLitScoreList: Array[LitScore] ={
    litScoreQueue.clone().dequeueAll.toArray
  }

  def numVar: Int = {
    if(literalScoreAssigment == null) 0 else literalScoreAssigment.size
  }

  //set
  def setDefaultListModel(defaultListModel: DefaultListModel[LitScore]): Unit = listModel = defaultListModel
  def setMapLiteralIndex(map : java.util.HashMap[Integer, Integer]): Unit = literalIndex = map

  //Initial method
  def initScores(clausesArg: Array[Array[Int]], numVariables: Int, property: VSIDSProperty) : Unit ={
    //Inicialitzem les propietats
    vsidsProperty = property
    maxScore = Double.MinValue
    maxLengthScore = 0
    maxLengthLit = 0

    //Estructura per guardar cada literal els scores
    literalScoreAssigment = new mutable.HashMap(numVariables,0.75)

    //Estructura auxiliar per si hi ha literals que no apareixen les clausules
    var present: mutable.HashSet[Int] = new mutable.HashSet()
    present.addAll(List.range(1, numVariables + 1))


    var maxBound = false
    //Recorrem totes les clàusules i calculem les puntuacions
    for(c <- clausesArg){
      for(l <- c){
        present -= math.abs(l)
        var scoreAssigment = literalScoreAssigment.getOrElse( math.abs(l), null)

        if(scoreAssigment == null)
          scoreAssigment = (vsidsProperty.getStartScore,LITERAL_NEGATIU)
        else
          scoreAssigment = (scoreAssigment._1 + vsidsProperty.getStartScore, scoreAssigment._2)

        literalScoreAssigment.put(math.abs(l), scoreAssigment)
        if(MAX_BOUND <= scoreAssigment._1) maxBound = true
        if(maxScore == Double.MinValue || maxScore < scoreAssigment._1) maxScore = scoreAssigment._1
        if(maxLengthLit < math.abs(l).toString.length) maxLengthLit = math.abs(l).toString.length
      }
    }
    val score = 0.0
    for(p <- present){
      literalScoreAssigment.put(p,(score,LITERAL_NEGATIU))
      if(maxScore == Double.MinValue || maxScore < score) maxScore = score
      if(maxLengthLit < p.toString.length) maxLengthLit = p.toString.length
    }

    if(maxBound) reset()

    //Guardem els valors obtinguts
    updateQueue()
  }

  def reset(): Unit = {
    maxLengthScore = 0
    vsidsProperty.setAddScore(vsidsProperty.getAddScore*RESCALE_MAX_BOUND)
    for((l, sa) <- literalScoreAssigment.clone()) {
      val rescaleValue = sa._1 * RESCALE_MAX_BOUND
      literalScoreAssigment.put(l, (rescaleValue, sa._2))
    }
  }


  def updateQueue(): Unit = {
    litScoreQueue = mutable.PriorityQueue.empty[LitScore]
    maxLengthScore = 0

    for((l,sa) <- literalScoreAssigment){
      litScoreQueue.enqueue(new LitScore(l, sa._1, sa._2))
      val candidat = DecimalPrecision.trunc(sa._1,2).toString.length
      if(maxLengthScore < candidat) maxLengthScore = candidat
    }

    if(listModel != null){
      listModel.clear()
      var i = 0
      for(ls <- getOrderedLitScore){
        listModel.addElement(ls)
        literalIndex.put(ls.getLiteral(), i)
        i = i + 1
      }
    }
  }



  //Update new clauses
  def addNewClausule(newClause: Clause) : Unit = {

    var maxBound = false
    //Update scores
    for(l <- newClause.getClauseArray) {
      var scoreAssigment = literalScoreAssigment.getOrElse(math.abs(l), null)
      if(scoreAssigment != null){
        scoreAssigment = (scoreAssigment._1 + vsidsProperty.getAddScore, scoreAssigment._2)
        literalScoreAssigment.put(math.abs(l), scoreAssigment)
        if(MAX_BOUND <= scoreAssigment._1) maxBound = true
        if(maxScore < scoreAssigment._1) maxScore = scoreAssigment._1
      }
    }
    vsidsProperty.setAddScore(vsidsProperty.getAddScore*vsidsProperty.getProductScore)
    if(maxBound) reset()
    updateQueue()
  }

  //Update assign literal
  def updateAssignLiteral(lit : Int) : Unit = {
    val signe = if (lit < 0) LITERAL_NEGATIU else LITERAL_POSITIU

    if (literalScoreAssigment != null){
      val scoreAssigment = literalScoreAssigment.getOrElse(math.abs(lit), null)
      if (scoreAssigment != null) literalScoreAssigment.put(math.abs(lit), (scoreAssigment._1, signe))
    }

    if(literalIndex != null) {
      val index = literalIndex.get(math.abs(lit))
      if (index != null) {
        val sl = listModel.get(index)
        sl.setSign(signe)
        listModel.setElementAt(sl, index)
      }
    }
  }

  //String
  override def toString: String = {
    var string = ""
    val aux = litScoreQueue.clone().dequeueAll
    for(ls <- aux)
      string = string.concat(ls +  "\n" )
    string
  }

}

