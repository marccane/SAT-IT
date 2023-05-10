package solver

import java.util

import gui.DecisionCallback
import structure.enumeration.State

import scala.collection.mutable

class HistorySolver extends DecisionCallback{

  var nAccionsT: Int = 0
  //var historyTrail = _
  var historyTrail: mutable.ListBuffer[Int] = _
  var historyAction:  mutable.ListBuffer[Int] = _
  var numH: Int  = 0
  var inHistory: Boolean = false
  var lastLengthTrailDecision: Int = -1

  def init(): Unit ={
    nAccionsT = 0
    numH = 0
    historyTrail =  mutable.ListBuffer[Int]()
    historyAction =  mutable.ListBuffer[Int]()
    inHistory = false
    lastLengthTrailDecision = -1
  }

  def addAction(solverAction: Int): Unit ={
    if(!inHistory) {
      update()
      nAccionsT = nAccionsT + 1
      numH = numH + 1
      historyAction = historyAction.appended(solverAction)
    }
  }

  def subtractAction(): Unit ={
    if(!inHistory) {
      nAccionsT = nAccionsT - 1
      numH = numH - 1
    }

  }

  def addLiteral(literal: Int): Unit ={
    if(!inHistory) {
      update()
      historyTrail = historyTrail.appended(literal)
    }
  }

  def undo(): Unit ={
    if(numH != 0) numH = numH - 1
  }

  def redo(): Unit ={
    if(numH != nAccionsT) numH = numH + 1
  }


  def update(): Unit ={
    if(numH != nAccionsT){

      nAccionsT = numH
      var newHistoryAction = mutable.ListBuffer[Int]()
      for(i <- 0 until nAccionsT zip historyAction) newHistoryAction = newHistoryAction.appended(i._2)
      historyAction = newHistoryAction

      if(numH == 0){
        historyTrail = new mutable.ListBuffer[Int]()
        lastLengthTrailDecision = -1
      } else if(0 < lastLengthTrailDecision){
        var newTrail =  mutable.ListBuffer[Int]()
        val max = historyTrail.length - lastLengthTrailDecision
        for(i <- 0 until  max zip historyTrail) newTrail = newTrail.appended(i._2)
        historyTrail = newTrail
      }
    }
    lastLengthTrailDecision = -1
  }


  def getInHistory: Boolean = inHistory

  def getNumH: Int = numH

  def getHistoryTrailClone:  mutable.ListBuffer[Int] = for(i <- historyTrail) yield i

  def getHistorAction: Array[Int] = historyAction.toArray

  def setInHistory(h: Boolean): Unit ={
    inHistory = h
  }

  def setHistoryTrail(t:  mutable.ListBuffer[Int]): Unit ={
    historyTrail = t
  }

  override def makeDecisionCallback(): Int = {
    var l = -1
    if(historyTrail.nonEmpty) {
      l = historyTrail.head
      historyTrail = historyTrail.drop(1)
    }
    lastLengthTrailDecision = historyTrail.length
    l
  }

}
