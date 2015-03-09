package playground.pieter.distributed.plans;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableUtils;

import playground.pieter.distributed.scoring.PlanScoreComponent;
import playground.pieter.distributed.scoring.ScoreComponentType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by fouriep on 1/28/15.
 * <p/>
 * Plan class that records the various manipulations it has undergone (for diagnostic purposes), as well as
 * its score in PSim
 */
public class PlanGenome implements Plan {
    private final static Logger log = Logger.getLogger(PlanImpl.class);
    protected ArrayList<PlanElement> actsLegs = new ArrayList<PlanElement>();
    ArrayList<PlanScoreComponent> scoreComponents = new ArrayList<>();
    ArrayList<PlanScoreComponent> altScoreComponents = new ArrayList<>();
    private double pSimScore;
    private String genome = "";
    private Double score = null;
    private Person person = null;
    private String type = null;
    private Customizable customizableDelegate;
    public PlanGenome() {
        System.out.print("");
    }
    /*
     * Creates a new Plan for the specified Person (without adding this Plan to the Person).
     * This is for special uses only, like if you need a Plan as a value object without adding it
     * to a scenario.
     *
     * For initial demand generation, please use scenario.getPopulation().getFactory().createPlan(...) instead.
     *
     */
    public PlanGenome(final Person person) {
        this.person = person;
    }

    public ArrayList<PlanScoreComponent> getScoreComponents() {
        return scoreComponents;
    }

    public void setScoreComponents(ArrayList<PlanScoreComponent> scoreComponents) {
        this.scoreComponents = scoreComponents;
    }

    public ArrayList<PlanScoreComponent> getAltScoreComponents() {
        return altScoreComponents;
    }

    public void setAltScoreComponents(ArrayList<PlanScoreComponent> altScoreComponents) {
        this.altScoreComponents = altScoreComponents;
    }

    public void addScoreComponent(ScoreComponentType type, double score, String description) {
        scoreComponents.add(new PlanScoreComponent(type, score, description));
    }

    public String getGenome() {
        return genome;
    }

    public void setGenome(String genome) {
        this.genome = genome;
    }

    public void appendStrategyToGenome(String strategyCode) {
        genome += strategyCode;
    }

    @Deprecated // use scenario.getPopulation().getFactory().createActivity(...) instead, and add it yourself
    public final ActivityImpl createAndAddActivity(final String type, final Coord coord) {
        ActivityImpl a = new ActivityImpl(type, coord);
        getPlanElements().add(a);
        return a;
    }

    @Deprecated // use scenario.getPopulation().getFactory().createActivity(...) instead, and add it yourself
    public final ActivityImpl createAndAddActivity(final String type, final Id<Link> linkId) {
        ActivityImpl a = new ActivityImpl(type, linkId);
        getPlanElements().add(a);
        return a;
    }

    @Deprecated // use scenario.getPopulation().getFactory().createLeg(...) instead, and add it yourself
    public LegImpl createAndAddLeg(final String mode) {
        verifyCreateLeg();
        LegImpl leg = new LegImpl(mode);
        // Override leg number with an appropriate value
        getPlanElements().add(leg);
        return leg;
    }

    private final void verifyCreateLeg() throws IllegalStateException {
        if (getPlanElements().size() == 0) {
            throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
        }
    }

    /**
     * Removes the specified act from the plan as well as a leg according to the following rule:
     * <ul>
     * <li>first act: removes the act and the following leg</li>
     * <li>last act: removes the act and the previous leg</li>
     * <li>in-between act: removes the act, removes the previous leg's route, and removes the following leg.
     * </ul>
     *
     * @param index
     */
    public final void removeActivity(final int index) {
        if ((index % 2 != 0) || (index < 0) || (index > getPlanElements().size() - 1)) {
            log.warn(this + "[index=" + index + " is wrong. nothing removed]");
        } else if (getPlanElements().size() == 1) {
            log.warn(this + "[index=" + index + " only one act. nothing removed]");
        } else {
            if (index == 0) {
                // remove first act and first leg
                getPlanElements().remove(index + 1); // following leg
                getPlanElements().remove(index); // act
            } else if (index == getPlanElements().size() - 1) {
                // remove last act and last leg
                getPlanElements().remove(index); // act
                getPlanElements().remove(index - 1); // previous leg
            } else {
                // remove an in-between act
                LegImpl prev_leg = (LegImpl) getPlanElements().get(index - 1); // prev leg;
                prev_leg.setDepartureTime(Time.UNDEFINED_TIME);
                prev_leg.setTravelTime(Time.UNDEFINED_TIME);
                prev_leg.setArrivalTime(Time.UNDEFINED_TIME);
                prev_leg.setRoute(null);

                getPlanElements().remove(index + 1); // following leg
                getPlanElements().remove(index); // act
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    // create methods
    //////////////////////////////////////////////////////////////////////

    /**
     * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
     * the following leg will be emptied to keep consistency (i.e. for the route)
     *
     * @param index
     */
    public final void removeLeg(final int index) {
        if ((index % 2 == 0) || (index < 1) || (index >= getPlanElements().size() - 1)) {
            log.warn(this + "[index=" + index + " is wrong. nothing removed]");
        } else {
            if (index != getPlanElements().size() - 2) {
                // not the last leg
                LegImpl next_leg = (LegImpl) getPlanElements().get(index + 2);
                next_leg.setDepartureTime(Time.UNDEFINED_TIME);
                next_leg.setTravelTime(Time.UNDEFINED_TIME);
                next_leg.setArrivalTime(Time.UNDEFINED_TIME);
                next_leg.setRoute(null);
            }
            getPlanElements().remove(index + 1); // following act
            getPlanElements().remove(index); // leg
        }
    }

    @Override
    public final PersonForPlanGenomes getPerson() {
        return (PersonForPlanGenomes) this.person;
    }

    //////////////////////////////////////////////////////////////////////
    // remove methods
    //////////////////////////////////////////////////////////////////////

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
    public final boolean isSelected() {
        return this.getPerson().getSelectedPlan() == this;
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
                "[selected=" + this.isSelected() + "]" +
                "[nof_acts_legs=" + getPlanElements().size() + "]" +
                "[type=" + this.type + "]" +
                "[personId=" + personIdString + "]";
    }

    /**
     * loads a copy of an existing plan, but keeps the person reference
     * <p/>
     * Design comments:<ul>
     * <li> In my intuition, this is really a terrible method: (1) Plan is a data object, not a behavioral object, and thus it should be accessed
     * from static, interface-based methods only.
     * (2) It is not clear about the fact if it is doing a deep or a shallow copy.  The only excuse is that this is one of the oldest parts of
     * matsim.  kai, jan'13
     * </ul>
     *
     * @param in a plan who's data will be loaded into this plan
     */
    public final void copyFrom(final Plan in) {
        this.getPlanElements().clear();
        setScore(in.getScore());
        this.setType(in.getType());
        for (PlanElement pe : in.getPlanElements()) {
            if (pe instanceof Activity) {
                //no need to cast to ActivityImpl here
                Activity a = (Activity) pe;
                getPlanElements().add(new ActivityImpl(a));
            } else if (pe instanceof Leg) {
                Leg l = (Leg) pe;
                LegImpl l2 = createAndAddLeg(l.getMode());
                l2.setDepartureTime(l.getDepartureTime());
                l2.setTravelTime(l.getTravelTime());
                if (pe instanceof LegImpl) {
                    // get the arrival time information only if available
                    l2.setArrivalTime(((LegImpl) pe).getArrivalTime());
                }
                if (l.getRoute() != null) {
                    l2.setRoute(l.getRoute().clone());
                }
            } else {
                throw new IllegalArgumentException("unrecognized plan element type discovered");
            }
        }
    }

    /**
     * Inserts a leg and a following act at position <code>pos</code> into the plan.
     *
     * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
     * @param leg the leg to insert
     * @param act the act to insert, following the leg
     * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
     */
    public void insertLegAct(final int pos, final Leg leg, final Activity act) throws IllegalArgumentException {
        if (pos < getPlanElements().size()) {
            Object o = getPlanElements().get(pos);
            if (!(o instanceof Leg)) {
                throw new IllegalArgumentException("Position to insert leg and act is not valid (act instead of leg at position).");
            }
        } else if (pos > getPlanElements().size()) {
            throw new IllegalArgumentException("Position to insert leg and act is not valid.");
        }

        getPlanElements().add(pos, act);
        getPlanElements().add(pos, leg);
    }

    public Leg getPreviousLeg(final Activity act) {
        int index = this.getActLegIndex(act);
        if (index != -1) {
            return (Leg) getPlanElements().get(index - 1);
        }
        return null;
    }

    public Activity getPreviousActivity(final Leg leg) {
        int index = this.getActLegIndex(leg);
        if (index != -1) {
            return (Activity) getPlanElements().get(index - 1);
        }
        return null;
    }

    public Leg getNextLeg(final Activity act) {
        int index = this.getActLegIndex(act);
        if ((index < getPlanElements().size() - 1) && (index != -1)) {
            return (Leg) getPlanElements().get(index + 1);
        }
        return null;
    }

    public Activity getNextActivity(final Leg leg) {
        int index = this.getActLegIndex(leg);
        if (index != -1) {
            return (Activity) getPlanElements().get(index + 1);
        }
        return null;
    }

    public int getActLegIndex(final PlanElement o) {
        if ((o instanceof Leg) || (o instanceof Activity)) {
            for (int i = 0; i < getPlanElements().size(); i++) {
                if (getPlanElements().get(i).equals(o)) {
                    return i;
                }
            }
            return -1;
        }
        throw new IllegalArgumentException("Method call only valid with a Leg or Act instance as parameter!");
    }

    public Activity getFirstActivity() {
        return (Activity) getPlanElements().get(0);
    }

    public Activity getLastActivity() {
        return (Activity) getPlanElements().get(getPlanElements().size() - 1);
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        if (this.customizableDelegate == null) {
            this.customizableDelegate = CustomizableUtils.createCustomizable();
        }
        return this.customizableDelegate.getCustomAttributes();
    }

    public double getpSimScore() {
        return pSimScore;
    }

    public void setpSimScore(double pSimScore) {
        this.pSimScore = pSimScore;
    }


    public void resetScoreComponents() {
        this.scoreComponents = new ArrayList<>();
    }
}
