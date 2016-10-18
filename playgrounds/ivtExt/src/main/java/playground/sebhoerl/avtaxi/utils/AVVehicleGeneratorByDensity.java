package playground.sebhoerl.avtaxi.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.gbl.MatsimRandom;
import playground.sebhoerl.avtaxi.data.AVData;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;

public class AVVehicleGeneratorByDensity {
    final private Population population;
    final private Network network;
    final private AVData data;
    final private AVOperator operator;
    
    public AVVehicleGeneratorByDensity(AVData data, Network network, Population population, AVOperator operator) {
        this.population = population;
        this.network = network;
        this.data = data;
        this.operator = operator;
    }

	public void generate(long numberOfVehicles) {
        Map<Id<Link>, Double> density = new HashMap<Id<Link>, Double>();
        
        double sum = 0.0;
        
        // Determine density
        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Id<Link> linkId = ((Activity) planElement).getLinkId();
                    
                    if (density.containsKey(linkId)) {
                        density.put(linkId, density.get(linkId) + 1.0);
                    } else {
                        density.put(linkId, 1.0);
                    }
                    
                    sum += 1.0;                    
                }
                
                break;
            }
        }
        
        // Compute relative frequencies and cumulative
        double cumsum = 0.0;
        List<Id<Link>> linkList = new LinkedList<Id<Link>>();
        
        for (Id<Link> linkId : density.keySet()) {
            cumsum += density.get(linkId) / sum;
            density.put(linkId, cumsum);
            linkList.add(linkId);
        }

        // Multinomial selection
        List<Id<Link>> selection = new LinkedList<Id<Link>>();
        
        int i = 0;
        
        while (i < numberOfVehicles) {
            double r = MatsimRandom.getRandom().nextDouble();
            
            for (Id<Link> linkId : linkList) {
                if (r <= density.get(linkId)) {
                    selection.add(linkId);
                    i++;
                    break;
                }
            }
        }
        
        long id = 1;
        
        // Create vehicles
        for (Id<Link> linkId : selection) {
        	AVVehicle vehicle = new AVVehicle(
            	    Id.create("av" + String.valueOf(id), Vehicle.class),
            	    network.getLinks().get(linkId),
            	    4.0,
            	    0.0,
            	    108000.0,
                    operator
            	);

            data.addVehicle(vehicle);
            operator.getDispatcher().addVehicle(vehicle);
        	
        	id++;
        }
	}

}
