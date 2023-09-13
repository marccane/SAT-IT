package gui;

import solver.CDCL;
import solver.Backtracking;
import solver.DPLL;
import solver.Solver;
import util.Constants;

import javax.swing.*;
import java.awt.*;

class SolverSelectorWindow extends JFrame {
    SolverSelectorWindow(MainWindow mainWindow, Solver solver){
        super("Solver selector ");
        setIconImages(Constants.getLogos());
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
        setResizable(false);

        JLabel chooseLabel = new JLabel("Choose a solver:");
        jPanel.add(chooseLabel);

        ButtonGroup group = new ButtonGroup();

        JRadioButton RBbacktracking = new JRadioButton("Backtracking", solver instanceof Backtracking);
        JRadioButton RBdpll = new JRadioButton("DPLL", solver instanceof DPLL);
        JRadioButton RBcdclVsids = new JRadioButton("CDCL with VSIDS", solver instanceof CDCL && solver.vsids());
        JRadioButton RBcdcl = new JRadioButton("CDCL", solver instanceof CDCL && !solver.vsids());

        group.add(RBbacktracking);
        group.add(RBdpll);
        group.add(RBcdcl);
        group.add(RBcdclVsids);

        jPanel.add(RBcdcl);
        jPanel.add(RBcdclVsids);
        jPanel.add(RBdpll);
        jPanel.add(RBbacktracking);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(stub -> {
            boolean loadInstance = true;
            if(mainWindow.instanceLoaded){
                int confirmed = JOptionPane.showConfirmDialog(this,
                        MainWindow.confirmResetMessage, "Confirmation",
                        JOptionPane.YES_NO_OPTION);

                if(confirmed == JOptionPane.NO_OPTION)
                    loadInstance = false;
            }

            if(loadInstance){
                MainWindow.SolverType solverType;
                if(RBbacktracking.isSelected()) solverType = MainWindow.SolverType.Backtracking;
                else if(RBdpll.isSelected()) solverType = MainWindow.SolverType.DPLL;
                else if(RBcdcl.isSelected()) solverType = MainWindow.SolverType.CDCL;
                else if(RBcdclVsids.isSelected()) solverType = MainWindow.SolverType.CDCL_VSIDS;
                else throw new RuntimeException("unknown solver");
                mainWindow.setSolverType(solverType);
                mainWindow.resetGUI();
                mainWindow.changeSolverTitle();
                mainWindow.setViewScoreListEnable(solverType == MainWindow.SolverType.CDCL_VSIDS);
                dispose();
            }
        });
        jPanel.add(btnSave, BorderLayout.SOUTH);

        setSize((int) (Constants.getWidth() * 0.143), (int) (Constants.getHeight() * 0.165)); //amplada, altura
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }
}
