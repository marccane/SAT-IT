package gui;

import structure.Clause;
import util.SolverHelper;

import javax.swing.*;
import java.awt.*;

//https://www.logicbig.com/tutorials/java-swing/list-filter-html-highlighting.html
class ClauseListLiteralHighlight {

    static ListCellRenderer<? super Clause> createListRenderer(Object[] varValue, int numInitialClauses) {
        return new DefaultListCellRenderer() {
            private Color satisfied = new Color(0, 255, 0, 30);
            private Color contradiction = new Color(255, 0, 0, 30);

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Clause clause = (Clause) value;
                boolean learntClause = index >= numInitialClauses;
                String displayText = LiteralHighlighter.highlightText(clause, varValue, index, learntClause);
                setText(displayText);

                int clauseState = getClauseState(clause, varValue);
                if(clauseState == -1) setBackground(contradiction);
                else if(clauseState == 1) setBackground(satisfied);

                return this;
            }

            //Post: -1 contradiction, 0 unsatisfied, 1 satisfied
            private int getClauseState(Clause clause, Object[] varValue){
                boolean satisfied = false;
                boolean contradicting = true;
                int[] litArray = clause.getClauseArray();

                int i = 0;
                while((!satisfied || contradicting) && i<litArray.length){
                    int lit = litArray[i++];
                    boolean trueLit = SolverHelper.trueLiteral(lit, varValue);
                    boolean falseLit = SolverHelper.falseLiteral(lit, varValue);
                    if(trueLit) satisfied = true;
                    if(!falseLit) contradicting = false;
                }

                return contradicting?-1:(satisfied?1:0);
            }
        };
    }

    private static class LiteralHighlighter {
        private static final String HighLightDefault = "%d ";
        private static final String HighLightTrue = "<font color=\"green\">%d </font>";
        private static final String HighLightFalse = "<font color=\"red\">%d </font>";
        private static final String HighLightLearnt = "<font color=\"orange\">%d </font>";

        static String highlightText(Clause clause, Object[] varValue, int index, boolean learntClause) {
            int[] litArray = clause.getClauseArray();
            StringBuilder sb = new StringBuilder();

            sb.append("<html>").append(String.format(learntClause ? HighLightLearnt : HighLightDefault, index)).append(": ");
            for (int lit: litArray) {
                boolean trueLit = SolverHelper.trueLiteral(lit, varValue);
                boolean falseLit = SolverHelper.falseLiteral(lit, varValue);
                sb.append(String.format(trueLit? HighLightTrue :(falseLit? HighLightFalse :HighLightDefault), lit));
            }
            sb.append(" </html>");

            return sb.toString();
        }
    }

}
