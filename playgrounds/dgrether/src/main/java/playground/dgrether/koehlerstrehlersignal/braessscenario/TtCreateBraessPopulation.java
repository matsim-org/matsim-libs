/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.braessscenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;

/**
 * Class to create a population for the braess scenario.
 * Choose the number of persons you like to simulate before starting this class.
 * 
 * @author tthunig
 */
public class TtCreateBraessPopulation {

	private Population population;
	private Network network;

	public TtCreateBraessPopulation(Population pop, Network net) {
		this.population = pop;
		this.network = net;
	}

	/**
	 * @param args not used
	 */
	public static void main(String[] args) {
		
		int numberOfPersons = 60;
		boolean sameStartTime = true;
		boolean createRoutes = true;
		String outputDir = DgPaths.SHAREDSVN
				+ "studies/tthunig/scenarios/BraessWoSignals/";
//				+ "projects/cottbus/data/scenarios/braess_scenario/";
		String popOutputFile = outputDir + "plans" + numberOfPersons;
		if (sameStartTime)
			popOutputFile += "SameStartTime";
		if (createRoutes)
			popOutputFile += "WithRoutes";
		popOutputFile += ".xml";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(outputDir + "network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population pop = PopulationUtils.createPopulation(config, network);

		TtCreateBraessPopulation creator = new TtCreateBraessPopulation(pop, network);
		creator.createPersons(numberOfPersons, sameStartTime, createRoutes);
		creator.writePersons(popOutputFile);
	}

	private void writePersons(String popOutputFile) {
		new PopulationWriter(population, network).write(popOutputFile);
	}

	/**
	 * Fills a population container with the given number of persons. All
	 * persons travel from the left to the right through the network as in
	 * Braess's original paradox.
	 * 
	 * If sameStartTime is true, all agents start their trip at 8 am.
	 * If not, the agents start after each other with one second gaps, 
	 * the first one at 8 am.
	 * 
	 * @param numberOfPersons
	 * @param sameStartTime 
	 * @param createRoutes 
	 */
	private void createPersons(int numberOfPersons, boolean sameStartTime, boolean createRoutes) {

		for (int i = 0; i < numberOfPersons; i++) {

			// create a person and a plan container
			Person person = population.getFactory().createPerson(
					Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();

			// add a start activity at link 1
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.create(1, Link.class));
			if (sameStartTime){
				// 8:00 am.
				startAct.setEndTime(8*3600);
			}
			else{
				 // 8:00 am. plus i seconds
				startAct.setEndTime(8*3600 + i);
			}
			plan.addActivity(startAct);
			
			// add a leg
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			if (createRoutes){
				// create a route for the Z path
				List<Id<Link>> pathZ = new ArrayList<>();
				pathZ.add(Id.create(2, Link.class));
				pathZ.add(Id.create(4, Link.class));
				pathZ.add(Id.create(6, Link.class));
				Route route = new LinkNetworkRouteImpl(Id.create(1, Link.class), pathZ , Id.create(7, Link.class));
				leg.setRoute(route);
			}		
			plan.addLeg(leg);
			
			// add a drain activity at link 7
			plan.addActivity(population.getFactory().
					createActivityFromLinkId("dummy", Id.create(7, Link.class)));

			// store information in population
			person.addPlan(plan);
			population.addPerson(person);

		}

	}

}
