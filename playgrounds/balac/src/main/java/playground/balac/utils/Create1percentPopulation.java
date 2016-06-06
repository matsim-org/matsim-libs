package playground.balac.utils;



import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

public class Create1percentPopulation {

	
	public void run(String plansFilePath, String networkFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
	//	new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
				
		
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		
		for (int i = 0; i < size; i++) {
			if (i % 10 != 0) {				
				scenario.getPopulation().getPersons().remove(((Person)arr[i]).getId());
				
			}	
			else {
				
				Person person = (Person)arr[i];
				Plan plan = person.getSelectedPlan();
				((Activity)plan.getPlanElements().get(0)).setStartTime(Time.UNDEFINED_TIME);
			}
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4("C:/Users/balacm/Documents/InducedDemand/plans_1perc.xml.gz");		
		
	}
	
	public static void main(String[] args) {
		
		Create1percentPopulation cp = new Create1percentPopulation();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		//String facilitiesfilePath = args[2];
		//String outputFolder = args[2];
		
		cp.run(plansFilePath, networkFilePath);
	}

}
