package gui;
import solver.CDCL;
import structure.LitScoreSet;
import structure.LitScore;
import util.Constants;
import javax.swing.*;
import java.util.HashMap;

class VsidsScoreWindow extends JFrame {

    private JList<LitScore> list;

    VsidsScoreWindow(MainWindow mainWindow, CDCL cdcl, String title){
        super("VSIDS SCORES " + title.replaceFirst(MainWindow.APP_NAME + " .* ", ""));
        this.setIconImages(Constants.getLogos());
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));

        int width = (int) (Constants.getWidth()*0.185);
        LitScoreSet litScoreSet = cdcl.scoreLitQueue();
        if(litScoreSet.numVar() == 0)
        {
            JLabel chooseLabel = new JLabel("No scores",JLabel.CENTER);
            chooseLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            jPanel.add(chooseLabel);
            setSize(width, (int) (Constants.getHeight()*0.075)); //amplada, altura
            list = new JList<>();
        }
        else {
            //Model per guardar les variables de la llista
            DefaultListModel<LitScore> model = new DefaultListModel<>();
            //Estructura per guardar a on esta cada element
            HashMap<Integer, Integer> literalIndex = new HashMap<>();
            //Obtenim el llistat de puntuacions
            litScoreSet.setDefaultListModel(model);
            int i = 0;
            for (LitScore ls : litScoreSet.getOrderedLitScoreList()) {
                model.addElement(ls);
                literalIndex.put(ls.getLiteral(), i);
                i++;
            }
            litScoreSet.setMapLiteralIndex(literalIndex);
            list = new JList<>(model);
            jPanel.add(new JScrollPane(list));
            list.setCellRenderer(new ScoreLitRender(cdcl));
            setSize(width, width); //amplada, altura
        }

        add(jPanel);
        setResizable(true);
        setVisible(true);
        mainWindow.setVsidsScoreWindow(this);
    }

    void setCellRenderList(CDCL cdcl){
        list.setCellRenderer(new ScoreLitRender(cdcl));
    }
}
