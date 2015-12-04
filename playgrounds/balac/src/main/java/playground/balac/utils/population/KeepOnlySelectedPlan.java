package playground.balac.utils.population;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class KeepOnlySelectedPlan {

	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
	//	new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(args[1]);
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		for (Person p : scenario.getPopulation().getPersons().values()) {
			
			Person p1 = scenario2.getPopulation().getFactory().createPerson(p.getId());
			p1.addPlan(p.getSelectedPlan());
			p1.setSelectedPlan(p.getSelectedPlan());
			
			PersonUtils.setAge(p1, PersonUtils.getAge(p));
			PersonUtils.setCarAvail(p1, PersonUtils.getCarAvail(p));
			PersonUtils.setSex(p1, PersonUtils.getSex(p));
			PersonUtils.setLicence(p1, PersonUtils.getLicense(p));
			scenario2.getPopulation().addPerson(p1);
		}
		
		
		
		new PopulationWriter(scenario2.getPopulation(), scenario.getNetwork()).writeFileV4(args[2] + "/plans_normal.xml.gz");		

	}

}
