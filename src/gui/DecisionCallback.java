package gui;

//Per poder passar una lambda cap a Scala, per tal de desacoblar la gui del solver
// (es pot fer al reves? osigui definir la interficie en scala i usarla des de java, seria mes logic)
public interface DecisionCallback {
    int makeDecisionCallback();
}