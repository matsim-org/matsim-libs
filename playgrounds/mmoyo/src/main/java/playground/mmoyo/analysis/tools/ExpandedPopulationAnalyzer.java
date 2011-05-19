package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**Counts the number of original agents and expanded agents */ 
public class ExpandedPopulationAnalyzer {
	Scenario scenario;
	
	public ExpandedPopulationAnalyzer(Scenario scenario){
		this.scenario = scenario;
	}
	
	public void count(){
		int originalAgents = 0;
		int clons = 0;
			
		final String X= "X";
		for (Person person : this.scenario.getPopulation().getPersons().values() ){
			String strId = person.getId().toString();
			if (strId.indexOf(X)==-1){
				originalAgents++;	
			}else {
				clons++;
			}
		}

		System.out.println("Agents:  \t" + this.scenario.getPopulation().getPersons().size());
		System.out.println("original:\t" + originalAgents);
		System.out.println("clons:  \t" + clons);
		
		if (originalAgents + clons != this.scenario.getPopulation().getPersons().size() ){
			System.out.println ("The sum does not match");
		}
	}

	public static void main(String[] args) {
		String configFile = null;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/fouth/config.xml";
		}
		
		ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile);
		Scenario scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		new ExpandedPopulationAnalyzer(scenario).count();
	}
}
