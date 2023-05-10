package structure

import event.{BackjumpEvent, BacktrackEvent, DecisionEvent, Event, UnitPropEvent}

import scala.collection.mutable.ArrayBuffer

class EventManager {
  var events = new ArrayBuffer[Event]()
  private var conflictIdx = 0

  def insertEvent(e: Event): Unit = {
    if(events.nonEmpty && e.isInstanceOf[UnitPropEvent] && events.last.isInstanceOf[UnitPropEvent])
      events.last.asInstanceOf[UnitPropEvent].addLiteral(e.asInstanceOf[UnitPropEvent].literals.head)
    else events += e
  }

  def getConflictIdx: Int = {
    conflictIdx += 1
    conflictIdx-1
  }

  def getStatistics: (Int,Int,Int) = {
    var numDecisions = 0
    var numPropagations = 0
    var numConflicts = 0
    for(event <- events){
      event match {
        case DecisionEvent(_, _) => numDecisions += 1
        case UnitPropEvent(literals) => numPropagations += literals.length
        case BacktrackEvent(_, _) => numConflicts += 1
        case BackjumpEvent(_, _) => numConflicts += 1
      }
    }
    (numDecisions,numPropagations,numConflicts)
  }

  def printEvents(compliantFormat: Boolean): Unit = {
    if(compliantFormat)
      print("c ")
    else println
    println("Events:")
    events.foreach({if(compliantFormat) print("c "); _.print})
  }

  def reset() = {
    events = new ArrayBuffer[Event]()
    conflictIdx = 0
  }

}
