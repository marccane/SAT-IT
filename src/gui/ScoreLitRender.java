package gui;

import solver.CDCL;
import solver.Solver;
import structure.litScore;
import util.SolverHelper;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

public class ScoreLitRender extends JLabel implements ListCellRenderer<litScore> {

    Solver solver;
    public static Border borderStyle =  BorderFactory.createLineBorder(Color.blue, 1);

    ScoreLitRender(Solver s){
        solver = s;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends litScore> list, litScore ls, int index,
    boolean isSelected, boolean cellHasFocus) {

        int maxLengthLit = 0;
        int maxLengthScore = 0;
        if(solver != null && solver.vsids()){
            maxLengthLit = ((CDCL) solver).maxLengthLit();
            maxLengthScore = ((CDCL) solver).maxLengthScore();
        }

        if(isSelected)
            setBorder(borderStyle);
        else
            setBorder(null);


        setFont(new Font("Consolas", Font.BOLD, 14));
        //setText(ls.toStringHtml(solver != null ? solver.numVariables() : 0));
        setText(ls.toString(maxLengthLit, maxLengthScore));

        if(!solver.undifinedLiteral(ls.lit())) {
            setOpaque(true);
            //setBackground(new Color(117, 129, 209, 40));
            setBackground(new Color(0, 255, 0, 30));
        }
        else{
            setOpaque(false);
        }
        return this;
    }

}
