package gui;

import event.BackjumpEvent;
import scala.Enumeration;
import solver.*;
import structure.Clause;
import structure.ClauseWrapper;
import structure.Instance;
import scala.collection.Iterator;
import event.Event;
import structure.enumeration.Reason;
import structure.enumeration.SolvingState;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;

import static gui.EventHandler.*;
import static gui.ClauseListLiteralHighlight.createListRenderer;
import static util.Constants.*;

public class MainGUI extends JFrame{

    static final String VERSION = "0.3141592";

    public MainGUI() {
        super("SAT-IT");
        handleParametersAndInit("", null);
    }

    public MainGUI(String filename){
        super("SAT-IT");
        handleParametersAndInit(filename, null);
    }

    public MainGUI(String filename, SolverType st){
        super("SAT-IT");
        handleParametersAndInit(filename, st);
    }

    private void handleParametersAndInit(String filename, SolverType solverType){
        Instance instance = new Instance();
        if(!filename.isEmpty())
            instance.readDimacs(filename);
        if(solverType!=null)
            this.solverType = solverType;
        initGUI(instance);
    }

    //temp vars per timetravel (per fer una copia parcial del trail, unused)
    StyledDocument testDoc;
    boolean testCopiaFeta = false;
    int specCount;

    //Aixo s'ha de cridar NOMES una vegada (quan inicialitzem la gui)
    private void initGUI(Instance instance){
        initComponents();
        setEventHandlers();
        addStylesToDocument(textPaneTrail.getStyledDocument());

        loadInstance(instance);
    }

    private void loadInstance(Instance instance){
        //Netejar GUI
        textPaneTrail.setText("");
        ((DefaultListModel<Event>) listEvents.getModel()).clear();
        ((DefaultListModel<Clause>) listClauses.getModel()).clear();

        //Inicialitzar variables
        solver = newSolver();
        if(manualDecision) solver.setDecisionCallback(manualDecisionCallback);
        oldTrailLength = 0;
        lastInstance = instance;
        instanceLoaded = instance.numVariables() != 0;
        TWLsolver = solver instanceof TwoWatchedLiteralSolver;
        boolean doInitialUnitProp = !(solver instanceof Backtracking);

        solver.initSolverForGUI(instance, doInitialUnitProp);

        //Desactivar boto Unit Propagation si el solver es backtracking
        if(!TWLsolver)
            btnUnitProp.setEnabled(false);
        else
            btnUnitProp.setEnabled(true);

        try{
            updateTrail();
            trailAddFinishText(solver.solverState(),textPaneTrail.getStyledDocument());
            updateClauses();
            updateEvents();
        }
        catch(Exception e){e.printStackTrace();}
    }

    //Netejar i reiniciar la instancia actual
    void resetGUI(){
        loadInstance(lastInstance);
    }

    private void setEventHandlers(){
        listEvents.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                eventListMouseClick(evt, solver);
            }
        });

        btnDecision.addActionListener(stub -> btnDecisionActionHandler(solver,this));
        btnUnitProp.addActionListener(stub -> btnUnitPropActionHandler(solver,this));
        btnConflict.addActionListener(stub -> btnConflictActionHandler(solver,this));
        btnEnd.addActionListener(stub -> btnEndActionHandler(solver,this));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if(instanceLoaded) {
                    int confirmed = JOptionPane.showConfirmDialog(jPanel,
                            "Are you sure you want to exit the program?", "Confirmation",
                            JOptionPane.YES_NO_OPTION);

                    if (confirmed == JOptionPane.YES_OPTION) {
                        //dispose();
                        System.exit(0);
                    }
                }
                else{
                    System.exit(0);
                }
            }
        });
    }

    private void addStylesToDocument(StyledDocument doc){
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        //StyleConstants.setFontFamily(def, "SansSerif");
        doc.addStyle("default", def);
        doc.addStyle("propagationLiteral", def);

        Style redBold = doc.addStyle("fail", def);
        StyleConstants.setBold(redBold, true);
        StyleConstants.setForeground(redBold, Color.red);

        Style blackBold = doc.addStyle("backtrack", redBold);
        StyleConstants.setForeground(blackBold, Color.black);

        Style orangeBold = doc.addStyle("learned", redBold);
        StyleConstants.setForeground(orangeBold, Color.orange);

        Style greenBold = doc.addStyle("sat", redBold);
        StyleConstants.setForeground(greenBold, Color.green);

        Style blueBold = doc.addStyle("decisionLiteral", redBold);
        StyleConstants.setForeground(blueBold, Color.blue);

        Style cyanBold = doc.addStyle("backtrackLiteral", blueBold);
        StyleConstants.setForeground(cyanBold, new Color(255, 183, 0));

        Style superscript = doc.addStyle("superscript", def);
        StyleConstants.setSuperscript(superscript, true);
    }

    private void initComponents(){
        GridBagConstraints c;
        jPanel = new JPanel(new GridBagLayout());

        // Regions principals
        // __________________
        // |    1     |      |
        // |__________|      |
        // |     |    |  2   |
        // |  3  | 4  |      |
        // |     |    |      |
        // |_____|____|______|

        //1
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 2;
        c.ipadx = 100; // dc
        c.ipady = 200;
        c.weightx = 1; // 0.5
        c.weighty = 0.04; // 0
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        textPaneTrail = new JTextPane();
        textPaneTrail.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPaneTrail);
        //cp.add(scrollPane, BorderLayout.CENTER);
        jPanel.add(scrollPane, c);

        //2
        listEvents = new JList<>(new DefaultListModel<>());
        listEvents.setCellRenderer(createEventsListRenderer());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        c.ipadx = 0;
        c.weightx = 0.4;

        JScrollPane sp = new JScrollPane();
        sp.setViewportView(listEvents);
        JPanel rightPanel = new JPanel(new GridBagLayout());
        {
            GridBagConstraints c2 = new GridBagConstraints();
            c2.gridx = 0;
            c2.gridy = 0;
            c2.gridheight = 1;
            c2.gridwidth = 4;
            c2.weightx = 1;
            c2.weighty = 0.8;
            c2.fill = GridBagConstraints.BOTH;
            c2.anchor = GridBagConstraints.NORTH;
            rightPanel.add(sp, c2);

            c2 = new GridBagConstraints();
            btnDecision = new JButton("Decision");
            c2.gridx = 0;
            c2.gridy = 1;
            c2.gridheight = 1;
            c2.gridwidth = 1;
            c2.weightx = 0.1;
            c2.weighty = 0.1;
            c2.fill = GridBagConstraints.BOTH;
            rightPanel.add(btnDecision,c2);

            c2 = new GridBagConstraints();
            btnUnitProp = new JButton("Unit Prop.");
            c2.gridx = 1;
            c2.gridy = 1;
            c2.gridheight = 1;
            c2.gridwidth = 1;
            c2.weightx = 0.1;
            c2.fill = GridBagConstraints.BOTH;
            rightPanel.add(btnUnitProp,c2);

            c2 = new GridBagConstraints();
            btnConflict = new JButton("Conflict");
            c2.gridx = 2;
            c2.gridy = 1;
            c2.gridheight = 1;
            c2.gridwidth = 1;
            c2.weightx = 0.1;
            c2.fill = GridBagConstraints.BOTH;
            rightPanel.add(btnConflict,c2);

            c2 = new GridBagConstraints();
            btnEnd = new JButton("End");
            c2.gridx = 3;
            c2.gridy = 1;
            c2.gridheight = 1;
            c2.gridwidth = 1;
            c2.weightx = 0.1;
            c2.fill = GridBagConstraints.BOTH;
            rightPanel.add(btnEnd,c2);
        }
        jPanel.add(rightPanel,c);

        //3
        listClauses = new JList<>(new DefaultListModel<>());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.ipadx = 250;
        //c.ipady = 100;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.5;
        jPanel.add(new JScrollPane(listClauses),c);

        initJMenuBar();

        //---------------------------------------------
        setSize(1280,900); //amplada, altura
        //pack();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        add(jPanel);
        setResizable(true);
        setVisible(true);
    }

    private void initJMenuBar(){
        JMenuBar jMenuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(stub -> {
            JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
            //JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

            jfc.setDialogTitle("Select an instance");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("DIMACS CNF (.cnf, .dimacs)", "cnf", "dimacs");
            jfc.addChoosableFileFilter(filter);
            jfc.removeChoosableFileFilter(jfc.getAcceptAllFileFilter());
            jfc.addChoosableFileFilter(jfc.getAcceptAllFileFilter());

            boolean loadInstance = true;
            int returnVal = jfc.showOpenDialog(this);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                File file = jfc.getSelectedFile();
                if(instanceLoaded){
                    int confirmed = JOptionPane.showConfirmDialog(this,
                            confirmResetMessage, "Confirmation", JOptionPane.YES_NO_OPTION);

                    if(confirmed == JOptionPane.NO_OPTION)
                        loadInstance = false;
                }

                if(loadInstance){
                    Instance instance = new Instance();
                    instance.readDimacs(file);
                    loadInstance(instance);
                }
            }
        });

        JMenuItem resetMenuItem = new JMenuItem("Reset");
        resetMenuItem.addActionListener(stub -> {
                    int confirmed = JOptionPane.showConfirmDialog(this, confirmResetMessage, "Confirmation",
                            JOptionPane.YES_NO_OPTION);

                    if (confirmed == JOptionPane.YES_OPTION)
                        resetGUI();
                }
        );

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(stub -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        fileMenu.add(openMenuItem);
        fileMenu.add(resetMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu optionsMenu = new JMenu("Options");

        JMenuItem solverMenuItem = new JMenuItem("Change solver");
        solverMenuItem.addActionListener(stub -> new SolverSelectorWindow(this,solver));
        optionsMenu.add(solverMenuItem);

        JCheckBoxMenuItem manualDecisionsCheckBoxMenuItem = new JCheckBoxMenuItem("Manual Decisions", manualDecision);
        manualDecisionsCheckBoxMenuItem.addItemListener(evt -> {
            //System.out.println(evt.getStateChange() == ItemEvent.SELECTED ? "SEL" : "NOT SEL");
            //JCheckBoxMenuItem item = (JCheckBoxMenuItem)evt.getItem();
            if(evt.getStateChange() == ItemEvent.SELECTED){
                manualDecision = true;
                solver.setDecisionCallback(manualDecisionCallback);
            } else {
                manualDecision = false;
                solver.resetDecisionCallback();
            }
        });
        optionsMenu.add(manualDecisionsCheckBoxMenuItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem cliMenuItem = new JMenuItem("CLI");

        cliMenuItem.addActionListener(stub -> JOptionPane.showMessageDialog(this,
                "Run this executable with\njava -jar SAT-IT.jar -h\nto find more about the command line interface ",
                "CLI", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(cliMenuItem);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(stub -> JOptionPane.showMessageDialog(this,
                "SAT-IT " + MainGUI.VERSION + "\n\nAuthor: Marc Cané Salamià\nTutors: Mateu Villaret & Jordi Coll",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutMenuItem);

        jMenuBar.add(fileMenu);
        jMenuBar.add(optionsMenu);
        jMenuBar.add(helpMenu);

        setJMenuBar(jMenuBar);
    }

    private final DecisionCallback manualDecisionCallback = () -> { //potser hauriem de passar el solver com a parametre?
        String response;
        int literal = 0, automaticResponse = this.solver.initialMakeDecision(); //no tinc ni la mes remota idea de perque s'ha de posar el this...
        updateClauses(); //per tenir les clausules act. al moment de decidir. testejar
        boolean decisionMade = false;
        while(!decisionMade){
            boolean error = false;
            response = (String)JOptionPane.showInputDialog(
                    this,"Choose a decision literal","Manual Decision",
                    JOptionPane.PLAIN_MESSAGE,null,null,automaticResponse);

            if (response == null || response.isEmpty()) { //ha clicat cancel o ha entrat una string buida
                JOptionPane.showMessageDialog(this, "Literal " + automaticResponse +
                        " will be decided","Info", JOptionPane.INFORMATION_MESSAGE);
                literal = automaticResponse;
                decisionMade = true;
            } else {
                response = response.trim();
                try {
                    literal = Integer.parseInt(response);
                    if(!this.solver.validDecisionLiteral(literal)) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid variable or already assigned","Error", JOptionPane.ERROR_MESSAGE);
                        error = true;
                    }
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "You must enter an integer number",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    error = true;
                }
            }

            if(!error)
                decisionMade = true;
        }

        if(literal==0){
            JOptionPane.showMessageDialog(this, "Something went wrong, literal = 0",
                    "Internal Error", JOptionPane.ERROR_MESSAGE);
        }
        return literal;
    };

    int solverStep() {
        int solverAction = solver.guiSolveStep();
        int actualTrailLength = solver.trail().length();
        StyledDocument doc = textPaneTrail.getStyledDocument();

        //actualitzar trailTextPane
        try{
            if(solverAction==SOLVER_CONFLICT){
                updateTrail();
                doc.insertString(doc.getLength(), "    FAIL "+solver.guiConflClause()+",  ", doc.getStyle("fail"));
            }
            else if(solverAction==SOLVER_BACKJUMP){
                doc.insertString(doc.getLength(), "LEARNED "+(solver.clauses().getNumClauses()-1)+"\n\n", doc.getStyle("learned"));

                //escriure trail despres el backjump
                for(int i=0; i<actualTrailLength; i++)
                    trailAddLiteral(i);
            }
            else if(solverAction==SOLVER_BACKTRACK){
                doc.insertString(doc.getLength(), "BACKTRACK\n\n", doc.getStyle("backtrack"));

                //escriure trail despres el backtrack
                for(int i=0; i<actualTrailLength; i++)
                    trailAddLiteral(i);
            }
            else if(solverAction==SOLVER_END){
                trailAddFinishText(solver.solverState(),doc);
                if(solver.solverState()==SolvingState.SAT() && !solver.checkSolution())
                    JOptionPane.showMessageDialog(this, "Internal error: incorrect or incomplete solution",
                            "Error", JOptionPane.ERROR_MESSAGE);
            }
            else{ //decisio o unitprop o conflicte(up)
                //afegir nous literals
                if(actualTrailLength > oldTrailLength){
                    for (int i = oldTrailLength; i < actualTrailLength; i++)
                        trailAddLiteral(i);
                }
            }
            oldTrailLength = actualTrailLength;
        }
        catch(Exception e){e.printStackTrace();}

        return solverAction;
    }

    private void updateTrail(){
        int actualTrailLength = solver.trail().length();

        try {
            //afegir nous literals
            if(actualTrailLength > oldTrailLength){
                for (int i = oldTrailLength; i < actualTrailLength; i++)
                    trailAddLiteral(i);
                oldTrailLength = actualTrailLength;
            }
        }
        catch(Exception e){e.printStackTrace();}
    }

    private void trailAddLiteral(int litIdx) throws javax.swing.text.BadLocationException{

        int lit = solver.getTrailIndex(litIdx);
        StyledDocument doc = textPaneTrail.getStyledDocument();

        if(TWLsolver) { //cdcl i dpll

            TwoWatchedLiteralSolver wlSolver = (TwoWatchedLiteralSolver) solver;
            Integer propagator = wlSolver.getWhoPropagated(lit);

            if (propagator == -5) { //backtrack
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("backtrackLiteral"));
                doc.insertString(doc.getLength(), "BK", doc.getStyle("superscript"));
            } else if (propagator == -2) { //unitpropinicial
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("propagationLiteral"));
                doc.insertString(doc.getLength(), "UP", doc.getStyle("superscript"));
            } else if (propagator < 0) { //decisio
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("decisionLiteral"));
                doc.insertString(doc.getLength(), "D", doc.getStyle("superscript"));
            } else { //unitprop
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("propagationLiteral"));
                doc.insertString(doc.getLength(), propagator.toString(), doc.getStyle("superscript"));
            }
            doc.insertString(doc.getLength(), " ", doc.getStyle("default"));
        }
        else{ //backtracking
            Integer atom = Math.abs(lit);
            if(solver.assignmentReason().apply(atom)== Reason.DECISION()){
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("decisionLiteral"));
                doc.insertString(doc.getLength(), "D", doc.getStyle("superscript"));
            }
            else{ //backtrack
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("backtrackLiteral"));
                doc.insertString(doc.getLength(), "BK", doc.getStyle("superscript"));

            }
            doc.insertString(doc.getLength(), " ", doc.getStyle("default"));
        }
    }

    private void updateClauses(){
        DefaultListModel<Clause> lmClauses = (DefaultListModel<Clause>) listClauses.getModel();
        lmClauses.clear();

        java.util.List<Clause> clauses = Arrays.asList(solver.clauses().getClauseList());
        clauses.forEach(lmClauses::addElement);
        //FIXME (la gracia seria nomes setejarho una vegada)
        listClauses.setCellRenderer(createListRenderer(solver.variablesState(),solver.clauses().numInitialClauses()));
    }

    private void updateEvents(){ //todo millorar: no cal tornar a escriure tots els events
        Iterator<Event> eventIt = solver.eventManager().events().iterator();
        DefaultListModel<Event> lmEvents = (DefaultListModel<Event>) listEvents.getModel();
        lmEvents.clear();
        while(eventIt.hasNext()) lmEvents.addElement(eventIt.next());
        listEvents.ensureIndexIsVisible(lmEvents.size()-1); //autoscroll a l'ultim event cada cop que n'afegim un
    }

    void updateGUI(){
        updateClauses();
        updateEvents();
    }

    private void trailAddFinishText(Enumeration.Value solverState, StyledDocument doc) throws javax.swing.text.BadLocationException{
        if(solverState==SolvingState.SAT() && solver.numVariables()!=0){
            doc.insertString(doc.getLength(), " SAT", doc.getStyle("sat"));
        }
        else if(solverState==SolvingState.UNSAT() && solver.numVariables()!=0){
            doc.insertString(doc.getLength(), " UNSAT", doc.getStyle("fail"));
        }
    }

    JPanel getjPanel(){
        return jPanel;
    }
    
    void focusOnConflictClause(){
        listClauses.ensureIndexIsVisible(solver.guiConflClause());
    }

    private ViewableSolver newSolver(){
        if(solverType==SolverType.CDCL)
            return new CDCL();
        else if(solverType==SolverType.DPLL)
            return new DPLL();
        else if(solverType==SolverType.Backtracking)
            return new Backtracking();
        else return null;
    }

    void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    //EventList cell renderer
    private static ListCellRenderer<? super Event> createEventsListRenderer() {
        final Color darkOrange = new Color(255,128,0);
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                //this.setFont(this.getFont().deriveFont(14f).deriveFont(Font.PLAIN));

                Event event = (Event) value;
                setText(event.toString());

                if(event instanceof BackjumpEvent)
                    setForeground(darkOrange);

                return this;
            }
        };
    }

    public enum SolverType{
        Backtracking, DPLL, CDCL
    }

    //Guarda les dades per poder retrocedir a un estat anterior del solver (unused)
    private class TimeTravelData{
        ClauseWrapper clauseWrapper;
        int numEvents;
        int specIndex;
    }

    private SolverType solverType = SolverType.CDCL;
    boolean instanceLoaded = false;
    private boolean manualDecision = false;

    //Attributs d'Swing
    private JPanel jPanel;
    private JTextPane textPaneTrail;
    private JList<Event> listEvents;
    private JList<Clause> listClauses;
    private JButton btnDecision;
    private JButton btnUnitProp;
    private JButton btnConflict;
    private JButton btnEnd;

    //Atributs del solver
    private Instance lastInstance;
    private ViewableSolver solver;
    private boolean TWLsolver;
    private int oldTrailLength;

    static final String confirmResetMessage = "You will lose the solving progress. Are you sure?";

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(MainGUI::new);
    }

}
