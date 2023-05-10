package util;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;

public class Constants {

    //Indica l'ultima accio que ha fet el solver
    public static final int SOLVER_END = 0;
    public static final int SOLVER_DECISION = 1;
    public static final int SOLVER_UNITPROP = 2;
    public static final int SOLVER_BACKJUMP = 3;
    public static final int SOLVER_CONFLICT = 4;
    public static final int SOLVER_BACKTRACK = 5;
    public static final int CANCEL_DECISION = -1;
    public static final scala.math.BigDecimal INITIAL_SCORE_VALUE = new scala.math.BigDecimal(new BigDecimal("0"));
    public static final scala.math.BigDecimal BONUS_SCORE_VALUE = new scala.math.BigDecimal(new BigDecimal("2"));
    public static final scala.math.BigDecimal INCREMENTED_BONUS_CONSTANT = new scala.math.BigDecimal(new BigDecimal("1.05"));
    public static final boolean ADD_START_VALUE = true;


    public static ArrayList<Image> getLogos(){
        ImageIcon img = new ImageIcon(Constants.class.getResource("/logo/logo2.png"));
        ArrayList<Image> images = new ArrayList<>();
        images.add(img.getImage());
        //images.add(img2.getImage());
        return images;
    }

    public static ArrayList<Image> getLogosHelp(){
        ImageIcon img  = new ImageIcon(Constants.class.getResource("/logo/logo2.png"));
        ImageIcon img2 = new ImageIcon(Constants.class.getResource("/logo/question.png"));
        ArrayList<Image> images = new ArrayList<>();
        images.add(img.getImage());
        images.add(img2.getImage());
        return images;
    }

    public static Icon getRedBreakpoint(){
        return null;
    }

    public static Dimension getDimensionScreen(){
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    public static double getHeight(){
        return getDimensionScreen().getHeight();
    }

    public static double getWidth(){
        return getDimensionScreen().getWidth();
    }

}
