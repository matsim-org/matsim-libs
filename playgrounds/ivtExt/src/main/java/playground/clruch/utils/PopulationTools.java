package playground.clruch.utils;

import static org.matsim.pt.PtConstants.TRANSIT_ACTIVITY_TYPE;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
 * Created by Claudio on 1/4/2017.
 */


/**
 * Class iterates through plans in population file and replaces all trips by mode "av".
 * First pt_interaction activities are searched and following legs are deleted, then all
 * remaining legs are replace by <leg mode="av" dep_time="prevActivityEndTime" trav_time="NextActivityStartTime-PrevActivityEndTime"> </leg>
 */

// TODO move closer to PopulationConverter
public class PopulationTools {

    private static final Logger log = Logger.getLogger(PopulationTools.class);

    public static void changeModesOfTransportToAV(Population population) {

        log.info("All the modes of transport replaced by av");


        // iterate through population for changes
        for (Person person : population.getPersons().values()) {

            for (Plan plan : person.getPlans()) {
                {
                    // step 1: remove all mode ="pt_interaction" activities, and one subsequent leg
                    Iterator<PlanElement> it = plan.getPlanElements().iterator();
                    while (it.hasNext()) {
                        PlanElement pE = it.next();
                        if (pE instanceof Activity) {
                            Activity act = (Activity) pE;
                            // if act type = "pt_interaction" is deteceted, remove it and the next element which is a pt leg.
                            // this ensures that public transport legs are removed and the  sequence act - leg - act - leg - act - leg is preserved.
                            if (act.getType().equals(TRANSIT_ACTIVITY_TYPE)) {
                                it.remove(); // remove action pt interaction
                                it.next(); // go to next leg
                                it.remove(); // remove also this leg
                            }
                        }
                    }
                }
                {
                    // step 2: for all remaining legs set mode="av" and remove the routing information
                    for (PlanElement pE1 : plan.getPlanElements()) {
                        if (pE1 instanceof Leg) {
                            Leg leg = (Leg) pE1;
                            leg.setMode("av");
                            leg.setRoute(null);
                        }
                    }
                }
                {
                    // step 3: consistency of departure and travel times between the av legs and the activities
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
}
