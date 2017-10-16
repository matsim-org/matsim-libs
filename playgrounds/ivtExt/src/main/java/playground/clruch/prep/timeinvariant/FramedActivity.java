/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;

import ch.ethz.idsc.owly.data.GlobalAssert;

/** @author Claudio Ruch */
public class FramedActivity {
    public Activity abef;
    public Activity aaft;
    public Leg leg;

    public FramedActivity(Activity abef, Leg leg, Activity aaft) {
        GlobalAssert.that(abef != null);
        GlobalAssert.that(aaft != null);
        GlobalAssert.that(leg != null);
        this.abef = abef;
        this.aaft = aaft;
        this.leg = leg;
    }

}
