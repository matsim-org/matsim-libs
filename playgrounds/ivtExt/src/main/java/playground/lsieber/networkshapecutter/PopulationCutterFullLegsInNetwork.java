package playground.lsieber.networkshapecutter;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;

public class PopulationCutterFullLegsInNetwork extends PopulationCutter {

    private Network network;
    // private ActivityFacilities facilitiesInNetwork;

    public PopulationCutterFullLegsInNetwork(Network network, ActivityFacilities facilitiesInNetwork) {
        this.network = network;
        // this.facilitiesInNetwork = facilitiesInNetwork;
    }

    @Override
    public void process(Population population) {

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();

        int counter = 0;
        int nextMsg = 1;
        int NumdeletedLegs = 0;

        while (itPerson.hasNext()) {

            counter++;
            if (counter % nextMsg == 0) {
                nextMsg *= 2;
                System.out.println("we are at person # " + counter + ". ");
            }

            Person person = itPerson.next();
            boolean removePerson = false;
            // keep only if we find one leg completely in Network: Start and End Facility in the Network
            // for (Plan plan : person.getPlans()) {
            // Activity actualAct = null;
            // Activity previousAct = null;
            // Leg leg = null;
            // for (PlanElement planElement : plan.getPlanElements()) {
            // if (planElement instanceof Activity) {
            // actualAct = (Activity) planElement;
            // if (previousAct != null) {
            // if (!network.getLinks().containsKey(previousAct.getLinkId()) || !network.getLinks().containsKey(actualAct.getLinkId())) {
            // previousAct.setLinkId(id);
            // leg.setMode("delete");
            // leg.setRoute(null);
            // NumdeletedLegs++;
            // }
            // }
            // }
            // if (planElement instanceof Leg) {
            // leg = (Leg) planElement;
            // }
            // previousAct = actualAct;
            // }
            // }

            for (Plan plan : person.getPlans()) {
                Activity previousAct = null;
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        if (!network.getLinks().containsKey(act.getLinkId())) {
                            removePerson = true;
                            // keep only now until we fix this funtion
                            break;
                        }
                        if (previousAct != null) {
                            if (network.getLinks().containsKey(act.getLinkId()) && network.getLinks().containsKey(previousAct.getLinkId())) {
                            }
                        }

                        previousAct = act;
                    }
                    if (planElement instanceof Leg) {
                    }
                }
            }

            
            // TODO write code to keep reduced person
            if (removePerson)
                itPerson.remove();
        }

        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");
        System.out.println(NumdeletedLegs);
        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");
        System.out.println(".---------------------------------------------");

    }

}
