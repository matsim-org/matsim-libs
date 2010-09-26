package playground.mmoyo.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;

/**
 * Converts an agent in a population file
 */
public class Plan2Population {
	private Scenario scenario;
	String SEPARATOR = ":\t ";
	
	public Plan2Population(Scenario scenario){	
		this.scenario= scenario;
	}
	
	//writes a planFile only with the desired agent
	public void CreatePersonFile(String strPersonId){
		if (scenario.getNetwork()==null){
			throw new NullPointerException("The network could not be found in the scenario" );
		}
		PopulationImpl outputPopulation = new PopulationImpl(new ScenarioImpl());
		outputPopulation.addPerson(this.scenario.getPopulation().getPersons().get(new IdImpl(strPersonId)));
		PopulationWriter popwriter = new PopulationWriter(outputPopulation, this.scenario.getNetwork());
		popwriter.write(scenario.getConfig().controler().getOutputDirectory() + "/person_" + strPersonId + ".xml");
	}
	
	public static void main(String[] args) {
		String configFile;
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";	
		}
		Scenario scenario = new TransScenarioLoader().loadScenario(configFile);
		Plan2Population plan2Population = new Plan2Population(scenario);
		plan2Population.CreatePersonFile("11101104X1");
	}
}
