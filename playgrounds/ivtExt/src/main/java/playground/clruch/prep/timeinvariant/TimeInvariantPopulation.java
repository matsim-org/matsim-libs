/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/** @author Claudio Ruch */
public enum TimeInvariantPopulation {
    ;

    public static Population at(int time, int duration, Population population) {
        System.out.println("calc. time-invariant pop. from " + time + " to " + (time + duration));

        // 1) get all trips that occur during timeslice
        Map<Id<Person>, Person> people = (Map<Id<Person>, Person>) population.getPersons();

        HashMap<Person, HashSet<Plan>> toRemove = new HashMap<>();
        for(Person person : population.getPersons().values()){
            toRemove.put(person, new HashSet<>());
        }
        
        

        for (Person person : people.values()) {

            List<Plan> plans = (List<Plan>) person.getPlans();

            for (Plan plan : plans) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Leg leg = (Leg) planElement;
                        double depTime = leg.getDepartureTime();
                        System.out.println("depTime: " +  depTime);
                        boolean inWindow = depTime >= time && depTime <= time + duration;
                        if (!inWindow) {
                            toRemove.get(person).add(plan);
                        }else{
                            System.out.println("the plan can stay, it is in the window");
                        }
                    }
                }
            }
        }

        for(Person person : people.values()){
            for(Plan plan : toRemove.get(person)){
                person.removePlan(plan);
            }
        }
        

        return population;
    }

}
