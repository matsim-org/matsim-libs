package playground.lsieber.networkshapecutter;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class PopulationCutterShape {

    public PopulationCutterShape() {
    }

    
    public Population elminateOutsideNetwork(Population population, Network network) {


        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        int counter = 0;
        int nextMsg = 1;
        
        while (itPerson.hasNext()) {
            
            counter++;
            if (counter % nextMsg == 0) {
                nextMsg *= 2;
                System.out.println("we are at person # "+ counter + ". ");
            }
            
            
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
        return population;
    }
    
}
