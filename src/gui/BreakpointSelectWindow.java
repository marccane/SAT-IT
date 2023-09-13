package gui;

import scala.Tuple2;
import scala.collection.Iterator;
import solver.Solver;
import util.Constants;
import javax.swing.*;
import java.awt.*;

class BreakpointSelectWindow extends JFrame {

    private Solver solver;

    BreakpointSelectWindow(MainWindow mainWindow, Solver solverInit){
        super("Breakpoints");
        this.setIconImages(Constants.getLogos());
        this.solver = solverInit;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());

        DefaultListModel<Tuple2<Integer, Boolean>> model = new DefaultListModel<>();
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
                Tuple2<Integer, Boolean> value = model.get(index1);
                Boolean isBreakpoint = !value._2();
                model.set(index1, new Tuple2<>(value._1(), isBreakpoint));
                if(isBreakpoint)
                    solver.addBreakpoints(value._1());
                else
                    solver.removeBreakpoints(value._1());
            }
        });

        JButton clearAll = new JButton("Clear All");
        clearAll.addActionListener(stub ->clearAll(mainWindow, this, model));
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
        mainWindow.setBreakpointSelectWindow(this);
    }

    private void initPairDefaultListModel(DefaultListModel<Tuple2<Integer, Boolean>> model) {
        Iterator<Tuple2<Object, Object>> it = solver.getAllVariablesBreakpoints().iterator();
        while (it.hasNext()){
            Tuple2<Object, Object> p = it.next();
            Tuple2<Integer, Boolean> p2 = new Tuple2<>((Integer)p._1(), (Boolean)p._2());
            model.addElement(p2);
        }
    }

    public void setSolver(Solver solver){
        this.solver = solver;
    }

    private void clearAll(MainWindow mainWindow, JFrame jframe, DefaultListModel<Tuple2<Integer, Boolean>> model){
        mainWindow.setEnableKeyBoard(false);
        int confirmed = JOptionPane.showConfirmDialog(jframe,
                "Are you sure you want to remove all breakpoints?", "Confirmation",
                JOptionPane.YES_NO_OPTION);
        if(confirmed == JOptionPane.YES_OPTION){
            solver.removeAllBreakpoints();
            model.clear();
            initPairDefaultListModel(model);
        }
        mainWindow.setEnableKeyBoard(true);
    }

    static class BreakpointListRender extends JLabel implements ListCellRenderer<Tuple2<Integer, Boolean>> {

        public Component getListCellRendererComponent(JList<? extends Tuple2<Integer, Boolean>> list, Tuple2<Integer, Boolean> value, int index, boolean isSelected, boolean cellHasFocus) {
            this.setHorizontalTextPosition(JLabel.RIGHT);
            this.setText(value._1().toString());
            this.setFont(new Font(this.getFont().getName(), this.getFont().getStyle(), 14));
            this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            if(value._2()){
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
