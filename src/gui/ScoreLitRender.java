package gui;

import solver.CDCL;
import solver.Solver;
import structure.LitScore;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;

class ScoreLitRender extends JLabel implements ListCellRenderer<LitScore> {

    private Solver solver;
    private static Border borderStyle =  BorderFactory.createLineBorder(Color.blue, 1);

    ScoreLitRender(Solver s){
        solver = s;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends LitScore> list, LitScore ls, int index,
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
        setText(ls.toString(maxLengthLit, maxLengthScore));

        if(!solver.undefinedLiteral(ls.lit())) {
            setOpaque(true);
            setBackground(new Color(0, 255, 0, 30));
        }
        else{
            setOpaque(false);
        }
        return this;
    }

}
