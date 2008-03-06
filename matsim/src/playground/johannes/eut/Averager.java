package playground.johannes.eut;

import java.util.Random;

/**
 * 
 * Calculates an average in a simple recursive way.
 * 
 * @author gunnar
 * 
 */
public class Averager {

    // -------------------- MEMBER VARIABLES --------------------

    private int size;

    private double sum;

    // -------------------- CONSTRUCTION --------------------

    public Averager() {
        clear();
    }

    // -------------------- PUBLIC ACCESS --------------------

    public void clear() {
        size = 0;
        sum = 0;
    }

    public void add(double val) {
        sum += val;
        size++;
    }

    public double getAvg() {
        return sum / size;
    }

    public int getSize() {
        return size;
    }

    // -------------------- MAIN-FUNCTION FOR TESTING --------------------

    public static void main(String[] args) {
        Random rnd = new Random();
        Averager c = new Averager();
        for (int i = 0; i < 1000 * 1000; i++) {
            final double x = rnd.nextGaussian();
            c.add(x);
        }
        System.out.println("xAvg = " + c.getAvg());
    }

}
