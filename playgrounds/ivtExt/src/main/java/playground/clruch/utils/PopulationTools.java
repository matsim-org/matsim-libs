package playground.clruch.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.matsim.pt.PtConstants.TRANSIT_ACTIVITY_TYPE;

/**
 * Created by Claudio on 1/4/2017.
 */


// Class iterates through plans in population file and replaces all trips by mode "av".
// First pt_interaction activities are searched and following legs are deleted, then all
// remaining legs are replace by <leg mode="av" dep_time="prevActivityEndTime" trav_time="NextActivityStartTime-PrevActivityEndTime"> </leg>



public class PopulationTools {

    private static final Logger log = Logger.getLogger(PopulationTools.class);

    public static void changeModesOfTransportToAV(Population population) {

        log.info("All the modes of transport replaced by av");



        // iterate through population for changes
        for (Person person : population.getPersons().values()) {

            for (Plan plan : person.getPlans()) {
                final List<PlanElement> planElem = plan.getPlanElements();
                {
                    // step 1: remove all mode ="pt_interaction" activities, and one subsequent leg
                    Iterator<PlanElement> it = plan.getPlanElements().iterator();
                    while (it.hasNext()) {
                        PlanElement pE = it.next();
                        if (pE instanceof Activity) {
                            Activity act = (Activity) pE;
                            if (act.getType().equals( TRANSIT_ACTIVITY_TYPE)) {
                                it.remove();
                                it.next();
                                it.remove();
                            }
                        }
                    }
                }
                {
                    // step 2: for all remaining legs set mode="av" and remove the routing information
                    ListIterator<PlanElement> it1 = planElem.listIterator(0);
                    while (it1.hasNext()) {
                        PlanElement pE1 = it1.next();
                        if (pE1 instanceof Leg) {
                            Leg leg = (Leg) pE1;
                            leg.setMode("av");
                            leg.setRoute(null);
                        }
                    }
                }
                {
                    // step 3: consistency of departure and travel times between the av legs and the activities
                    ListIterator<PlanElement> it1 = planElem.listIterator(0);
                    double endTimeActivity = 0;
                    Leg prevLeg = null;
                    while (it1.hasNext()) {
                        PlanElement pE1 = it1.next();
                        if (pE1 instanceof Activity) {
                            Activity act = (Activity) pE1;
                            if(prevLeg!=null){
                                prevLeg.setTravelTime(act.getStartTime()-endTimeActivity);
                            }
                            endTimeActivity=act.getEndTime();
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
