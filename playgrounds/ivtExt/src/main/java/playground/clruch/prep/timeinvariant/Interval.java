/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import ch.ethz.idsc.owly.data.GlobalAssert;

/** @author Claudio Ruch */
public class Interval {
    private final int n; // dimension
    private final double[] limits; // borders

    public Interval(double[] limits) {
        // check entry
        GlobalAssert.that(limits != null);
        GlobalAssert.that(limits.length % 2 == 0);
        n = limits.length / 2;

        for (int i = 0; i < n; i += 2) {
            GlobalAssert.that(limits[i] <= limits[i + 1]);
        }
        this.limits = limits;
    }

    public boolean contains(double[] p) {
        if (p.length != n) {
            System.out.println("p not same dimension as interval, aborting...");
            GlobalAssert.that(false);
        }

        for (int i = 0; i < p.length; ++i) {

            boolean b1 = p[i] >= limits[2 * i];
            boolean b2 = p[i] <= limits[2 * i + 1];
            if (!(b1 && b2)) {
                return false;
            }
        }

        return true;
    }

    public int getDim() {
        return n;
    }
    
    public double[] getLength(){
        double length[] = new double[n];
        for(int i = 0; i<n; i+=2){
            length[i] = limits[i+1]- limits[i];
        }
        return length;
        
    }
    

    public String print() {
        String info = "";
        for (int i = 0; i < n; i += 2) {
            info = info + limits[i] + "-->" + limits[i + 1]+ "\n";
        }
        return info;
    }

}
