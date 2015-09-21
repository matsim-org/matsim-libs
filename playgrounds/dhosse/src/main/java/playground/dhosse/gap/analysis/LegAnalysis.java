package playground.dhosse.gap.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class LegAnalysis {

	public static void main(String args[]){
		
		getPersonsWithNegativeScores("/home/danielhosse/run9/output/ITERS/it.0/0.plans.xml.gz");
//		analyzeModeChoice("/home/danielhosse/run8b/output/ITERS/it.10/10.plans.xml.gz", Global.matsimDir + "OUTPUT/" + Global.runID +"/input/subpopulationAtts.xml");
		
	}
	
	private static void getPersonsWithNegativeScores(String plansFile){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		int cnt = 0;
		
//		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
//		lmdd.init(scenario);
//		lmdd.preProcessData();
//		lmdd.postProcessData();
//		lmdd.writeResults("/home/danielhosse/Dokumente/lmdd/");
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Activity){
					
					Activity act = (Activity)pe;
					if(act.getType().contains("48.0H"))
						System.out.println(person.getId());
					
				}
				
			}
			
		}
		
//		System.out.println(cnt + " persons with negative scores...");
		
	}
	
	private static void analyzeModeChoice(String plansFile, String personAttributesFile){
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new MatsimPopulationReader(scenario).readFile(plansFile);
		
		ObjectAttributes subpopAtts = new ObjectAttributes();
		new ObjectAttributesXmlReader(subpopAtts).parse(personAttributesFile);
		
		int nNonCarUsersUsingCar = 0;
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			String userGroup = (String)subpopAtts.getAttribute(person.getId().toString(), "usrGroup");
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Leg){
					
					Leg leg = (Leg)pe;
					
					if(userGroup == null){
						
						if(leg.getMode().equals(TransportMode.car)){
							nNonCarUsersUsingCar++;
							break;
						}
						
					}
					
				}
				
			}
			
		}
		
		System.out.println(nNonCarUsersUsingCar + " persons should not have access to car but are using it!");
		
	}
	
}
