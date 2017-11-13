/**
 * 
 */
package playground.clruch.prep.timeinvariant;

/**
 * @author Claudio Ruch copied from MATSim
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

/* deliberately package */ final class PlanImplAdd implements Plan {

    private ArrayList<PlanElement> actsLegs = new ArrayList<>();

    private Double score = null;
    private Person person = null;

    private String type = null;

    @SuppressWarnings("unused")
    private final static Logger log = Logger.getLogger(Plan.class);

    private Customizable customizableDelegate;

    private final Attributes attributes = new Attributes();

    @Override
    public final Attributes getAttributes() {
        return this.attributes;
    }

    /* package */ PlanImplAdd() {}

    // @Override
    // public final Activity createAndAddActivity(final String type1) {
    // Activity a = new ActivityImpl(type1);
    // // (PlanImpl knows the corresponding Activity implementation, so it does not have to go through the factory. kai, jun'16)
    //
    // this.addActivity(a) ;
    // return a;
    // }

    //////////////////////////////////////////////////////////////////////
    // create methods
    //////////////////////////////////////////////////////////////////////

    // @Override
    // public Leg createAndAddLeg(final String mode) {
    // verifyCreateLeg();
    // Leg leg = new LegImpl( mode ) ;
    // getPlanElements().add(leg);
    // return leg;
    // }

    // private static void verifyCreateLeg(Plan plan) throws IllegalStateException {
    // if (plan.getPlanElements().size() == 0) {
    // throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
    // }
    // }

    //////////////////////////////////////////////////////////////////////
    // remove methods
    //////////////////////////////////////////////////////////////////////

    @Override
    public final Person getPerson() {
        return this.person;
    }

    @Override
    public void setPerson(final Person person) {
        this.person = person;
    }

    @Override
    public final Double getScore() {
        return this.score;
    }

    @Override
    public void setScore(final Double score) {
        this.score = score;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public final List<PlanElement> getPlanElements() {
        return this.actsLegs;
    }

    @Override
    public final void addLeg(final Leg leg) {
        this.actsLegs.add(leg);
    }

    @Override
    public final void addActivity(final Activity act) {
        this.actsLegs.add(act);
    }

    @Override
    public final String toString() {

        String scoreString = "undefined";
        if (this.getScore() != null) {
            scoreString = this.getScore().toString();
        }
        String personIdString = "undefined";
        if (this.getPerson() != null) {
            personIdString = this.getPerson().getId().toString();
        }

        return "[score=" + scoreString + "]" +
        // "[selected=" + PersonUtils.isSelected(this) + "]" +
                "[nof_acts_legs=" + getPlanElements().size() + "]" + "[type=" + this.type + "]" + "[personId=" + personIdString + "]";
    }

    @Override
    public final Map<String, Object> getCustomAttributes() {
        if (this.customizableDelegate == null) {
            this.customizableDelegate = CustomizableUtils.createCustomizable();
        }
        return this.customizableDelegate.getCustomAttributes();
    }

    // public final void setLocked() {
    // for ( PlanElement pe : this.actsLegs ) {
    // if ( pe instanceof ActivityImpl ) {
    // ((ActivityImpl) pe).setLocked();
    // } else if ( pe instanceof LegImpl ) {
    // ((LegImpl) pe).setLocked() ;
    // }
    // }
    // }

}
