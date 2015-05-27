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
 * 
 * Choose the number of persons you like to simulate, 
 * their starting times and their initial routes before running this class.
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
		boolean sameStartTime = false;
		boolean createZRoute = true;
		boolean createAllRoutes = true;
		String outputDir = DgPaths.SHAREDSVN
				+ "studies/tthunig/scenarios/BraessWoSignals/";
//				+ "projects/cottbus/data/scenarios/braess_scenario/";
		String popOutputFile = outputDir + "plans" + numberOfPersons;
		if (sameStartTime)
			popOutputFile += "SameStartTime";
		if (createAllRoutes){
			popOutputFile += "AllRoutes";
			createZRoute = true; // especially... needed while creating legs
		}
		else if (createZRoute)
			popOutputFile += "RouteZ";
		popOutputFile += ".xml";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(outputDir + "network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population pop = PopulationUtils.createPopulation(config, network);

		TtCreateBraessPopulation creator = new TtCreateBraessPopulation(pop, network);
		creator.createPersons(numberOfPersons, sameStartTime, createAllRoutes, createZRoute);
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
	 * @param createZRoute 
	 */
	private void createPersons(int numberOfPersons, boolean sameStartTime, boolean createAllRoutes, boolean createZRoute) {

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
			if (createZRoute){
				// create a route for the Z path
				List<Id<Link>> pathZ = new ArrayList<>();
				pathZ.add(Id.create(2, Link.class));
				pathZ.add(Id.create(4, Link.class));
				pathZ.add(Id.create(6, Link.class));
				Route routeZ = new LinkNetworkRouteImpl(Id.create(1, Link.class), pathZ , Id.create(7, Link.class));
				leg.setRoute(routeZ);
			}		
			plan.addLeg(leg);
			
			// add a drain activity at link 7
			plan.addActivity(population.getFactory().
					createActivityFromLinkId("dummy", Id.create(7, Link.class)));
			
			// store information in population
			person.addPlan(plan);			
			population.addPerson(person);

			// copy plan if different routes should be created
			if (createAllRoutes){
				// create the second plan
				Plan plan2 = population.getFactory().createPlan();
				
				// add the same start activity as for the first plan
				plan2.addActivity(startAct);
				
				// add a leg with the upper path
				Leg legUp = population.getFactory().createLeg(TransportMode.car);
				List<Id<Link>> pathUp = new ArrayList<>();
				pathUp.add(Id.create(2, Link.class));
				pathUp.add(Id.create(5, Link.class));
				Route routeUp = new LinkNetworkRouteImpl(Id.create(1, Link.class), 
						pathUp, Id.create(7, Link.class));
				legUp.setRoute(routeUp);
				plan2.addLeg(legUp);
				
				// add the same drain activity as for the first plan
				plan2.addActivity(population.getFactory().
						createActivityFromLinkId("dummy", Id.create(7, Link.class)));
				
				person.addPlan(plan2);
				
				
				// create the third plan
				Plan plan3 = population.getFactory().createPlan();

				// add the same start activity as for the first plan
				plan3.addActivity(startAct);
				
				// add a leg with the lower path
				Leg legDown = population.getFactory().createLeg(TransportMode.car);
				List<Id<Link>> pathDown = new ArrayList<>();
				pathDown.add(Id.create(3, Link.class));
				pathDown.add(Id.create(6, Link.class));
				Route routeDown = new LinkNetworkRouteImpl(Id.create(1, Link.class), 
						pathDown, Id.create(7, Link.class));
				legDown.setRoute(routeDown);
				plan3.addLeg(legDown);
				
				// add a drain activity at link 7
				plan3.addActivity(population.getFactory().
						createActivityFromLinkId("dummy", Id.create(7, Link.class)));
				
				person.addPlan(plan3);				
			}
			
		}

	}

}
