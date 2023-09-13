package gui;

import structure.VSIDSProperty;
import util.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;

public class VsidsOptionsWindow extends JFrame {

    //Titols
    public static String titleInitialValue   = "Initial Score";
    public static String titleIncrementValue = "Bonus";
    public static String titleProductValue   = "Incremented Bonus Constant";

    VsidsOptionsWindow(MainWindow mainWindow){
        super("VSIDS OPTIONS");
        add(new FrameVsids(mainWindow));
        this.setIconImages(Constants.getLogos());
        setResizable(false);
        pack();
        setVisible(true);
    }

    private class FrameVsids extends JPanel implements PropertyChangeListener{

        //Init value
        private double numInitialScore;
        private double numIncrementScore;
        private double numProductValueScore;

        //Field
        private JFormattedTextField  numInitialScoreField;
        private JFormattedTextField  numIncrementScoreField;
        private JFormattedTextField  numProductValueField;

        FrameVsids(MainWindow mainWindow){
            super(new BorderLayout());
            //Inicialitzem valors

            VSIDSProperty vsidsProperty = mainWindow.getVsidsProperty();

            numInitialScore = vsidsProperty.getStartScore().toDouble();
            numIncrementScore = vsidsProperty.getInitialAddScore().toDouble();
            numProductValueScore = vsidsProperty.getProductScore().toDouble();

            String[] labelsTitle = {titleInitialValue, titleIncrementValue, titleProductValue + "  "};
            //Entrada valor inicial
            //Labels
            JLabel initialScoreLabel = new JLabel(labelsTitle[0]);
            JLabel numIncrementScoreLabel = new JLabel(labelsTitle[1]);
            JLabel numProductValueLabel = new JLabel(labelsTitle[2]);

            numInitialScoreField =  new JFormattedTextField();
            numIncrementScoreField =  new JFormattedTextField();
            numProductValueField = new JFormattedTextField();

            numInitialScoreField.setValue(numInitialScore);
            numIncrementScoreField.setValue(numIncrementScore);
            numProductValueField.setValue(numProductValueScore);

            ArrayList<JFormattedTextField> fields = new ArrayList<>();
            fields.add(numInitialScoreField);
            fields.add(numIncrementScoreField);
            fields.add(numProductValueField);

            ArrayList<JLabel> labels = new ArrayList<>();
            labels.add(initialScoreLabel);
            labels.add(numIncrementScoreLabel);
            labels.add(numProductValueLabel);

            initInputs(fields, labels);

            //Lay out the labels in a panel.
            JPanel labelPane = new JPanel(new GridLayout(0,1));
            for(JLabel l: labels) labelPane.add(l);

            //Layout the text fields in a panel.
            JPanel fieldPane = new JPanel(new GridLayout(0,1));
            for(JFormattedTextField f: fields) fieldPane.add(f);

            labelPane.add(new JLabel(""));
            fieldPane.add(new JLabel(""));
            JLabel help = new JLabel("<html><font color=\"blue\"><u>Help</u></font><html>");
            help.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                        new VsidsHelpWindow();
                }
            });
            labelPane.add(help);

            JButton btnSave = new JButton("Save");
            btnSave.addActionListener(stub -> {
                String startScore = normalizeText(fields.get(0).getText());
                String addScore = normalizeText(fields.get(1).getText());
                String productScore = normalizeText(fields.get(2).getText());
                mainWindow.setVsidsProperty(new VSIDSProperty(new scala.math.BigDecimal(new BigDecimal(startScore)), new scala.math.BigDecimal(new BigDecimal(addScore)), new scala.math.BigDecimal(new BigDecimal(productScore))));
                dispose();
                if (mainWindow.getSolverType() == MainWindow.SolverType.CDCL_VSIDS) mainWindow.resetGUI();
            });

            fieldPane.add(btnSave);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            add(labelPane, BorderLayout.CENTER);
            add(fieldPane, BorderLayout.LINE_END);
        }

        public void propertyChange(PropertyChangeEvent e) {
            Object source = e.getSource();
            if (source == numInitialScoreField) {
                numInitialScore = ((Number)numInitialScoreField.getValue()).doubleValue();
            } else if(source == numIncrementScoreField){
                numIncrementScore = ((Number)numIncrementScoreField.getValue()).doubleValue();
            } else if(source == numProductValueField){
                numProductValueScore = ((Number)numProductValueField.getValue()).doubleValue();
            }
        }

        String normalizeText(String text){
            return text.replaceAll("\\.", "").replaceAll(",","\\.");
        }

        void initInputs(ArrayList<JFormattedTextField> fields, ArrayList<JLabel> labels){
            for(int i = 0; i < fields.size(); i++){
                JFormattedTextField field = fields.get(i);
                JLabel label = labels.get(i);
                field.setHorizontalAlignment(JTextField.RIGHT);
                field.setColumns(10);
                field.setEditable(true);
                field.addPropertyChangeListener("value", this);
                label.setLabelFor(field);
            }
        }
    }
}
