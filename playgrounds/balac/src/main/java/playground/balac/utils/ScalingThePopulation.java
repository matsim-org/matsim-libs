package playground.balac.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class ScalingThePopulation {

	
	public void run(String plansFilePath, String networkFilePath, String outputFilePath) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
		populationReader.readFile(plansFilePath);
		
				
		int size = scenario.getPopulation().getPersons().values().size();
		Object[] arr = scenario.getPopulation().getPersons().values().toArray();
		String j = "c";
		for (int i = 1; i < size; i++) {
			if (i % 2 == 0) {
				
				Person p = (Person)scenario.getPopulation().getFactory().createPerson(Id.create(((Person)arr[i]).getId().toString() + j, Person.class));
				p.addPlan(((Person)arr[i]).getSelectedPlan());
			
				Person originalPerson = (Person)((Person)arr[i]);
				
				PersonUtils.setAge(p, PersonUtils.getAge(originalPerson));
				PersonUtils.setCarAvail(p, PersonUtils.getCarAvail(originalPerson));
				PersonUtils.setLicence(p, PersonUtils.getLicense(originalPerson));
				PersonUtils.setSex(p, PersonUtils.getSex(originalPerson));
				PersonUtils.setEmployed(p, PersonUtils.isEmployed(originalPerson));
				PersonUtils.addTravelcard(p, "unknown");
//				Desires d = originalPerson.getDesires();
//				p.createDesires(d.getDesc());
//				p.getDesires().setDesc(d.getDesc());
//				for (String type : d.getActivityDurations().keySet()) {
//
//					p.getDesires().putActivityDuration(type, d.getActivityDuration(type));
//				}
				
				scenario.getPopulation().addPerson(p);
			}			
		}
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(outputFilePath + "/population_150%.xml");		
		
	}
	
	public static void main(String[] args) {
		
		ScalingThePopulation cp = new ScalingThePopulation();
				
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String outputFolder = args[2];
		
		cp.run(plansFilePath, networkFilePath, outputFolder);
	}

}
