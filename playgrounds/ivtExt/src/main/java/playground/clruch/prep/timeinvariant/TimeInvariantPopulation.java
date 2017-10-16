/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    public static Population at(int time, int duration, Population population) {
        System.out.println("calc. time-invariant pop. from " + time + " to " + (time + duration));

        // 1) get all trips that occur during timeslice
        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();

        HashMap<Person, HashSet<FramedActivity>> toAdd = new HashMap<>();
        for (Person person : population.getPersons().values()) {
            toAdd.put(person, new HashSet<>());
        }

        PlanElement planE1 = null;
        PlanElement planE2 = null;
        PlanElement planE3 = null;

        for (Person person : people.values()) {

            List<Plan> plans = (List<Plan>) person.getPlans();

            for (Plan plan : plans) {
                for (PlanElement planENew : plan.getPlanElements()) {
                    planE3 = planE2;
                    planE2 = planE1;
                    planE1 = planENew;

                    if (planE2 instanceof Leg) {
                        Leg leg = (Leg) planE2;
                        GlobalAssert.that(planE3 instanceof Activity);
                        GlobalAssert.that(planE1 instanceof Activity);
                        Activity abefor = (Activity) planE3;
                        Activity aafter = (Activity) planE1;
                        double depTime = leg.getDepartureTime();
                        System.out.println("depTime: " + depTime);
                        boolean inWindow = depTime >= time && depTime <= time + duration;
                        if (inWindow) {
                            FramedActivity fac = new FramedActivity(abefor, leg, aafter);
                            toAdd.get(person).add(fac);
                            System.out.println("the plan is in the morning window");
                        }
                    }
                }
            }
        }

        for (Person person : people.values()) {
            for (FramedActivity fa : toAdd.get(person)) {
                Utils.removeAllButFramedA(person, fa);
            }
        }

        return population;
    }

}
