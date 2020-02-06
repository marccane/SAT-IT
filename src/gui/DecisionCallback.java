package gui;

//Per poder passar una lambda cap a Scala, per tal de desacoblar la gui del solver
public interface DecisionCallback {
    int makeDecisionCallback();
}