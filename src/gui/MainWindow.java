package gui;

import event.BackjumpEvent;
import event.BacktrackEvent;
import event.DecisionEvent;
import scala.Enumeration;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.ListBuffer;
import solver.*;
import structure.*;
import scala.collection.Iterator;
import event.Event;
import structure.enumeration.Reason;
import structure.enumeration.SolvingState;
import util.Constants;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import static gui.EventHandler.*;
import static gui.ClauseListLiteralHighlight.createListRenderer;
import static util.Constants.*;

public class MainWindow extends JFrame{

    public MainWindow() {
        super(APP_NAME);
        this.setFocusTraversalKeysEnabled(false);
        handleParametersAndInit("", null);
    }

    public MainWindow(String cnfFilePath){
        super();
        this.setTitle(getTitle(cnfFilePath));
        handleParametersAndInit(cnfFilePath, null);
    }

    public MainWindow(String cnfFilePath, SolverType st){
        super();
        this.setTitle(getTitle(cnfFilePath));
        handleParametersAndInit(cnfFilePath, st);
    }

    private String getTitle(String cnfFilePath)
    {
        String[] cnfFilePathSplit = cnfFilePath.split(File.separator);
        String cnfFilename = cnfFilePathSplit[cnfFilePathSplit.length-1];
        return APP_NAME + " " + solverType + " " + cnfFilename;
    }

    //Aixo s'ha de cridar NOMES una vegada (quan inicialitzem la gui)
    private void handleParametersAndInit(String filename, SolverType solverType){
        Instance instance = new Instance();
        if(!filename.isEmpty())
            instance = loadInstanceFromFile(new File(filename));
        if(solverType!=null)
            this.solverType = solverType;

        initComponents();
        setEventHandlers();
        styles.addStylesToDocument(textPaneTrail.getStyledDocument());

        loadInstanceGUI(instance);
    }

    private void loadInstanceGUI(Instance instance){
        //Netejar GUI
        LitScoreSet litScoreSet = null;
        if(solverType == SolverType.CDCL_VSIDS && solver instanceof CDCL) litScoreSet = ((CDCL) solver).scoreLitQueue();
        if(vsidsScoreWindow != null && vsidsScoreWindow.isVisible() && (historySolver == null || !historySolver.inHistory()))
            vsidsScoreWindow.dispose();
        if(breakpointSelectWindow != null && breakpointSelectWindow.isVisible() && (historySolver == null || !historySolver.inHistory()))
            breakpointSelectWindow.dispose();
        textPaneTrail.setText("");
        ((DefaultListModel<Event>) listEvents.getModel()).clear();
        ((DefaultListModel<Clause>) listClauses.getModel()).clear();

        //Inicialitzar variables
        solver = newSolver();
        if(historySolver == null) historySolver = new HistorySolver();
        if(solverType == SolverType.CDCL_VSIDS && solver != null && litScoreSet != null){
            ((CDCL) solver).setScoreLitQueue(litScoreSet);
            if(vsidsScoreWindow != null) vsidsScoreWindow.setCellRenderList((CDCL) solver);
        }
        if(!historySolver.getInHistory())historySolver.init();
        if(manualDecisionOption) solver.setDecisionCallback(manualDecisionCallback);
        oldTrailLength = 0;
        lastInstance = instance;
        instanceLoaded = instance.numVariables() != 0;
        TWLsolver = solver instanceof TwoWatchedLiteralSolver;
        boolean doInitialUnitProp = !(solver instanceof Backtracking);
        vsidsProperty.resetAddScore();
        solver.setVSIDSProperty(vsidsProperty);
        solver.initSolverForGUI(instance, doInitialUnitProp);

        //Desactivar boto Unit Propagation si el solver es backtracking
        if(!TWLsolver)
            btnUnitProp.setEnabled(false);
        else
            btnUnitProp.setEnabled(true);

        initButtons();
        try{
            updateTrail();
            trailAddFinishText(solver.solverState(),textPaneTrail.getStyledDocument());
            updateClauses();
            updateEvents();
            //Afegim l'historial al solver
            solver.setHistorySolver(historySolver);
        } catch(Exception e){
            JOptionPane.showMessageDialog(this,"Unknown exception: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addTextAreaToJPanel(JPanel jPanel, String title, String text){
        JTextArea textArea = new JTextArea(10, 50);
        textArea.setText(text);
        textArea.setEditable(false);

        JScrollPane scrollPanel = new JScrollPane(textArea);
        scrollPanel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);

        jPanel.add(titleLabel);
        jPanel.add(scrollPanel);
        jPanel.add(new JLabel("\n"));
    }

    private Instance loadInstanceFromFile(File file){
        Instance instance = new Instance();
        try {
            instance.readDimacs(file);
        } catch(FileNotFoundException e){
            JOptionPane.showMessageDialog(this,"File not found","Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch(Exception e){
            JOptionPane.showMessageDialog(this,"Unknown exception: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        if(instance.numErrors() != 0 || instance.numWarnings() != 0){
            JPanel errorsPanel = new JPanel();
            errorsPanel.setLayout(new BoxLayout(errorsPanel, BoxLayout.Y_AXIS));

            if(instance.numErrors() != 0)
                addTextAreaToJPanel(errorsPanel, "Errors:", instance.getErrorsText());
            if(instance.numWarnings() != 0)
                addTextAreaToJPanel(errorsPanel, "Warnings:", instance.getWarningsText());

            JOptionPane.showMessageDialog(this, errorsPanel, instance.numErrors() != 0 ? "Errors" : "Warnings", instance.numErrors() != 0 ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
        }
        this.setTitle(getTitle(file.getPath()));
        return instance;
    }

    //Netejar i reiniciar la instancia actual
    void resetGUI(){
        loadInstanceGUI(lastInstance);
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
        btnResolve.addActionListener(stub -> btnResolveActionHandler(solver,this));
        btnEnd.addActionListener(stub -> btnEndActionHandler(solver,this));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if(instanceLoaded) {
                    int confirmed = JOptionPane.showConfirmDialog(jPanel,
                            "Are you sure you want to exit the program?", "Confirmation",
                            JOptionPane.YES_NO_OPTION);

                    if (confirmed == JOptionPane.YES_OPTION)
                        System.exit(0);
                }
                else
                    System.exit(0);
            }
        });
    }

    private void initComponents(){
        GridBagConstraints c;
        jPanel = new JPanel(new GridBagLayout());

        this.setIconImages(Constants.getLogos());


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
            c2.gridwidth = 5;
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
            btnResolve= new JButton("Resolve C.");
            c2.gridx = 3;
            c2.gridy = 1;
            c2.gridheight = 1;
            c2.gridwidth = 1;
            c2.weightx = 0.1;
            c2.fill = GridBagConstraints.BOTH;
            rightPanel.add(btnResolve,c2);

            c2 = new GridBagConstraints();
            btnEnd = new JButton("End");
            c2.gridx = 4;
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
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.5;
        jPanel.add(new JScrollPane(listClauses),c);

        initJMenuBar();

        //---------------------------------------------
        setSize((int) (Constants.getWidth()*0.7), (int) (Constants.getHeight()*0.85)); //amplada, altura
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        add(jPanel);
        setResizable(true);
        setVisible(true);
        initGlobalKey();
    }

    private void initGlobalKey() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && enableKeyboard) {
                if ((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) || e.getKeyCode() == KeyEvent.VK_LEFT) {
                    undoRedoFunction(UNDO);
                }
                else if (solver.solverState() != SolvingState.SAT() && ((e.getKeyCode() == KeyEvent.VK_Y) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
                    undoRedoFunction(REDO);
                }
                else if( e.getKeyCode() == KeyEvent.VK_M){
                    manualDecisionsCheckBoxMenuItem.setSelected(!manualDecisionOption);
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {

            } else if (e.getID() == KeyEvent.KEY_TYPED) {

            }
            return false;
        });
    }

    void initButtons(){
        btnDecision.setEnabled(true);

        if(!TWLsolver)
            btnUnitProp.setEnabled(false);
        else
            btnUnitProp.setEnabled(true);

        btnConflict.setEnabled(true);
        btnResolve.setEnabled(false);
        btnEnd.setEnabled(true);
    }

    private void undoRedoFunction(boolean isUndo){
        historySolver.setInHistory(true);
        HashSet<Object> breakpoints = solver.getBreakpoints();
        if(isUndo) historySolver.undo();
        else historySolver.redo();
        loadInstanceGUI(lastInstance);
        solver.setDecisionCallback(historySolver);
        solver.setBreakpoints(breakpoints);
        ListBuffer<Object> t = historySolver.getHistoryTrailClone();
        int[] actions = historySolver.getHistorAction();
        int last = -1;
        int actual = -1;
        for(int i = 0; i < historySolver.getNumH(); i++){
            solverStep();
            last = actual;
            actual = actions[i];
        }
        updateGUI();
        updateButtons(last, actual);
        historySolver.setHistoryTrail(t);
        historySolver.setInHistory(false);
        solver.resetDecisionCallback();
        solver.removeDetectedBreakpoints();
        if(manualDecisionOption) solver.setDecisionCallback(manualDecisionCallback);
        if(breakpointSelectWindow != null && breakpointSelectWindow.isVisible()) breakpointSelectWindow.setSolver(solver);
    }

    private void initJMenuBar(){
        JMenuBar jMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

            JMenuItem undoMenuItem = new JMenuItem("<html><body>Undo&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: gray;\">(Ctrl+Z or &#8592;)</span></body></html>");
            undoMenuItem.addActionListener(stub -> undoRedoFunction(UNDO));

            JMenuItem redoMenuItem  = new JMenuItem("<html><body>Redo&nbsp;&nbsp;&nbsp;&nbsp;<span style = \"color: gray;\">(Ctrl+Y or &#8594;)</span></body></html>");
            redoMenuItem.addActionListener(stub ->{
                if (solver.solverState() != SolvingState.SAT())
                    undoRedoFunction(REDO);
            });
            JMenuItem openMenuItem = new JMenuItem("Open");
            openMenuItem.addActionListener(stub -> {
                JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));

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

                    if(loadInstance)
                        loadInstanceGUI(loadInstanceFromFile(file));
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
        fileMenu.add(redoMenuItem);
        fileMenu.add(undoMenuItem);
        fileMenu.add(resetMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu optionsMenu = new JMenu("Options");

            JMenuItem solverMenuItem = new JMenuItem("Change solver");
            solverMenuItem.addActionListener(stub -> new SolverSelectorWindow(this,solver));

            manualDecisionsCheckBoxMenuItem = new JCheckBoxMenuItem("<html><body>Manual Decisions&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: gray;\">(M)</span></body></html>", manualDecisionOption);
            manualDecisionsCheckBoxMenuItem.addItemListener(evt -> manualFunction());

            JMenuItem vsidsParameters = new JMenuItem("VSIDS parameters");
            vsidsParameters.addActionListener(evt -> new VsidsOptionsWindow(this));

            JCheckBoxMenuItem focusOnConflictClauseCheckBoxMenuItem = new JCheckBoxMenuItem("Auto-focus on conflict clause", autoFocusOnConflictOption);
            focusOnConflictClauseCheckBoxMenuItem.addItemListener(evt -> {
                autoFocusOnConflictOption = !autoFocusOnConflictOption;
            });

            JCheckBoxMenuItem focusOnLearnedClauseCheckBoxMenuItem = new JCheckBoxMenuItem("Auto-focus on learned clause", autoFocusOnLearnedOption);
            focusOnLearnedClauseCheckBoxMenuItem.addItemListener(evt -> {
                autoFocusOnLearnedOption = !autoFocusOnLearnedOption;
            });

        optionsMenu.add(solverMenuItem);
        optionsMenu.add(vsidsParameters);
        optionsMenu.add(manualDecisionsCheckBoxMenuItem);
        optionsMenu.addSeparator();
        optionsMenu.add(focusOnConflictClauseCheckBoxMenuItem);
        optionsMenu.add(focusOnLearnedClauseCheckBoxMenuItem);

        JMenu helpMenu = new JMenu("Help");

            JMenuItem cliMenuItem = new JMenuItem("CLI");
            cliMenuItem.addActionListener(stub -> JOptionPane.showMessageDialog(this,
                    "Run this executable with\njava -jar SAT-IT.jar -h\nto find more about the command line interface ",
                    "CLI", JOptionPane.INFORMATION_MESSAGE));
            helpMenu.add(cliMenuItem);

            JMenuItem aboutMenuItem = new JMenuItem("About");
            aboutMenuItem.addActionListener(stub -> JOptionPane.showMessageDialog(this,
                    "SAT-IT " + MainWindow.VERSION + "\n\nAuthors: Marc Cané Salamià & Marc Rojo Campillos\nTutors: Mateu Villaret & Jordi Coll",
                    "About", JOptionPane.INFORMATION_MESSAGE));
            helpMenu.add(aboutMenuItem);

        JMenu viewMenu = new JMenu("View");
        viewScoreList = new JMenuItem("Score List");
        viewScoreList.addActionListener(stub -> new VsidsScoreWindow(this, (CDCL) solver, getTitle()));

        MainWindow mainWindow = this;
        class MouseAdapterBreakPoint extends MouseAdapter{
            public void mouseClicked(MouseEvent evt) {
                boolean notVisible = breakpointSelectWindow == null || !breakpointSelectWindow.isVisible();
                if (evt.getClickCount() == 1 && notVisible){
                    new BreakpointSelectWindow(mainWindow, solver);
                }
                else if(! notVisible){
                    breakpointSelectWindow.toFront();
                }
            }
        }

        JMenu breakPoint = new JMenu("Breakpoints");
        breakPoint.addMouseListener(new  MouseAdapterBreakPoint());

        viewMenu.add(viewScoreList);
        if(solverType != SolverType.CDCL_VSIDS) viewScoreList.setEnabled(false);
        jMenuBar.add(fileMenu);
        jMenuBar.add(optionsMenu);
        jMenuBar.add(breakPoint);
        jMenuBar.add(viewMenu);
        jMenuBar.add(helpMenu);

        setJMenuBar(jMenuBar);
    }

    private final DecisionCallback manualDecisionCallback = () -> { //potser hauriem de passar el solver com a parametre?
        setEnableKeyBoard(false);
        String response;
        int literal = 0, automaticResponse;
        if(this.solverType == SolverType.CDCL_VSIDS) automaticResponse =  this.solver.initialMakeDecisionVSIDS();
        else  automaticResponse = this.solver.initialMakeDecision(); //no tinc ni la mes remota idea de perque s'ha de posar el this...
        updateClauses(); //per tenir les clausules act. al moment de decidir
        boolean decisionMade = false;
        while(!decisionMade){
            boolean error = false;
            response = (String)JOptionPane.showInputDialog(
                    this,"Choose a decision literal","Manual Decision",
                    JOptionPane.PLAIN_MESSAGE,null,null,automaticResponse);

            if (response == null || response.isEmpty()) { //ha clicat cancel o ha entrat una string buida
                this.solver.setCancel(true);
                setEnableKeyBoard(true);
                return CANCEL_DECISION;
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
        setEnableKeyBoard(true);
        return literal;
    };

    int solverStep() {
        int solverAction = solver.guiSolveStep();
        if(solverAction != CANCEL_DECISION) historySolver.addAction(solverAction);
        int actualTrailLength = solver.trail().length();
        StyledDocument doc = textPaneTrail.getStyledDocument();

        //actualitzar trailTextPane
        try{
            if(solverAction==SOLVER_CONFLICT){
                updateTrail();
                doc.insertString(doc.getLength(), "    CONFLICT "+solver.guiConflClause()+",  ", doc.getStyle("fail"));
            }
            else if(solverAction==SOLVER_BACKJUMP){
                doc.insertString(doc.getLength(), "LEARNED "+(solver.clauses().getNumClauses()-1)+"\n\n", doc.getStyle("learned"));
                styles.setBlurredStyle();
                //escriure trail despres el backjump
                for(int i=0; i<actualTrailLength - 1; i++)
                    trailAddLiteral(i);
                styles.setClearStyle();
                trailAddLiteral(actualTrailLength- 1);
            }
            else if(solverAction==SOLVER_BACKTRACK){
                doc.insertString(doc.getLength(), "BACKTRACK\n\n", doc.getStyle("backtrack"));
                styles.setBlurredStyle();
                //escriure trail despres el backtrack
                for(int i=0; i<actualTrailLength - 1; i++)
                    trailAddLiteral(i);
                styles.setClearStyle();
                trailAddLiteral(actualTrailLength - 1);
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
        boolean breakpoint = solver.isBreakpoint(Math.abs(lit));
        styles.setHighlightStyle(breakpoint);

        if(TWLsolver) { //cdcl i dpll
            TwoWatchedLiteralSolver wlSolver = (TwoWatchedLiteralSolver) solver;
            Integer propagator = wlSolver.getWhoPropagated(lit);

            if (propagator == -5) { //backtrack
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("backtrackLiteral"));
                doc.insertString(doc.getLength(), "k", doc.getStyle("superscript"));
            } else if (propagator == -2) { //unitpropinicial
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("propagationLiteral"));
                doc.insertString(doc.getLength(), "p", doc.getStyle("superscript"));
            } else if (propagator < 0) { //decisio
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("decisionLiteral"));
                doc.insertString(doc.getLength(), "d", doc.getStyle("superscript"));
            } else if(propagator >= solver.getInitialClauses()){ //unitprop
                doc.insertString(doc.getLength(), Integer.toString(lit), doc.getStyle("propagationLiteralLearned"));
                doc.insertString(doc.getLength(), propagator.toString(), doc.getStyle("superscript"));
            }else { //unitprop
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
        styles.setHighlightStyle(false);
    }

    void updateGUI(){
        updateClauses();
        updateEvents();
    }

    private void updateClauses(){
        DefaultListModel<Clause> lmClauses = (DefaultListModel<Clause>) listClauses.getModel();
        lmClauses.clear();

        java.util.List<Clause> clauses = Arrays.asList(solver.clauses().getClauseList());
        clauses.forEach(lmClauses::addElement);
        //FIXME (la gracia seria nomes setejarho una vegada)
        listClauses.setCellRenderer(createListRenderer(solver.variablesState(),solver.clauses().numInitialClauses()));

        if(focusOnLearnedClauseDelay){
            focusOnLearnedClauseDelay = false;
            listClauses.ensureIndexIsVisible(listClauses.getModel().getSize()-1);
        }
    }

    private void updateEvents(){ //todo millorar: no cal tornar a escriure tots els events
        Iterator<Event> eventIt = solver.eventManager().events().iterator();
        DefaultListModel<Event> lmEvents = (DefaultListModel<Event>) listEvents.getModel();
        lmEvents.clear();
        while(eventIt.hasNext()) lmEvents.addElement(eventIt.next());
        listEvents.ensureIndexIsVisible(lmEvents.size()-1); //autoscroll a l'ultim event cada cop que n'afegim un
    }

    private void trailAddFinishText(Enumeration.Value solverState, StyledDocument doc) throws javax.swing.text.BadLocationException{
        if(solverState==SolvingState.SAT() && solver.numVariables()!=0){
            doc.insertString(doc.getLength(), " SAT", doc.getStyle("sat"));
        }
        else if(solverState==SolvingState.UNSAT() && solver.numVariables()!=0){
            doc.insertString(doc.getLength(), " UNSAT", doc.getStyle("fail"));
        }
    }

    JPanel getPanel(){
        return jPanel;
    }

    void focusOnConflictClause(){
        if(autoFocusOnConflictOption)
            listClauses.ensureIndexIsVisible(solver.guiConflClause());
    }

    void focusOnLearnedClause(){
        if(autoFocusOnLearnedOption)
            focusOnLearnedClauseDelay = true; //we can't do it right now because the new clause still hasn't been added to the gui's clause list
    }

    private ViewableSolver newSolver(){
        if(solverType==SolverType.CDCL)
            return new CDCL();
        else if(solverType==SolverType.DPLL)
            return new DPLL();
        else if(solverType==SolverType.Backtracking)
            return new Backtracking();
        else if(solverType==SolverType.CDCL_VSIDS){
            ViewableSolver vs = new CDCL();
            vs.setVsids(true);
            return vs;
        }
        else return null;
    }

    SolverType getSolverType(){
        return solverType;
    }

    void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    //EventList cell renderer
    private static ListCellRenderer<? super Event> createEventsListRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                Event event = (Event) value;
                setText(event.toString());

                if(event instanceof BacktrackEvent)
                    setForeground(Styles.darkYellow);
                else if(event instanceof DecisionEvent)
                    setForeground(Color.blue);
                else if(event instanceof BackjumpEvent)
                    setForeground(Styles.purple);

                return this;
            }
        };
    }

    void failAction(int solverAction)
    {
        if(solverAction == SOLVER_CONFLICT)
        {
            btnDecision.setEnabled(false);
            btnUnitProp.setEnabled(false);
            btnConflict.setEnabled(false);
            btnResolve.setEnabled(true);
        }
        else initButtons();
    }

    private void updateButtons(int last, int actual){
        boolean decision = true;
        boolean unitProp = true;
        boolean conflict = true;
        boolean resolveC = false;

        if(!isBacktracking() && last != SOLVER_END && (Arrays.asList(SOLVER_DECISION, SOLVER_BACKTRACK).contains(actual))){
            decision = false;
        } else if(!isBacktracking() && last == SOLVER_CONFLICT && actual == SOLVER_BACKJUMP){
            decision = false;
        } else if(last != SOLVER_END && actual == SOLVER_CONFLICT){
            decision = false;
            unitProp = false;
            conflict = false;
            resolveC = true;
        } else if(isBacktracking()){
            unitProp = false;
        }

        btnDecision.setEnabled(decision);
        btnUnitProp.setEnabled(unitProp);
        btnConflict.setEnabled(conflict);
        btnResolve.setEnabled(resolveC);
        btnEnd.setEnabled(true);
    }

    private void manualFunction(){
        if(!manualDecisionOption){
            manualDecisionOption = true;
            solver.setDecisionCallback(manualDecisionCallback);
        } else {
            manualDecisionOption = false;
            solver.resetDecisionCallback();
        }
    }

    void setDecision(boolean enabled)
    {
        btnDecision.setEnabled(enabled);
    }

    void setBreakpointSelectWindow(BreakpointSelectWindow breakpointSelectWindow){
        this.breakpointSelectWindow = breakpointSelectWindow;
    }

    public boolean isBacktracking()
    {
        return !TWLsolver;
    }

    void changeSolverTitle()
    {
        String title = this.getTitle();
        title = title.replaceFirst(APP_NAME + " ", "");
        title = title.replaceFirst(".* ", "");
        this.setTitle(APP_NAME + " " + solverType + " " + title);
    }

    void setEnableKeyBoard(boolean enable) {
        enableKeyboard = enable;
    }

    void setViewScoreListEnable(boolean enable){
        viewScoreList.setEnabled(enable);
    }

    void setVsidsScoreWindow(VsidsScoreWindow vW){
        this.vsidsScoreWindow = vW;
    }

    VSIDSProperty getVsidsProperty(){
        return this.vsidsProperty;
    }

    void setVsidsProperty(VSIDSProperty vP){
        this.vsidsProperty = vP;
    }

    public enum SolverType{
        Backtracking, DPLL, CDCL, CDCL_VSIDS
    }

    //Attributs d'Swing
    private JPanel jPanel;
    private JTextPane textPaneTrail;
    private JList<Event> listEvents;
    private JList<Clause> listClauses;
    private JButton btnDecision;
    private JButton btnUnitProp;
    private JButton btnConflict;
    private JButton btnEnd;
    private JButton btnResolve;

    private JMenuItem viewScoreList;
    private JCheckBoxMenuItem manualDecisionsCheckBoxMenuItem;

    //Atributs del solver
    private Instance lastInstance;
    private ViewableSolver solver;
    private SolverType solverType = SolverType.CDCL;
    private VSIDSProperty vsidsProperty = new VSIDSProperty(Constants.INITIAL_SCORE_VALUE, Constants.BONUS_SCORE_VALUE,Constants.INCREMENTED_BONUS_CONSTANT);
    private HistorySolver historySolver;

    //Atributs auxiliars
    boolean instanceLoaded = false;
    private boolean TWLsolver;
    private int oldTrailLength;
    private boolean focusOnLearnedClauseDelay = false;
    private boolean enableKeyboard = true;
    private Styles styles = new Styles();

    private VsidsScoreWindow vsidsScoreWindow;
    private BreakpointSelectWindow breakpointSelectWindow;

    //User-toggleable options
    private boolean manualDecisionOption = false;
    private boolean autoFocusOnConflictOption = true;
    private boolean autoFocusOnLearnedOption = true;

    static final String APP_NAME = "SAT-IT";
    private static final String VERSION = "0.4";
    static final String confirmResetMessage = "You will lose the solving progress. Are you sure?";
    private static final boolean UNDO = true;
    private static final boolean REDO = false;

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(MainWindow::new);
    }

}
