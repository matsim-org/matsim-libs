package playground.sebhoerl.av.model.distribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import playground.sebhoerl.av.framework.AVModule;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.agent.AVAgentFactory;

public class FirstLegDistributionStrategy implements DistributionStrategy {
	final private Scenario scenario;
	final private AVAgentFactory factory;
	
	final private Set<Id<Person>> servedPersons = new HashSet<Id<Person>>();
	final private int numberOfVehicles;
	
	
	private int assignFirstLeg = 0;
	private int assignRandomly = 0;
	
	
	public FirstLegDistributionStrategy(AVAgentFactory factory, Scenario scenario, int numberOfVehicles) {
		this.scenario = scenario;
		this.numberOfVehicles = numberOfVehicles;
		this.factory = factory;
	}
	
	@Override
	public void createDistribution(Collection<AVAgent> agents) {
		servedPersons.clear();
		
		Population population = scenario.getPopulation();
		Random random = MatsimRandom.getRandom();
		
		// Find all persons which have AV as first leg
		
		List<Id<Person>> availableIds = new ArrayList<Id<Person>>();
		Map<Id<Person>, Id<Link>> linkIds = new HashMap<Id<Person>, Id<Link>>();
		
		for (Id<Person> personId : population.getPersons().keySet()) {
			Person person = population.getPersons().get(personId);
			
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg)planElement;
					
					if (leg.getMode().equals(AVModule.AV_MODE)) {
						availableIds.add(personId);
						linkIds.put(personId, leg.getRoute().getStartLinkId());
					}
					
					break; // Only register if FIRST leg is AV
				}
			}
		}
		
		// TODO: Logging
		//System.err.println(String.format("AV PERSONS %d", availableIds.size()));
		
		// Assign AVs for a random selection of them
		// if more AVs are requested, distribute them randomly
		
		assignFirstLeg = Math.min(numberOfVehicles, availableIds.size());
		assignRandomly = numberOfVehicles - assignFirstLeg;
		
		for (int i = 0; i < assignFirstLeg; i++) {
			int randomIndex = random.nextInt(availableIds.size());
			
			Id<Person> selectedId = availableIds.get(randomIndex);
			availableIds.remove(randomIndex);
			servedPersons.add(selectedId);
			
			agents.add(factory.createAgent(linkIds.get(selectedId)));
		}
		
		// Randomly distribute remaining AVs
		
		if (assignRandomly > 0) {
			DistributionStrategy strategy = new RandomDistributionStrategy(factory, scenario, assignRandomly);
			strategy.createDistribution(agents);
		}
	}
	
	public Set<Id<Person>> getServedPersons() {
		return servedPersons;
	}
	
	public int getAssignRandomly() {
		return assignRandomly;
	}
	
	public int getAssignFirstLeg() {
		return assignFirstLeg;
	}
}
