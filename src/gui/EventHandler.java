package gui;

import event.BackjumpEvent;
import event.Event;
import scala.Enumeration;
import scala.Tuple3;
import scala.collection.mutable.ArrayBuffer;
import solver.CDCL;
import solver.Solver;
import structure.ConflictLog;
import structure.EventManager;
import structure.enumeration.SolvingState;

import javax.swing.*;
import java.awt.event.MouseEvent;

import static util.Constants.*;

class EventHandler {

    static void eventListMouseClick(MouseEvent evt, Solver solver){
        JList list = (JList)evt.getSource();
        EventManager em = solver.eventManager();

        if (evt.getClickCount() == 2) { // Double-click detected
            int index = list.locationToIndex(evt.getPoint());
            ArrayBuffer<Event> events = em.events();
            Event event = events.apply(index);

            if(event instanceof BackjumpEvent){
                BackjumpEvent bkjp = (BackjumpEvent) event;
                int conflictIdx = bkjp.conflictIdx();
                ConflictLog cl = ((CDCL)solver).conflictLogger().getLog(conflictIdx);

                java.awt.EventQueue.invokeLater(() -> new ConflictAnalisisWindow(cl));
            }
        }
    }

    private interface ButtonConfig {
        boolean stopOnEvents(int solverAction);
    }

    static void btnDecisionActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction == SOLVER_UNITPROP);
    }

    static void btnUnitPropActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction == SOLVER_DECISION);
    }

    static void btnConflictActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int sAct) -> sAct != SOLVER_BACKTRACK && sAct != SOLVER_CONFLICT
                && sAct != SOLVER_END);
    }

    static void btnEndActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction != SOLVER_END);
    }

    static void btnStepActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int solverAction) -> false);
    }

    private static void runSolverUntil(Solver solver, MainGUI mainGUI, ButtonConfig bc){
        Enumeration.Value solverState = solver.solverState(); // 0->unsolved, 1->sat, 2->unsat
        if(solverState == SolvingState.UNSOLVED()){ //si no hem acabat...
            int solverAction;
            do{
                solverAction = mainGUI.solverStep();
                //debugPrintSolverAction(solverAction);

                if(solverAction == SOLVER_END)
                    solverShowResult(solver, mainGUI.getjPanel());
            }
            while(bc.stopOnEvents(solverAction));
        }
        else solverShowResult(solver, mainGUI.getjPanel());
        mainGUI.updateGUI();
    }

    private static void debugPrintSolverAction(int solverAction){
        switch(solverAction){
            case 0:
                System.out.println("Finished");
                break;
            case 1:
                System.out.println("Decision");
                break;
            case 2:
                System.out.println("UnitProp");
                break;
            case 3:
                System.out.println("Backjump");
                break;
            case 4:
                System.out.println("Conflict");
                break;
            case 5:
                System.out.println("Backtrack");
                break;
            default:
                System.out.println("SWITCH DEFAULT CASE");
                break;
        }
    }

    private static void solverShowResult(Solver solver, JPanel jpanel){
        Tuple3 tuple3 = solver.eventManager().getStatistics();
        int decisions = (int)tuple3._1();
        int propagations = (int)tuple3._2();
        int conflicts = (int)tuple3._3();
        JOptionPane.showMessageDialog(jpanel, (solver.solverState()==SolvingState.SAT()?"SAT":"UNSAT") +
                "\n\nDecisions: " + decisions + "\nPropagations: " + propagations + "\nConflicts: " + conflicts,
                "Summary", JOptionPane.INFORMATION_MESSAGE);
    }

}
