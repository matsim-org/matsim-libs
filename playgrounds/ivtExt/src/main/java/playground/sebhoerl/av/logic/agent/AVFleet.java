package playground.sebhoerl.av.logic.agent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.sebhoerl.av.model.distribution.DistributionStrategy;

public class AVFleet {
    final private Map<Id<Person>, AVAgent> agents = new HashMap<Id<Person>, AVAgent>();
    
    public AVFleet(DistributionStrategy strategy) {
        LinkedList<AVAgent> agents = new LinkedList<AVAgent>();
        strategy.createDistribution(agents);
        
        for (AVAgent agent : agents) {
            this.agents.put(agent.getId(), agent);
        }
    }
    
    public Map<Id<Person>, AVAgent> getAgents() {
        return agents;
    }
}
