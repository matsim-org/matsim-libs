package playground.lsieber.scenario.reducer;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class ModeConverter {
    
    public Population population;


    public ModeConverter(Population population) {
        // TODO Auto-generated constructor stub
        this.population = population;
    }

    // TODO Make input a HasSet of moddes which should be changes to AV HashSet<String> modes
    public Population run() {
        for (Person person : population.getPersons().values()) {

            for (Plan plan : person.getPlans()) {
            {
                // step 1: for all remaining legs set mode="av" and remove the routing
                // information
                for (PlanElement pE1 : plan.getPlanElements()) {
                    if (pE1 instanceof Leg) {
                        Leg leg = (Leg) pE1;
                        if (leg.getMode().equals("pt")) {
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
        return population;
    }
}
