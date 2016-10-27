package playground.sebhoerl.av.model.distribution;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.agent.AVAgentFactory;

public class DensityDistributionStrategy implements DistributionStrategy {
    final private Population population;
    final private AVAgentFactory factory;
    
    final private int numberOfVehicles;
    
    public DensityDistributionStrategy(AVAgentFactory factory, Scenario scenario, int numberOfVehicles) {
        this.population = scenario.getPopulation();
        this.numberOfVehicles = numberOfVehicles;
        this.factory = factory;
    }
    
    public void createDistribution(Collection<AVAgent> agents) {
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
        
        for (int i = 0; i < numberOfVehicles; i++) {
            double r = MatsimRandom.getRandom().nextDouble();
            
            for (Id<Link> linkId : linkList) {
                if (r <= density.get(linkId)) {
                    selection.add(linkId);
                    break;
                }
            }
        }
        
        // Create vehicles
        for (Id<Link> linkId : selection) {
            agents.add(factory.createAgent(linkId));
        }
    }
}