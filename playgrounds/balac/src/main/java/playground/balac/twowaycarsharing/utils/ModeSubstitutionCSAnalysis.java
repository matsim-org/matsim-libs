package playground.balac.twowaycarsharing.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class ModeSubstitutionCSAnalysis {

	
	public void run(String[] args) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);	
		
		ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader2 = new MatsimPopulationReader(scenario2);
		MatsimNetworkReader networkReader2 = new MatsimNetworkReader(scenario2);
		networkReader2.readFile(args[1]);
		populationReader2.readFile(args[2]);	
		
		int[] bla = new int[5];
		int count = 0;
		int number = 0;
		int countWalk = 0;
		double distance = 0.0;
		for (Person person: scenario2.getPopulation().getPersons().values()) {
			int j = 0;
			for (PlanElement pe:person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity && !((Activity) pe).getType().equals("cs_interaction") && !((Activity) pe).getType().equals("pt interaction"))
					j++;
				 if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("onewaycarsharing")) {
						Leg s = null;
						
						s = findMode(person.getId(), j, scenario);
						
						if (s.getMode().equals("car")) {
							
							bla[0]++;
							count++;
						}
						else if (s.getMode().equals("bike")) {
							
							bla[1]++;
							count++;
						}
						else if (s.getMode().equals("walk")) {
							bla[2]++;
							count++;
						}
						else if (s.getMode().equals("walk_rb") || s.getMode().equals("twowaycarsharing")) {
							
							bla[3]++;
							count++;
							
						}
						else if (s.getMode().equals("transit_walk") || s.getMode().equals("pt")) {
							
							bla[4]++;
							count++;
						}
						else 
							number++;
						
						
					}
					else if (((Leg) pe).getMode().equals("walk")) {
						
						countWalk++;
						distance += ((Leg)pe).getRoute().getDistance();
						
					}
				}
			}
			
		}
		
		for (int i = 0; i < 5; i++) { 
    		System.out.println((double)bla[i]/(double)count * 100.0);
						
    	}
		System.out.println(distance/(double)countWalk);
    	System.out.println(count);
    	System.out.println(number);
	}
	
	public Leg findMode(Id id, int j, ScenarioImpl scenario) {
		
		Person p = scenario.getPopulation().getPersons().get(id);
		int i = 0;
		int k = 0;
		for(PlanElement pe1: p.getSelectedPlan().getPlanElements()) {
			
			if (pe1  instanceof Activity && !((Activity) pe1).getType().equals("cs_interaction") && !((Activity) pe1).getType().equals("pt interaction")) {
				i++;
				if (i == j) {
					
					return ((Leg)p.getSelectedPlan().getPlanElements().get(k + 1));
				}
			}
			k++;
		}
		
		
		
		return null;
	}
	public static void main(String[] args) {

		ModeSubstitutionCSAnalysis ms = new ModeSubstitutionCSAnalysis();
		ms.run(args);
		
		
	}

}
