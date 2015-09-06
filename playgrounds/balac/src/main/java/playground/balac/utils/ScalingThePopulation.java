package playground.balac.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.Desires;

public class ScalingThePopulation {

	
	public void run(String plansFilePath, String networkFilePath, String outputFilePath) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkFilePath);
		populationReader.readFile(plansFilePath);
		
				
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		String j = "c";
		for (int i = 1; i < size; i++) {
			if (i % 2 == 0) {
				
				PersonImpl p = (PersonImpl)scenario.getPopulation().getFactory().createPerson(Id.create(((Person)arr[i]).getId().toString() + j, Person.class));
				p.addPlan(((Person)arr[i]).getSelectedPlan());
			
				PersonImpl originalPerson = (PersonImpl)((Person)arr[i]);
				
				p.setAge(originalPerson.getAge());
				p.setCarAvail(originalPerson.getCarAvail());
				p.setLicence(originalPerson.getLicense());
				p.setSex(originalPerson.getSex());
				p.setEmployed(originalPerson.isEmployed());
				p.addTravelcard("unknown");
				//Desires d = originalPerson.getDesires();
				//p.createDesires(d.getDesc());
				//p.getDesires().setDesc(d.getDesc());
				//for (String type : d.getActivityDurations().keySet()) {
				//
				//	p.getDesires().putActivityDuration(type, d.getActivityDuration(type));
				//}
				
				scenario.getPopulation().addPerson(p);
			}			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outputFilePath + "/population_150%.xml");		
		
	}
	
	public static void main(String[] args) {
		
		ScalingThePopulation cp = new ScalingThePopulation();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String outputFolder = args[2];
		
		cp.run(plansFilePath, networkFilePath, outputFolder);
	}

}
