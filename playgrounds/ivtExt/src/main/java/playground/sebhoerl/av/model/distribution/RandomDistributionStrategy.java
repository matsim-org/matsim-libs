package playground.sebhoerl.av.model.distribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.agent.AVAgentFactory;

public class RandomDistributionStrategy implements DistributionStrategy {
	final private AVAgentFactory factory;
    final private Scenario scenario;
	final private int numberOfVehicles;
	
	public RandomDistributionStrategy(AVAgentFactory factory, Scenario scenario, int numberOfVehicles) {
		this.scenario = scenario;
		this.numberOfVehicles = numberOfVehicles;
		this.factory = factory;
	}
	
	@Override
	public void createDistribution(Collection<AVAgent> agents) {
		List<Id<Link>> keys = new ArrayList<Id<Link>>(scenario.getNetwork().getLinks().keySet());
        Random random = MatsimRandom.getRandom();
        
        for (int i = 0; i < numberOfVehicles; i++) {
        	Id<Link> linkId = keys.get(random.nextInt(keys.size()));
        	agents.add(factory.createAgent(linkId));
        }
	}
}
