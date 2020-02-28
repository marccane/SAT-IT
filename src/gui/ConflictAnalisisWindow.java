package gui;

import scala.collection.Iterator;
import scala.collection.immutable.Set;
import scala.collection.mutable.ArrayBuffer;
import structure.ConflictLog;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class ConflictAnalisisWindow extends JFrame{

    public ConflictAnalisisWindow(ConflictLog clog) {
        super("Conflict analisis (clauses " + clog.conflClauses()._1() + " and " + clog.conflClauses()._2() + ")");
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

        JTextPane jTextPane = new JTextPane();
        jTextPane.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(jTextPane);
        StyledDocument doc = jTextPane.getStyledDocument();
        addStylesToDocument(doc);

        //StringBuilder sb = new StringBuilder();
        Set<Object> lastDecisionLevelLits = clog.lastDecisionLevelLits();
        ArrayBuffer<Set<Object>> left = clog.clausesLeft(), right = clog.clausesRight();
        ArrayBuffer<Object> resolutionLits = clog.resolutionLits();
        resolutionLits.$plus$eq(0);
        int iters = clog.clausesLeft().length();
        try {
            for (int i = 0; i < iters; i++) {
                //Modificant el numero d'espais es pot canviar l'identacio
                String ident = new String(new char[i]).replace("\0", "  ");
                doc.insertString(doc.getLength(), ident, doc.getStyle("default"));

                writeClause(left.apply(i),-(int) resolutionLits.apply(i),doc,lastDecisionLevelLits);
                if (i != iters - 1) {
                    doc.insertString(doc.getLength(), "    ", doc.getStyle("bold"));
                    writeClause(right.apply(i),(int) resolutionLits.apply(i),doc,lastDecisionLevelLits);
                    doc.insertString(doc.getLength(), "\n" + ident + "------------------------------------------------------\n", doc.getStyle("default"));
                }
            }
        }
        catch(Exception e){e.printStackTrace();}
        jPanel.add(jScrollPane);


        setSize(600, 400); //amplada, altura
        //pack();
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }

    private String set2str(Set<Object> s){
        return s.foldLeft("",(String str, Object o) -> str+" "+o);
    }

    private void writeClause(Set<Object> s, int resolutionLit, StyledDocument doc, Set<Object> lastLevelLits) throws javax.swing.text.BadLocationException{
        Iterator<Object> it = s.iterator();
        while(it.hasNext()){
            Integer lit = (Integer) it.next();

            if(lit.equals(resolutionLit)){
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("resolutionLit"));
            }
            else if(lastLevelLits.contains(lit)){
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("lastLevelLit"));
            }
            else{
                doc.insertString(doc.getLength(), lit + " ", doc.getStyle("default"));
            }
        }
    }

    private void addStylesToDocument(StyledDocument doc){
        //ULL! def sembla ser una referencia a una variable estatica compartida per tots els documents
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontSize(def,14); //default font size: 12
        doc.addStyle("default", def); //potser una altre font quedaria millor...

        Style bold = doc.addStyle("bold", def);
        StyleConstants.setBold(bold, true);

        Style lastLevelLit = doc.addStyle("lastLevelLit", def);
        StyleConstants.setForeground(lastLevelLit, new Color(0, 69, 210));

        Style resolutionLit = doc.addStyle("resolutionLit", lastLevelLit);
        StyleConstants.setBold(resolutionLit, true);
    }

}
