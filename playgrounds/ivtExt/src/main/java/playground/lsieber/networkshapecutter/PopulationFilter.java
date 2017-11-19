package playground.lsieber.networkshapecutter;

import static org.matsim.pt.PtConstants.TRANSIT_ACTIVITY_TYPE;

import java.util.Iterator;

import org.hamcrest.BaseDescription;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import playground.lsieber.scenario.reducer.SecondPotenceSysout;

public class PopulationFilter {

    protected Population population;

    public PopulationFilter(Population population) {
        // TODO Auto-generated constructor stub
        this.population = population;
    }
    
    public void run(Network network) {
        reducePtToOneLeg();
        basedOnActivities(network);

    }

    private void reducePtToOneLeg() {
        // iterate through population for changes
        /* Adapted form PopulationTOOés */
        for (Person person : population.getPersons().values()) {
            
            for (Plan plan : person.getPlans()) {
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
        }
    }
    
    private void basedOnActivities(Network network) {
        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        SecondPotenceSysout print = new SecondPotenceSysout("we are at person # ");
                
        while (itPerson.hasNext()) {           
            print.ifPotenceOf2();
            
            Person person = itPerson.next();
            boolean removePerson = true;
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        // Coord actCoord = act.getCoord(); // not used
                        Id<Link> actLink = act.getLinkId();
                        if (network.getLinks().containsKey(actLink)) {
                            removePerson = false;
                            break;
                        }
                    }
                }
            }
            if (removePerson)
                itPerson.remove();
        }
    }
    
    public Population getPopulation() {
        return population;
    }

}
