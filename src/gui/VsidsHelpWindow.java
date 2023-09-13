package gui;

import util.Constants;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

class VsidsHelpWindow extends JFrame {
    VsidsHelpWindow(){
        super("VSIDS HELP");
        this.setIconImages(Constants.getLogos());
        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setEditable(false);

        this.setIconImages(Constants.getLogosHelp());

        JScrollPane jScrollPane = new JScrollPane(jEditorPane);
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        jEditorPane.setEditorKit(htmlEditorKit);

        String page ="<html>" +
                "<body style=\"font-size:18px;margin:10px;\"> " +
                "<h1 style = \"text-align: center;font-size:24px;\"> " +
                "Variable State Independent Decaying Sum " +
                "</h1> " +
                " " +
                "<p> " +
                "In our implementation of CDCL, DPLL and Backtracking solvers, the decision literals are chosen according to their number, being the smallest the first." +
                "</p> " +
                "" +
                "<p> " +
                "In the CDCL solver, the user can change this default decision strategy to Variable State Independent Decaying Sum (VSIDS). This can be done by choosing the CDCL with VSIDS solver. " +
                "</p>" +
                " " +
                "<p> " +
                "VSIDS is a heuristic decision technique based on the idea that each literal has a score, and when the solver has to decide a literal, it will choose the literal with the highest score. The score intents to capture the relevance of the literal in the found conflicts. To be able to assign and modify the literal score, the program follows the next scheme: " +
                "</p> " +
                " " +
                "<ol> " +
                "<li> " +
                "When loading a new instance in the program, the initial score of each variable is obtained by adding the value of the <b><i>Initial Score</i></b> each time the literal appears in a clause.  " +
                "</li> " +
                "<br> " +
                "<li> " +
                "When the solver learns a new clause, the score of each literal of the clause will be increased by <b><i>Bonus</i></b>. Once these scores have been increased, the value of <b><i>Bonus</i></b> is modified according to the following formula:" +
                "<br> " +
                "<br> " +
                "<p style =\"text-align: center;\"><b><i>Bonus = (Incremented Bonus Constant)*Bonus</i></b></p> " +
                "<br> " +
                "Thanks to this <b><i>Bonus</i></b> updated, the more learnt clauses, the more reward for the involved literals." +
                "To avoid numerical overflow, if a score surpasses the value 10<sup>100</sup>, all scores from all variables and the variable <b><i>Bonus</i></b>, are scaled by 10<sup>&#8722;100</sup>. " +
                "</li> " +
                "<br> " +
                "<li> " +
                "Each time that a variable is decided or unit propagated, the sign with which it has been decided or propagated will be saved. If during the trail that variable have to be decided, it will be used the saved sign. Initially all variables signs are saved to be negative. This process is called <b><i>phase saving</i></b>. " +
                "</li> " +
                "</ol> " +
                " " +
                "<p> " +
                "<br> " +
                "To see the scores of literals and their saved phase, the user can select the <i>Score List</i> in the <i>View</i> menu. " +
                "<br> " +
                "<br> " +
                "The values of variables <b><i>Initial Score</i></b>, <b><i>Bonus</i></b>, <b><i>Incremented Bonus Constant</i></b>, " +
                "can be seen and modified in the VSIDS OPTIONS window (to access this window, in the main window go to <i>Options &#8594; VSIDS parameters</i>). The default values of this variables are: " +
                " " +
                "<ul> " +
                "<li><b><i>Initial Score: " + Constants.INITIAL_SCORE_VALUE.toString().replaceAll("\\.",",") + "</i></b></li>" +
                "<li><b><i>Bonus: " + Constants.BONUS_SCORE_VALUE.toString().replaceAll("\\.",",") + " </i></b></li>" +
                "<li><b><i>Incremented Bonus Constant: " + Constants.INCREMENTED_BONUS_CONSTANT.toString().replaceAll("\\.",",") + "  </i></b></li>" +
                "</ul> " +
                "</p> " +
                "</body>  " +
                "</html>";

        htmlEditorKit.createDefaultDocument();
        jEditorPane.setContentType("text/html");
        jEditorPane.setText(page);
        add(jScrollPane);
        setPreferredSize(new Dimension((int) (Constants.getWidth()*0.7), (int) (Constants.getHeight()*0.8)));
        pack();

        setResizable(true);
        setVisible(true);
        SwingUtilities.invokeLater(() -> jScrollPane.getViewport().setViewPosition( new Point(0, 0) ));
    }
}
