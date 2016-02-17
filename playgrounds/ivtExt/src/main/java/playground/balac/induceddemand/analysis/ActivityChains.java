package playground.balac.induceddemand.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

public class ActivityChains {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		
		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();

		Set<String> activityChains = new HashSet<String>();
		Map<String, Integer> actPerType = new HashMap<String, Integer>();
		
		int[] sizeChain = new int[20];
		
		stageTypes.addActivityTypes( new StageActivityTypesImpl( "pt interaction" ) );

		int size = 0;
		int count = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			List<Activity> t = TripStructureUtils.getActivities(person.getSelectedPlan(), stageTypes);
			boolean ind = false;
			for (Activity a : t) {
				
				if (a.getType().equals("cb-home") || a.getType().equals("cb-tta") || a.getType().equals("freight") )
					ind = true;
			}
			if (!ind) {
			sizeChain[t.size()]++;
			size += t.size();
			count++;

			String chain = "";
			
			for(Activity a : t) {
				
				if (actPerType.containsKey(a.getType())) {
					
					int current = actPerType.get(a.getType());
					actPerType.put(a.getType(), current + 1);
					
				}
				else {
					
					actPerType.put(a.getType(), 1);
				}
				
				if (!chain.equals(""))
					chain = chain.concat("-");
				chain = chain.concat(a.getType());
			}
			
			activityChains.add(chain);
			}
			
		}
		
		System.out.println("Average number of activities per person is: " + (double)size/(double)count);
		System.out.println("Number of different chains is: " + activityChains.size());
		System.out.println("Chains are: ");
		System.out.println("");

		for (String c : activityChains) {
			
			System.out.println(c);
		}
		
		System.out.println("");

		for (String s : actPerType.keySet()) {
			
			System.out.println(s + ": " + actPerType.get(s).toString());
		}
		System.out.println("");

		
		for(int x : sizeChain) {
			
			System.out.println(x);
		}
		
		
		
		
		
	}

}
