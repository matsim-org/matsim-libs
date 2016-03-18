package playground.balac.utils.carsharingusers;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class AgentsScores {

	public static void main(String[] args) {
		int car = 0;
		int bike = 0;
		int pt = 0;
		int walk = 0;
		
		double carS = 0.0;
		double walkS = 0.0;
		double bikeS = 0.0;
		double ptS = 0.0;
		
		int carC = 0;
		int ptC = 0;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		populationReader.readFile(args[1]);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		networkReader2.readFile(args[0]);
		PopulationReader populationReader2 = new MatsimPopulationReader(scenario2);
		populationReader2.readFile(args[2]);
		
		double usersS = 0.0;
		int usersC = 0;
		
		double nonusersS = 0.0;
		int nonusersC = 0;
		
		for (Person person : scenario2.getPopulation().getPersons().values()) {
			boolean ind = false;
			boolean indC = false;
			boolean indPt=false;
			Plan plan = person.getSelectedPlan();
			Plan plan2 = scenario.getPopulation().getPersons().get(person.getId()).getSelectedPlan();

			int act = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				
				if (pe instanceof Leg) {
					
					if (((Leg)pe).getMode().equals("freefloating")) {
						ind = true;
						int act2 = 0;
						for ( PlanElement pe2: plan2.getPlanElements()) {
							
							if (act==act2 && pe2 instanceof Leg) {
								if (((Leg)pe2).getMode().equals("car")) {
										car++;
										indC = true;
								}
								else if (((Leg)pe2).getMode().equals("bike"))
									bike++;
								else if (((Leg)pe2).getMode().equals("walk"))
									walk++;
								else if (((Leg)pe2).getMode().equals("pt")) {
									pt++;
									indPt = true;
								}
								break;
							}
							else if (pe2 instanceof Activity)
								act2++;
							
						}
						
					}
				}
				else {
					
					act++;
				}
			}
			if (plan.getScore() > 0) {
				if (indC) {
					
					carS +=(plan.getScore() - plan2.getScore());
					carC++;
					
				}
				if (indPt) {
					
					ptS +=(plan.getScore() - plan2.getScore());
					ptC++;
					
				}
				if (ind) {
					System.out.println(plan.getScore() - plan2.getScore());
					
					usersS += (plan.getScore() - plan2.getScore());
					usersC++;				
					
				}
				else {
					nonusersC++;
					nonusersS += (plan.getScore() - plan2.getScore());
				}
			}
			
			
		}
		System.out.println(car);
		System.out.println(bike);

		System.out.println(walk);
		System.out.println(pt);
		System.out.println(carS / carC);
		System.out.println(ptS / ptC);

		System.out.println(usersS / usersC);
		System.out.println(nonusersS / nonusersC);

		
	}

}
