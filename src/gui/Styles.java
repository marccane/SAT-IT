package gui;

import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.ArrayList;

class Styles {

    private ArrayList<Style> styles;
    private ArrayList<Style> highlightStyles;

    //Constants
    private static final Color darkOrange = new Color(255,128,0);
    private static final Color clearOrange = new Color(255,200,145);
    static final Color darkYellow = new Color(255, 183, 0);
    static final Color purple = new Color(128,0,128);

    void setBlurredStyle(){
        ///propagationLiteral
        StyleConstants.setForeground(styles.get(0), Color.lightGray);
        //propagationLiteralLearned
        StyleConstants.setForeground(styles.get(1), clearOrange);
        ///decisionLiteral
        StyleConstants.setForeground(styles.get(2), new Color(120, 177, 255));
        ///backtrackLiteral
        StyleConstants.setForeground(styles.get(3), new Color(255, 216, 117));
        ///superscript
        StyleConstants.setForeground(styles.get(4), Color.lightGray);
    }

    void setClearStyle(){
        ///propagationLiteral
        StyleConstants.setForeground(styles.get(0), Color.black);
        //propagationLiteralLearned
        StyleConstants.setForeground(styles.get(1), darkOrange);
        ///decisionLiteral
        StyleConstants.setForeground(styles.get(2), Color.blue);
        ///backtrackLiteral
        StyleConstants.setForeground(styles.get(3),  darkYellow);
        ///superscript
        StyleConstants.setForeground(styles.get(4), Color.black);
    }

    void setHighlightStyle(boolean hihglight){
        Color color = hihglight ? new Color(255, 0, 0, 30) : new Color(0,0,0, 0);
        highlightStyles.forEach((s) -> StyleConstants.setBackground(s, color));
    }

    void addStylesToDocument(StyledDocument doc){
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        styles = new ArrayList<>();
        highlightStyles = new ArrayList<>();
        //StyleConstants.setFontFamily(def, "SansSerif");
        doc.addStyle("default", def);

        Style propLit = doc.addStyle("propagationLiteral", def);
        styles.add(propLit);
        highlightStyles.add(propLit);

        Style propLitLearned = doc.addStyle("propagationLiteralLearned", def);
        StyleConstants.setForeground(propLitLearned, darkOrange);
        StyleConstants.setBold(propLitLearned, true);
        styles.add(propLitLearned);
        highlightStyles.add(propLitLearned);

        Style redBold = doc.addStyle("fail", def);
        StyleConstants.setBold(redBold, true);
        StyleConstants.setForeground(redBold, Color.red);

        Style blackBold = doc.addStyle("backtrack", redBold);
        StyleConstants.setForeground(blackBold, darkYellow);

        Style orangeBold = doc.addStyle("learned", redBold);
        StyleConstants.setForeground(orangeBold, darkOrange);

        Style greenBold = doc.addStyle("sat", redBold);
        StyleConstants.setForeground(greenBold, Color.green);

        Style blueBold = doc.addStyle("decisionLiteral", redBold);
        StyleConstants.setForeground(blueBold, Color.blue);
        styles.add(blueBold);
        highlightStyles.add(blueBold);

        Style cyanBold = doc.addStyle("backtrackLiteral", blueBold);
        StyleConstants.setForeground(cyanBold, darkYellow);
        StyleConstants.setBold(cyanBold, true);
        styles.add(cyanBold);
        highlightStyles.add(cyanBold);

        Style superscript = doc.addStyle("superscript", def);
        StyleConstants.setSuperscript(superscript, true);
        styles.add(superscript);
        setClearStyle();
    }

}
