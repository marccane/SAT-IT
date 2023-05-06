package gui;

import solver.CDCL;
import solver.Backtracking;
import solver.DPLL;
import solver.Solver;
import sun.applet.Main;
import util.Constants;

import javax.swing.*;
import java.awt.*;

public class SolverSelectorWindow extends JFrame {
    SolverSelectorWindow(MainGUI mainGUI, Solver solver){
        super("Solver selector ");
        this.setIconImages(Constants.getLogos());
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

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
        //btnSave.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        btnSave.addActionListener(stub -> {
            boolean loadInstance = true;
            if(mainGUI.instanceLoaded){
                int confirmed = JOptionPane.showConfirmDialog(this,
                        MainGUI.confirmResetMessage, "Confirmation",
                        JOptionPane.YES_NO_OPTION);

                if(confirmed == JOptionPane.NO_OPTION)
                    loadInstance = false;
            }

            if(loadInstance){
                MainGUI.SolverType solverType;
                if(RBbacktracking.isSelected()) solverType = MainGUI.SolverType.Backtracking;
                else if(RBdpll.isSelected()) solverType = MainGUI.SolverType.DPLL;
                else if(RBcdcl.isSelected()) solverType = MainGUI.SolverType.CDCL;
                else if(RBcdclVsids.isSelected()) solverType = MainGUI.SolverType.CDCL_VSIDS;
                else throw new RuntimeException("unknown solver");
                mainGUI.setSolverType(solverType);
                mainGUI.resetGUI();
                mainGUI.changeSolverTitle();
                if(solverType != MainGUI.SolverType.CDCL_VSIDS) mainGUI.setViewScoreListEnable(false);
                else mainGUI.setViewScoreListEnable(true);
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
