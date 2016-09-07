package playground.balac.utils.population;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationComparison {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader2 = new PopulationReader(scenario2);
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[0]);
		populationReader2.readFile(args[2]);
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		double score1 = 0.0;
		double score2 = 0.0;
		double score3 = 0.0;
		int count = 0;

		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			Plan plan2 = scenario2.getPopulation().getPersons().get(person.getId()).getSelectedPlan();
			if (plan.getPlanElements().size() > plan2.getPlanElements().size()) {
				count1++;
				score1 += plan.getScore() - plan2.getScore();
			}
			else if (plan.getPlanElements().size() < plan2.getPlanElements().size()) {
				count2++;
				score2 += plan.getScore() - plan2.getScore();

			}
			else { 
				count3++;
				score3 += plan.getScore() - plan2.getScore();

			}
			
			List<Activity> activities1 = TripStructureUtils.getActivities(plan, null);
			
			List<Activity> activities2 = TripStructureUtils.getActivities(plan2, null);

			
			Set<String> act = new TreeSet<String>();
			for (Activity a : activities2) {
				
				act.add(a.getType());
			}
			for (Activity a : activities1) {
				
				if (!act.contains(a.getType()))
					count++;
			}
			
			
		}	
		
		System.out.println(count);

		
		System.out.println(count1 + " " + count2 + " " + count3);
		System.out.println(score1 / count1 + " " + score2 / count2 + " " + score3 / count3);

		
	}

}
