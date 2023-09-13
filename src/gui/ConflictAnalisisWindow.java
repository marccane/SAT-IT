package gui;

import scala.collection.Iterator;
import scala.collection.immutable.Set;
import scala.collection.mutable.ArrayBuffer;
import structure.ConflictLog;
import util.Constants;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

class ConflictAnalisisWindow extends JFrame{

    ConflictAnalisisWindow(ConflictLog clog) {
        super("Conflict analysis (clauses " + clog.conflClauses()._1() + " and " + clog.conflClauses()._2() + ")");
        this.setIconImages(Constants.getLogos());
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

        JTextPane jTextPane = new JTextPane();
        jTextPane.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(jTextPane);
        StyledDocument doc = jTextPane.getStyledDocument();
        addStylesToDocument(doc);

        Set<Object> lastDecisionLevelLits = clog.lastDecisionLevelLits();
        ArrayBuffer<Set<Object>> left = clog.clausesLeft(), right = clog.clausesRight();
        ArrayBuffer<Object> resolutionLits = clog.resolutionLits();
        resolutionLits.$plus$eq(0);
        int l1,l2,total = 0;
        int iters = clog.clausesLeft().length();

        try {
            for (int i = 0; i < iters; i++) {
                //Modificant el numero d'espais es pot canviar l'identacio
                String ident = new String(new char[i]).replace("\0", "  ");
                doc.insertString(doc.getLength(), ident, doc.getStyle("defaultConflict"));
                l1 = writeClause(left.apply(i),-(int) resolutionLits.apply(i),doc,lastDecisionLevelLits);
                if (i == iters - 1){
                    doc.remove(doc.getLength() - 2 - l1, 2 + l1);
                    int sep  = (total+5)/2 - (l1)/2;
                    String espais = "";
                    for(int w = 0; w < sep; w++) espais = espais.concat(" ");

                    doc.insertString(doc.getLength(), espais, doc.getStyle("defaultConflict"));
                    writeClause(left.apply(i),-(int) resolutionLits.apply(i),doc,lastDecisionLevelLits);
                }
                if (i != iters - 1) {
                    doc.insertString(doc.getLength(), "    ", doc.getStyle("bold"));
                    l2 = writeClause(right.apply(i),(int) resolutionLits.apply(i),doc,lastDecisionLevelLits);
                    total = l1 + l2;
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("endLConflict"));
                    String linia = "---";
                    for(int w = 0; w <= l1 + l2; w++) linia = linia.concat("-");
                    doc.insertString(doc.getLength(), ident + linia, doc.getStyle("defaultConflict"));
                }
                doc.insertString(doc.getLength(), "\n", doc.getStyle("endLConflict"));
            }
        }
        catch(Exception e){e.printStackTrace();}
        jPanel.add(jScrollPane);

        setSize((int) (Constants.getWidth() * 0.35), (int) (Constants.getHeight() * 0.4)); //amplada, altura
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }

    private int writeClause(Set<Object> s, int resolutionLit, StyledDocument doc, Set<Object> lastLevelLits) throws javax.swing.text.BadLocationException{
        Iterator<Object> it = s.iterator();
        int l = 0;
        while(it.hasNext()){
            Integer lit = (Integer) it.next();
            l += String.valueOf(lit).length()+ 1;
            if(lit.equals(resolutionLit)){
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("resolutionLit"));
            }
            else if(lastLevelLits.contains(lit)){
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("lastLevelLit"));
            }
            else{
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("defaultConflict"));
            }
        }
        return l;
    }

    private void addStylesToDocument(StyledDocument doc){
        //ULL! def sembla ser una referencia a una variable estatica compartida per tots els documents
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        doc.addStyle("default", def); //potser una altre font quedaria millor...

        Style defaultConflict = doc.addStyle("defaultConflict", def);
        StyleConstants.setFontSize(defaultConflict,18);
        StyleConstants.setFontFamily(defaultConflict, "Consolas");

        Style endLConflict = doc.addStyle("endLConflict", defaultConflict);
        StyleConstants.setFontSize(endLConflict,5);

        Style bold = doc.addStyle("bold", defaultConflict);
        StyleConstants.setBold(bold, true);
        StyleConstants.setFontFamily(bold, "Consolas");

        Style lastLevelLit = doc.addStyle("lastLevelLit", defaultConflict);
        StyleConstants.setForeground(lastLevelLit, new Color(0, 69, 210));
        StyleConstants.setFontFamily(lastLevelLit, "Consolas");

        Style resolutionLit = doc.addStyle("resolutionLit", lastLevelLit);
        StyleConstants.setBold(resolutionLit, true);
        StyleConstants.setFontFamily(resolutionLit, "Consolas");
    }

}
