package gui;

import javafx.util.Pair;
import scala.collection.Iterator;
import solver.Solver;
import util.Constants;
import javax.swing.*;
import java.awt.*;

public class BreakpointSelectWindow extends JFrame {

    private Solver solver;

    BreakpointSelectWindow( MainGUI mainGUI, Solver solverInit){
        super("Breakpoints");
        this.setIconImages(Constants.getLogos());
        this.solver = solverInit;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());


        DefaultListModel<Pair<Integer, Boolean>> model = new DefaultListModel<>();
        initPairDefaultListModel(model);

        JList list = new JList(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new BreakpointListRender());
        list.setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                if(super.isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                }
                else {
                    super.addSelectionInterval(index0, index1);
                }
                Pair<Integer, Boolean> value = model.get(index1);
                Boolean isBreakpoint = !value.getValue();
                model.set(index1, new Pair<>(value.getKey(), isBreakpoint));
                if(isBreakpoint)
                    solver.addBreakpoints(value.getKey());
                else
                    solver.removeBreakpoints(value.getKey());
            }
        });

        JButton clearAll = new JButton("Clear All");
        clearAll.addActionListener(stub ->clearAll(mainGUI, this, model));
        JScrollPane jScrollPane = new JScrollPane(list);
        jPanel.add(jScrollPane, BorderLayout.CENTER);
        if(model.size() != 0)
            jPanel.add(clearAll, BorderLayout.SOUTH);
        jPanel.setBackground(Color.white);
        int sizeRow = 20;
        int height = 2 * sizeRow + sizeRow * (model.size() != 0 ? 1 : 0);
        if(model.size() != 0)
            height += sizeRow * model.size();
        setSize((int) (Constants.getWidth() * 0.1303), (int) Math.min(height, Constants.getHeight() *0.9));
        add(jPanel);
        setResizable(true);
        setVisible(true);
        mainGUI.setBreakpointSelectWindow(this);
    }

    private void initPairDefaultListModel(DefaultListModel<Pair<Integer, Boolean>> model) {
        Iterator<Pair<Object, Object>> it = solver.getAllVariablesBreakpoints().iterator();
        while (it.hasNext()){
            Pair<Object, Object> p = it.next();
            model.addElement(new Pair<>((Integer) p.getKey(), (Boolean) p.getValue()));
        }
    }

    public void setSolver(Solver solver){
        this.solver = solver;
    }

    public void clearAll(MainGUI mainGUI, JFrame jframe, DefaultListModel<Pair<Integer, java.lang.Boolean>> model){
        mainGUI.setEnableKeyBoard(false);
        int confirmed = JOptionPane.showConfirmDialog(jframe,
                "Are you sure you want to remove all breakpoints?", "Confirmation",
                JOptionPane.YES_NO_OPTION);
        if(confirmed == JOptionPane.YES_OPTION){
            solver.removeAllBreakpoints();
            model.clear();
            initPairDefaultListModel(model);
        }
        mainGUI.setEnableKeyBoard(true);

    }


    class BreakpointListRender extends JLabel implements ListCellRenderer<Pair<Integer, Boolean>> {

        public Component getListCellRendererComponent(JList<? extends Pair<Integer, Boolean>> list, Pair<Integer, Boolean> value, int index, boolean isSelected, boolean cellHasFocus) {
            this.setHorizontalTextPosition(JLabel.RIGHT);
            this.setText(value.getKey().toString());
            this.setFont(new Font(this.getFont().getName(), this.getFont().getStyle(), 14));
            this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            if(value.getValue()){
                setOpaque(true);
                setBackground(new Color(255, 0, 0, 30));
                this.setIcon(new ImageIcon(this.getClass().getResource("/logo/RedBreakpoint.png"),"Breakpoint"));
            }
            else{
                this.setIcon(new ImageIcon(this.getClass().getResource("/logo/WhiteBreakpoint.png"),"No Breakpoint"));
                setOpaque(false);
            }

            return this;
        }
    }
}
