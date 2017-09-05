// code by clruch
package playground.clruch.prep;

import static org.matsim.pt.PtConstants.TRANSIT_ACTIVITY_TYPE;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Created by Claudio on 1/4/2017.
 */

/**
 * Class iterates through plans in population file and replaces all trips by mode "av". First pt_interaction activities are searched and following
 * legs are deleted, then all remaining legs are replace by
 * <leg mode="av" dep_time="prevActivityEndTime" trav_time= "NextActivityStartTime-PrevActivityEndTime"> </leg>
 */

public class PopulationTools {

    private static final Logger log = Logger.getLogger(PopulationTools.class);

    public static void changeModesOfTransportToAV(Population population) {

        log.info("All the modes of transport replaced by av");

        // iterate through population for changes
        for (Person person : population.getPersons().values()) {

            for (Plan plan : person.getPlans()) {
                {
                    // step 0: replace pt chains by single pt leg
                    Iterator<PlanElement> it = plan.getPlanElements().iterator();

                    boolean isOnPtTrip = false;
                    long numberOfPtTripElements = 0;
                    Leg ptTripChainStart = null;

                    while (it.hasNext()) {
                        PlanElement pE = it.next();

                        if (pE instanceof Leg) {
                            Leg leg = (Leg) pE;

                            if (!isOnPtTrip) {
                                if (leg.getMode().equals("pt") || leg.getMode().equals("transit_walk")) {
                                    isOnPtTrip = true;
                                    leg.setRoute(null);
                                    numberOfPtTripElements = 1;
                                    ptTripChainStart = leg;
                                }
                            } else {
                                it.remove();
                                numberOfPtTripElements++;
                            }
                        }

                        if (pE instanceof Activity && isOnPtTrip) {
                            Activity act = (Activity) pE;

                            if (!act.getType().equals(TRANSIT_ACTIVITY_TYPE)) {
                                isOnPtTrip = false;

                                if (numberOfPtTripElements == 1 && ptTripChainStart.getMode().equals("transit_walk")) {
                                    ptTripChainStart.setMode("walk");
                                } else {
                                    ptTripChainStart.setMode("pt");
                                }
                            } else {
                                it.remove();
                            }
                        }
                    }
                }

                // {
                // // step 1: remove all mode ="pt_interaction" activities, and one subsequent leg
                // Iterator<PlanElement> it = plan.getPlanElements().iterator();
                // while (it.hasNext()) {
                // PlanElement pE = it.next();
                // if (pE instanceof Activity) {
                // Activity act = (Activity) pE;
                // // if act type = "pt_interaction" is deteceted, remove it and the next
                // // element which is a pt leg.
                // // this ensures that public transport legs are removed and the sequence
                // // act - leg - act - leg - act - leg is preserved.
                // if (act.getType().equals(TRANSIT_ACTIVITY_TYPE)) {
                // it.remove(); // remove action pt interaction
                // it.next(); // go to next leg
                // it.remove(); // remove also this leg
                // }
                // }
                // }
                // }
                {
                    // step 1: for all remaining legs set mode="av" and remove the routing
                    // information
                    for (PlanElement pE1 : plan.getPlanElements()) {
                        if (pE1 instanceof Leg) {
                            Leg leg = (Leg) pE1;
                            if (leg.getMode().equals("car") || leg.getMode().equals("pt")) {
                                leg.setMode("av"); // TODO magic const
                                leg.setRoute(null);
                            }

                        }
                    }
                }
                {
                    // step 2: consistency of departure and travel times between the av legs and the
                    // activities
                    double endTimeActivity = 0;
                    Leg prevLeg = null;
                    for (PlanElement pE1 : plan.getPlanElements()) {
                        if (pE1 instanceof Activity) {
                            Activity act = (Activity) pE1;
                            if (prevLeg != null) {
                                prevLeg.setTravelTime(act.getStartTime() - endTimeActivity);
                            }
                            endTimeActivity = act.getEndTime();
                        }
                        if (pE1 instanceof Leg) {
                            Leg leg = (Leg) pE1;
                            leg.setDepartureTime(endTimeActivity);
                            prevLeg = leg;
                        }
                    }
                }

            }
        }

    }

    @Deprecated // reduce the network to some disk and use the next function supplying the network only
    public static void elminateOutsideRadius(Population population, Coord center, double radius) {

        log.info("All population elements which are more than " + radius + " [m] away from Coord " + center.toString() + " removed.");

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            boolean removePerson = false;
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        Coord actCoord = act.getCoord();
                        if (CoordUtils.calcEuclideanDistance(actCoord, center) > radius) {
                            removePerson = true;
                            break;
                        }
                    }
                }
            }
            if (removePerson)
                itPerson.remove();
        }
    }

    public static void elminateOutsideNetwork(Population population, Network network) {

        log.info("All population elements which have activities inside the provided network are removed.");

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            boolean removePerson = false;
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        // Coord actCoord = act.getCoord(); // not used
                        Id<Link> actLink = act.getLinkId();
                        if (!network.getLinks().containsKey(actLink)) {
                            removePerson = true;
                            break;
                        }
                    }
                }
            }
            if (removePerson)
                itPerson.remove();
        }
    }

    /**
     * removes all persons with activity Type "freight" from population
     * 
     * @param population
     */
    public static void eliminateFreight(Population population) {
        log.info("All population elements with activity freight are removed.");

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            boolean removePerson = false;
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        if (act.getType() == "freight") {
                            removePerson = true;
                            break;
                        }
                    }
                }
            }
            if (removePerson)
                itPerson.remove();
        }

    }

    /**
     * removes all persons with activity Type "freight" from population
     * 
     * @param population
     */
    public static void eliminateWalking(Population population) {
        log.info("All population elements with walking segments removed.");

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            boolean removePerson = false;
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Leg leg = (Leg) planElement;
                        if (leg.getMode() == "walk") {
                            removePerson = true;
                            break;
                        }
                    }
                }
            }
            if (removePerson)
                itPerson.remove();
        }

    }

}
