/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Population;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    public static Population at(int time, int duration, Population population) {
        System.out.println("calc. time-invariant pop. from " + time + " to " + time + duration);
        return population;
    }

}
