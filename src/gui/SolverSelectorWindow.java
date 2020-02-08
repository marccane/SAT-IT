package gui;

import solver.CDCL;
import solver.Backtracking;
import solver.DPLL;
import solver.Solver;

import javax.swing.*;

public class SolverSelectorWindow extends JFrame {
    SolverSelectorWindow(MainGUI mainGUI, Solver solver){
        super("Solver selector");
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

        JLabel chooseLabel = new JLabel("Choose a solver:");
        jPanel.add(chooseLabel);

        ButtonGroup group = new ButtonGroup();

        JRadioButton RBbacktracking = new JRadioButton("Backtracking", solver instanceof Backtracking);
        JRadioButton RBdpll = new JRadioButton("DPLL", solver instanceof DPLL);
        JRadioButton RBcdcl = new JRadioButton("CDCL", solver instanceof CDCL);

        group.add(RBbacktracking);
        group.add(RBdpll);
        group.add(RBcdcl);

        jPanel.add(RBcdcl);
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
                else throw new RuntimeException("unknown solver");
                mainGUI.setSolverType(solverType);
                mainGUI.resetGUI();
                dispose();
            }
        });
        jPanel.add(btnSave);

        setSize(150, 150); //amplada, altura
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }
}
