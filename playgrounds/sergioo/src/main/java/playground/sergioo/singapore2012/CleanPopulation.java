package playground.sergioo.singapore2012;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class CleanPopulation {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[0]);
		(new MatsimFacilitiesReader((MutableScenario)scenario)).readFile(args[1]);
		(new PopulationReader(scenario)).readFile(args[2]);
		int k=0;
		for(Person person:scenario.getPopulation().getPersons().values())
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Activity) {
					ActivityFacility facility = ((MutableScenario)scenario).getActivityFacilities().getFacilities().get(((Activity)planElement).getFacilityId());
					Map<String, ActivityOption> options = facility.getActivityOptions();
					String type = ((Activity)planElement).getType();
					if(!options.keySet().contains(type)) {
						System.out.println(++k+" "+person.getId()+" "+type+" "+facility.getId());
						options.put(type, new ActivityOptionImpl(type));
					}
				}
		(new FacilitiesWriter(((MutableScenario)scenario).getActivityFacilities())).write(args[3]);
	}

}
