package playground.sergioo.mixedtraffic2016;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ModifyMATSimFiles {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new PopulationReader(scenario).readFile(args[1]);
		for(Person person:scenario.getPopulation().getPersons().values()) {
			boolean walker = false;
			PLANS:
			for(Plan plan:person.getPlans())
				for(PlanElement element:plan.getPlanElements())
					if(element instanceof Leg && ((Leg)element).getMode().equals("passenger")) {
						walker = true;
						break PLANS;
					}
			if(Math.random()>0.2)
				for(Plan plan:person.getPlans())
					for(PlanElement element:plan.getPlanElements())
						if(element instanceof Leg && ((Leg)element).getMode().equals("passenger")) {
							((Leg)element).setMode("motorbike");
						}
		}
		new PopulationWriter(scenario.getPopulation()).writeV5(args[2]);
		/*new MatsimNetworkReader(scenario.getNetwork()).readFile(args[3]);
		for(Link link:scenario.getNetwork().getLinks().values())
			if(link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add("motorbike");
				link.setAllowedModes(modes);
			}
		new NetworkWriter(scenario.getNetwork()).writeV1(args[4]);*/
	}

}
