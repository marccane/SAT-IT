package gui;

import event.BackjumpEvent;
import event.Event;
import javafx.util.Pair;
import scala.Enumeration;
import scala.Tuple3;
import scala.collection.immutable.IndexedSeq;
import scala.collection.mutable.ArrayBuffer;
import solver.CDCL;
import solver.Solver;
import structure.ConflictLog;
import structure.EventManager;
import structure.enumeration.SolvingState;
import util.Constants;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import static javax.swing.BorderFactory.createEmptyBorder;
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
        int res = runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction == SOLVER_UNITPROP);
        if(res != CANCEL_DECISION) {
            mainGUI.failAction(res);
            if (!mainGUI.isBacktracking()) mainGUI.setDecision(false);
        }
    }

    static void btnUnitPropActionHandler(Solver solver, MainGUI mainGUI){
        int res = runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction == SOLVER_DECISION);
        if(res != CANCEL_DECISION) mainGUI.failAction(res);
    }

    static void btnConflictActionHandler(Solver solver, MainGUI mainGUI){
        int res = runSolverUntil(solver,mainGUI,(int sAct) -> sAct != SOLVER_BACKTRACK && sAct != SOLVER_CONFLICT
                && sAct != SOLVER_END);
        if(res != CANCEL_DECISION) mainGUI.failAction(res);
    }

    static void btnResolveActionHandler(Solver solver, MainGUI mainGUI){
        int res = runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction == SOLVER_UNITPROP);
        if(res != CANCEL_DECISION) {
            mainGUI.initButtoms();
            //Amb CDCL i DPLL hem de fer UP
            if (!mainGUI.isBacktracking()) mainGUI.setDecision(false);
        }
    }

    static void btnEndActionHandler(Solver solver, MainGUI mainGUI){
        int res = runSolverUntil(solver,mainGUI,(int solverAction) -> solverAction != SOLVER_END);
        if(res != CANCEL_DECISION) mainGUI.initButtoms();
    }

    static void btnStepActionHandler(Solver solver, MainGUI mainGUI){
        runSolverUntil(solver,mainGUI,(int solverAction) -> false);
    }

    private static int runSolverUntil(Solver solver, MainGUI mainGUI, ButtonConfig bc){
        Enumeration.Value solverState = solver.solverState(); // 0->unsolved, 1->sat, 2->unsat
        int solverAction = -1;
        if(solverState == SolvingState.UNSOLVED()){ //si no hem acabat...
            do{
                solverAction = mainGUI.solverStep();
                if(solver.foundBreakpoint())
                    showBreakpoints(solver, mainGUI, mainGUI.getPanel());
            } while(bc.stopOnEvents(solverAction) && solverAction != CANCEL_DECISION);

            if(solverAction == SOLVER_CONFLICT)
                mainGUI.focusOnConflictClause();
            else if(solverAction == SOLVER_BACKJUMP)
                mainGUI.focusOnLearnedClause();
            else if(solverAction == SOLVER_END)
                solverShowResult(mainGUI, solver, mainGUI.getPanel());
            else if(solverAction == CANCEL_DECISION)
                solver.setCancel(false);
        }
        else solverShowResult(mainGUI, solver, mainGUI.getPanel());
        mainGUI.updateGUI();
        return solverAction;
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

    private static void solverShowResult(MainGUI mainGUI, Solver solver, JPanel jpanel){
        mainGUI.setEnableKeyBoard(false);
        Tuple3 tuple3 = solver.eventManager().getStatistics();
        int decisions = (int)tuple3._1();
        int propagations = (int)tuple3._2();
        int conflicts = (int)tuple3._3();
        JOptionPane.showMessageDialog(jpanel, (solver.solverState()==SolvingState.SAT()?"SAT":"UNSAT") +
                "\n\nDecisions: " + decisions + "\nPropagations: " + propagations + "\nConflicts: " + conflicts,
                "Summary", JOptionPane.INFORMATION_MESSAGE);
        mainGUI.setEnableKeyBoard(true);
    }


    private static void showBreakpoints(Solver solver, MainGUI mainGUI, JPanel jpanel) {
        mainGUI.setEnableKeyBoard(false);
        scala.collection.mutable.Queue<Object> detected = solver.getDetectedBreakpoints();
        Object[] data = new Object[detected.size() + 1];
        data[0] = "The following variables have been assigned:";
        int i = 1;
        scala.collection.Iterator<Object> it = detected.iterator();
        while (it.hasNext()){
            data[i] = "\u2192" + it.next().toString();
            i ++;
        }

        JList jList = new JList(data);
        jList.setBackground(new JOptionPane().getBackground());
        jList.setSelectionModel(new DisableSlect());

        JScrollPane scrollPane = new JScrollPane(jList);
        scrollPane.setBorder(createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension((int) (Constants.getWidth()*0.25), (int) (Constants.getHeight()*0.15)));

        JOptionPane.showMessageDialog(jpanel,scrollPane,"Breakpoint", JOptionPane.INFORMATION_MESSAGE);
        solver.removeDetectedBreakpoints();
        mainGUI.setEnableKeyBoard(true);
    }

    private static class DisableSlect extends DefaultListSelectionModel{
        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) {}
    }

}
