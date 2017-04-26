package others.sergioo.mains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class NoRouteInPlans implements PersonAlgorithm {
	
	//Attributes

	//Methods
	@Override
	public void run(Person person) {
		for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
			if(planElement instanceof Leg && ((Leg)planElement).getRoute()==null)
				System.out.println();
	}

	//Main
	/**
	 * @param args
	 * 0-Network file
	 * 1-Facilities file
	 * 2-Source population file
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		new MatsimFacilitiesReader((MutableScenario) scenario).readFile(args[1]);
		StreamingDeprecated.setIsStreaming(((Population)scenario.getPopulation()), true);
		StreamingDeprecated.addAlgorithm(((Population)scenario.getPopulation()), new NoRouteInPlans());
		new PopulationReader(scenario).readFile(args[2]);
	}

}
