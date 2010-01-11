package playground.mmoyo.analysis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class PTLegIntoPlanConverter {

	public PTLegIntoPlanConverter(ScenarioImpl scenario) {
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String personId = person.getId().toString();
			int ptConnectionIndex=0;
			for (Plan plan: person.getPlans()){
				for (PlanElement pe : plan.getPlanElements() ){
					if (pe instanceof Leg) {																
						Leg leg = (Leg) pe;
					}
				}
					//person.setId(id)
					//create new plan
					Plan newPlan= new PlanImpl();
					new IdImpl(personId + " "  + ptConnectionIndex); 
					
			}
		}
	}

	
	public static void main(String[] args) {
		String configFile = args[0];
		double startTime = System.currentTimeMillis();
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		new PTLegIntoPlanConverter(scenario);
		System.out.println("total duration: " + (System.currentTimeMillis()-startTime));
	}

}
