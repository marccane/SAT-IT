package gui;

import event.BackjumpEvent;
import event.BacktrackEvent;
import event.DecisionEvent;
import javafx.util.Pair;
import scala.Enumeration;
import scala.collection.immutable.List;
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
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import static gui.EventHandler.*;
import static gui.ClauseListLiteralHighlight.createListRenderer;
import static util.Constants.*;

public class MainGUI extends JFrame{

    private static final String VERSION = "0.314159265";
    public static final String NAME = "SAT-IT";

    public MainGUI() {
        super(NAME);
        this.setFocusTraversalKeysEnabled(false);
        handleParametersAndInit("", null);
    }

    public MainGUI(String filename){
        super(NAME);
        this.setTitle(nameWithFile(filename));
        handleParametersAndInit(filename, null);
    }

    public MainGUI(String filename, SolverType st){
        super(NAME);
        this.setTitle(nameWithFile(filename));
        handleParametersAndInit(filename, st);
    }

    public String nameWithFile(String path)
    {
        int i = 0;
        String nom = "";
        boolean trobat = false;
        while(i < path.length() && !trobat)
        {
            char lletra = path.charAt(path.length() - i - 1);
            if( lletra == File.separatorChar)
                trobat = true;
            else
                nom = String.valueOf(lletra).concat(nom);
            i++;
        }
        return NAME + " " + solverType + " " + nom;
    }

    private void handleParametersAndInit(String filename, SolverType solverType){
        Instance instance = new Instance();
        if(!filename.isEmpty())
            instance = loadInstanceFromFile(new File(filename));
        if(solverType!=null)
            this.solverType = solverType;
        initGUI(instance);
    }

    //Aixo s'ha de cridar NOMES una vegada (quan inicialitzem la gui)
    private void initGUI(Instance instance){
        initComponents();
        setEventHandlers();
        addStylesToDocument(textPaneTrail.getStyledDocument());

        loadInstanceGUI(instance);
    }

    private void loadInstanceGUI(Instance instance){
        //Netejar GUI
        LitScoreSet litScoreSet = null;
        if(solverType == SolverType.CDCL_VSIDS && solver instanceof CDCL) litScoreSet = ((CDCL) solver).scoreLitQueue();
        if(viewWindows != null && viewWindows.isVisible() && (historySolver == null || !historySolver.inHistory()))
            viewWindows.dispose();
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
            if(viewWindows != null) viewWindows.setCellRenderList((CDCL) solver);
        }
        if(!historySolver.getInHistory())historySolver.init();
        if(manualDecisionOption) solver.setDecisionCallback(manualDecisionCallback);
        oldTrailLength = 0;
        lastInstance = instance;
        instanceLoaded = instance.numVariables() != 0;
        TWLsolver = solver instanceof TwoWatchedLiteralSolver;
        boolean doInitialUnitProp = !(solver instanceof Backtracking);
        vsidsPropiety.resetAddScore();
        solver.setVSIDSPropiety(vsidsPropiety);
        solver.initSolverForGUI(instance, doInitialUnitProp);

        if(manualDecisionOption && solverType == SolverType.CDCL_VSIDS) solver.setDecisionCallback(manualDecisionCallback);

        //Desactivar boto Unit Propagation si el solver es backtracking
        if(!TWLsolver)
            btnUnitProp.setEnabled(false);
        else
            btnUnitProp.setEnabled(true);
        initButtoms();
        try{
            updateTrail();
            trailAddFinishText(solver.solverState(),textPaneTrail.getStyledDocument());
            updateClauses();
            updateEvents();
            //Afeguim l'historial al solver
            solver.setHistorySolver(historySolver);
        } catch(Exception e){
            JOptionPane.showMessageDialog(this,"Unknown exception: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addText(JPanel jPanel, String title, String text){

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

            JPanel jPanel = new JPanel();
            jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));

            if(instance.numErrors() != 0)
                addText(jPanel, "Errors:", instance.getErrossText());
            if(instance.numWarnings() != 0)
                addText(jPanel, "Warnings:", instance.getWarningsText());


            JOptionPane.showMessageDialog(this, jPanel, instance.numErrors() != 0 ? "Errors" : "Warnings", instance.numErrors() != 0 ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
        }
        this.setTitle(nameWithFile(file.getPath()));
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


    private void setDifuminitat()
    {
        ///propagationLiteral
        StyleConstants.setForeground(estils.get(0), Color.lightGray);
        //propagationLiteralLearned
        StyleConstants.setForeground(estils.get(1), clearOrange);
        ///decisionLiteral
        StyleConstants.setForeground(estils.get(2), new Color(120, 177, 255));
        ///backtrackLiteral
        StyleConstants.setForeground(estils.get(3), new Color(255, 216, 117));
        ///superscript
        StyleConstants.setForeground(estils.get(4), Color.lightGray);

    }

    private void setClear()
    {
        ///propagationLiteral
        StyleConstants.setForeground(estils.get(0), Color.black);
        //propagationLiteralLearned
        StyleConstants.setForeground(estils.get(1), darkOrange);
        ///decisionLiteral
        StyleConstants.setForeground(estils.get(2), Color.blue);
        ///backtrackLiteral
        StyleConstants.setForeground(estils.get(3),  darkYellow);
        ///superscript
        StyleConstants.setForeground(estils.get(4), Color.black);
    }

    private void setHighlight(boolean hihglight){
        Color color = hihglight ? new Color(255, 0, 0, 30) : new Color(0,0,0, 0);
        estilsHighlight.forEach((s) -> StyleConstants.setBackground(s, color));
    }

    private void addStylesToDocument(StyledDocument doc){
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        estils = new ArrayList<>();
        estilsHighlight = new ArrayList<>();
        //StyleConstants.setFontFamily(def, "SansSerif");
        doc.addStyle("default", def);

        Style propLit = doc.addStyle("propagationLiteral", def);
        estils.add(propLit);
        estilsHighlight.add(propLit);

        Style propLitLearned = doc.addStyle("propagationLiteralLearned", def);
        StyleConstants.setForeground(propLitLearned, darkOrange);
        StyleConstants.setBold(propLitLearned, true);
        estils.add(propLitLearned);
        estilsHighlight.add(propLitLearned);

        Style redBold = doc.addStyle("fail", def);
        StyleConstants.setBold(redBold, true);
        StyleConstants.setForeground(redBold, Color.red);

        Style blackBold = doc.addStyle("backtrack", redBold);
        StyleConstants.setForeground(blackBold, darkYellow);
        //StyleConstants.setForeground(blackBold, Color.black);

        Style orangeBold = doc.addStyle("learned", redBold);
        StyleConstants.setForeground(orangeBold, darkOrange);

        Style greenBold = doc.addStyle("sat", redBold);
        StyleConstants.setForeground(greenBold, Color.green);

        Style blueBold = doc.addStyle("decisionLiteral", redBold);
        StyleConstants.setForeground(blueBold, Color.blue);
        estils.add(blueBold);
        estilsHighlight.add(blueBold);

        Style cyanBold = doc.addStyle("backtrackLiteral", blueBold);
        StyleConstants.setForeground(cyanBold, darkYellow);
        StyleConstants.setBold(cyanBold, true);
        estils.add(cyanBold);
        estilsHighlight.add(cyanBold);

        Style superscript = doc.addStyle("superscript", def);
        StyleConstants.setSuperscript(superscript, true);
        estils.add(superscript);
        setClear();
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
        //c.ipady = 100;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.5;
        jPanel.add(new JScrollPane(listClauses),c);

        initJMenuBar();

        //---------------------------------------------
        setSize((int) (Constants.getWidth()*0.7), (int) (Constants.getHeight()*0.85)); //amplada, altura
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        //pack();
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

    public void initButtoms()
    {
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
        HashSet<Object> breakpoints = solver.getVariablesBreakpoint();
        if(isUndo) historySolver.undo();
        else historySolver.redo();
        loadInstanceGUI(lastInstance);
        solver.setDecisionCallback(historySolver);
        solver.setVariablesBreakpoint(breakpoints);
        ListBuffer<Object> t = historySolver.getHistoryTrailClone();
        int[] actions = historySolver.getHistorAction();
        int last = -1;
        int actual = -1;
        for(int i = 0; i < historySolver.getNumH(); i++){
            int solverAction = solverStep();
            last = actual;
            actual = actions[i];
        }
        updateGUI();
        updateButtoms(last, actual);
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

            JMenuItem undonMenuItem = new JMenuItem("<html><body>Undo&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: gray;\">(Ctrl+Z or &#8592;)</span></body></html>");
            undonMenuItem.addActionListener(stub -> undoRedoFunction(UNDO));

            JMenuItem redoMenuItem  = new JMenuItem("<html><body>Redo&nbsp;&nbsp;&nbsp;&nbsp;<span style = \"color: gray;\">(Ctrl+Y or &#8594;)</span></body></html>");
            redoMenuItem.addActionListener(stub ->{
                if (solver.solverState() != SolvingState.SAT())
                    undoRedoFunction(REDO);
            });
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
        fileMenu.add(undonMenuItem);
        fileMenu.add(resetMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu optionsMenu = new JMenu("Options");

            JMenuItem solverMenuItem = new JMenuItem("Change solver");
            solverMenuItem.addActionListener(stub -> new SolverSelectorWindow(this,solver));

            manualDecisionsCheckBoxMenuItem = new JCheckBoxMenuItem("<html><body>Manual Decisions&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: gray;\">(M)</span></body></html>", manualDecisionOption);
            manualDecisionsCheckBoxMenuItem.addItemListener(evt -> manualFunction());
            JMenuItem vsidsParameters = new JMenuItem("VSIDS parameters");
            vsidsParameters.addActionListener(evt -> new VsidsOptionsWindow(this,solver));



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
                    "SAT-IT " + MainGUI.VERSION + "\n\nAuthor: Marc Cané Salamià\nTutors: Mateu Villaret & Jordi Coll",
                    "About", JOptionPane.INFORMATION_MESSAGE));
            helpMenu.add(aboutMenuItem);


        JMenu viewMenu = new JMenu("View");
        viewScoreList = new JMenuItem("Score List");
        viewScoreList.addActionListener(stub -> new ScoreVSIDSList(this, (CDCL) solver, getTitle()));

        MainGUI mainGUI = this;
        class MouseAdapterBreakPoint extends MouseAdapter{
            public void mouseClicked(MouseEvent evt) {
                boolean notVisible = breakpointSelectWindow == null || !breakpointSelectWindow.isVisible();
                if (evt.getClickCount() == 1 && notVisible){
                    new BreakpointSelectWindow(mainGUI, solver);
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
        updateClauses(); //per tenir les clausules act. al moment de decidir. testejar
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
                //decisionMade = true;
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
                setDifuminitat();
                //escriure trail despres el backjump
                for(int i=0; i<actualTrailLength - 1; i++)
                    trailAddLiteral(i);
                setClear();
                trailAddLiteral(actualTrailLength- 1);
            }
            else if(solverAction==SOLVER_BACKTRACK){
                doc.insertString(doc.getLength(), "BACKTRACK\n\n", doc.getStyle("backtrack"));
                setDifuminitat();
                //escriure trail despres el backtrack
                for(int i=0; i<actualTrailLength - 1; i++)
                    trailAddLiteral(i);
                setClear();
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
    //} void stub2(int litIdx) throws javax.swing.text.BadLocationException{
        int lit = solver.getTrailIndex(litIdx);
        StyledDocument doc = textPaneTrail.getStyledDocument();
        boolean breakpoint = solver.isBreakpoint(Math.abs(lit));
        ArrayList<Pair<Boolean, Color>> boldsColors = new ArrayList<>(estils.size());
        setHighlight(breakpoint);

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
            } else if(propagator >= solver.getInitialClausules()){ //unitprop
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
        setHighlight(false);
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

    public SolverType getSolverType(){
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
                //this.setFont(this.getFont().deriveFont(14f).deriveFont(Font.PLAIN));

                Event event = (Event) value;
                setText(event.toString());

                if(event instanceof BacktrackEvent)
                    setForeground(darkYellow);
                else if(event instanceof DecisionEvent)
                    setForeground(Color.blue);
                else if(event instanceof BackjumpEvent)
                    setForeground(purple);

                return this;
            }
        };
    }

    public void failAction(int solverAction)
    {
        if(solverAction == SOLVER_CONFLICT)
        {
            btnDecision.setEnabled(false);
            btnUnitProp.setEnabled(false);
            btnConflict.setEnabled(false);
            btnResolve.setEnabled(true);
        }
        else initButtoms();
    }

    public void updateButtoms(int last, int actual){
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

    public void manualFunction(){
        if(!manualDecisionOption){
            manualDecisionOption = true;
            solver.setDecisionCallback(manualDecisionCallback);
        } else {
            manualDecisionOption = false;
            solver.resetDecisionCallback();
        }
    }

    public void setDecision(boolean estat)
    {
        btnDecision.setEnabled(estat);
    }

    public void setBreakpointSelectWindow(BreakpointSelectWindow breakpointSelectWindow){
        this.breakpointSelectWindow = breakpointSelectWindow;
    }

    public boolean isBacktracking()
    {
        return !TWLsolver;
    }

    public void changeSolverTitle()
    {
        String title = this.getTitle();
        title = title.replaceFirst(NAME + " ", "");
        title = title.replaceFirst(".* ", "");
        this.setTitle(NAME + " " + solverType + " " + title);
    }

    public void setEnableKeyBoard(boolean enable) {
        enableKeyboard = enable;
    }

    public void setViewScoreListEnable(boolean enable){
        viewScoreList.setEnabled(enable);
    }

    public void setViewWindows(ScoreVSIDSList vW){
        this.viewWindows = vW;
    }

    public VSIDSPropiety getVsidsPropiety(){
        return this.vsidsPropiety;
    }

    public void setVsidsPropiety(VSIDSPropiety vP){
        this.vsidsPropiety = vP;
    }

    public enum SolverType{
        Backtracking, DPLL, CDCL, CDCL_VSIDS
    }

    //Guarda les dades per poder retrocedir a un estat anterior del solver (unused)
    private class TimeTravelData{
        ClauseWrapper clauseWrapper;
        int numEvents;
        int specIndex;
    }

    //Atributs pel timetravel (per fer una copia parcial del trail, unused)
    //StyledDocument testDoc;
    //boolean testCopiaFeta = false;
    //int specCount;

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
    private ScoreVSIDSList viewWindows;
    private BreakpointSelectWindow breakpointSelectWindow;
    private ArrayList<Style> estils;
    private ArrayList<Style> estilsHighlight;
    private JCheckBoxMenuItem manualDecisionsCheckBoxMenuItem;

    //Atributs del solver
    private Instance lastInstance;
    private ViewableSolver solver;
    private SolverType solverType = SolverType.CDCL;
    private VSIDSPropiety vsidsPropiety = new VSIDSPropiety(Constants.INITIAL_SCORE_VALUE, Constants.BONUS_SCORE_VALUE,Constants.INCREMENTED_BONUS_CONSTANT);
    private HistorySolver historySolver;

    //Atributs auxiliars
    boolean instanceLoaded = false;
    private boolean TWLsolver;
    private int oldTrailLength;
    private boolean focusOnLearnedClauseDelay = false;
    private static final boolean UNDO = true;
    private static final boolean REDO = false;
    private boolean enableKeyboard = true;

    //User-toggleable options
    private boolean manualDecisionOption = false;
    private boolean autoFocusOnConflictOption = true;
    private boolean autoFocusOnLearnedOption = true;

    static final String confirmResetMessage = "You will lose the solving progress. Are you sure?";
    static final Color darkOrange = new Color(255,128,0);
    static final Color clearOrange = new Color(255,200,145);
    static final Color darkYellow = new Color(255, 183, 0);
    static final Color purple = new Color(128,0,128);

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(MainGUI::new);
    }

}
