package gui;

import javax.swing.*;
import java.awt.*;

public class AboutDialogWindow extends JFrame {
    AboutDialogWindow(){
        super("About");
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
        jPanel.setBackground(Color.white);

        //no hi ha manera de que es centri...
        JLabel appNameLabel = new JLabel("SAT-IT " + MainGUI.VERSION, SwingConstants.CENTER);
        //appNameLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        JLabel authorLabel = new JLabel("Author: Marc Cané Salamià");
        JLabel tutorsLabel = new JLabel("Tutors: Mateu Villaret & Jordi Coll");

        jPanel.add(appNameLabel);
        jPanel.add(authorLabel);
        jPanel.add(tutorsLabel);

        setSize(400, 100); //amplada, altura
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }
}
