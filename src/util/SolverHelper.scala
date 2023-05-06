package util

import structure.enumeration.State
import structure.enumeration.State.State

object SolverHelper {

  //Post: retorna cert si el literal esta satisfet
  def trueLiteral(literal: Int, varValue: Object): Boolean ={
    val positive = literal>0
    val state = varValue.asInstanceOf[Array[State]](math.abs(literal))
    state == State.TRUE && positive || state == State.FALSE && !positive
  }

  //Post: retorna cert si el literal es fals
  def falseLiteral(literal: Int, varValue: Object): Boolean={
    val positive = literal>0
    val state = varValue.asInstanceOf[Array[State]](math.abs(literal))
    !positive && state==State.TRUE || positive && state==State.FALSE
  }


}
