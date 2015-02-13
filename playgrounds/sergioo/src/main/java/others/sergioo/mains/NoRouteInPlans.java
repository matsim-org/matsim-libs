package others.sergioo.mains;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.population.algorithms.PersonAlgorithm;

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
		new MatsimNetworkReader(scenario).readFile(args[0]);
		new MatsimFacilitiesReader((ScenarioImpl) scenario).readFile(args[1]);
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new NoRouteInPlans());
		new MatsimPopulationReader(scenario).readFile(args[2]);
	}

}
