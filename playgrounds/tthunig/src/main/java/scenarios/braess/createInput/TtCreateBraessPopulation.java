/**
 * 
 */
package scenarios.braess.createInput;

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
 * their starting times and their initial routes 
 * before running this class or calling the method createPersons(...)
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
	 * @param args
	 *            not used
	 */
	public static void main(String[] args) {

		int numberOfPersons = 60;
		boolean sameStartTime = true;
		boolean createAllRoutes = false;

		String outputDir = DgPaths.SHAREDSVN
				+ "projects/cottbus/data/scenarios/braess_scenario/";

		String popOutputFile = outputDir + "plans" + numberOfPersons;
		if (sameStartTime)
			popOutputFile += "SameStartTime";
		if (createAllRoutes) {
			popOutputFile += "AllRoutes";
		}
		popOutputFile += ".xml";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(outputDir + "basicNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population pop = PopulationUtils.createPopulation(config, network);

		TtCreateBraessPopulation creator = new TtCreateBraessPopulation(pop,
				network);
		creator.createPersons(numberOfPersons, sameStartTime, createAllRoutes);
		creator.writePersons(popOutputFile);
	}

	public void writePersons(String popOutputFile) {
		new PopulationWriter(population, network).write(popOutputFile);
	}
	
	/**
	 * Calls createPersons(int, boolean, int) with the correct numberOfInitRoutes.
	 * 
	 * @param numberOfPersons
	 * @param sameStartTime
	 * @param createAllRoutes
	 */
	public void createPersons(int numberOfPersons, boolean sameStartTime,
			boolean createAllRoutes) {
		
		if (createAllRoutes)
			createPersons(numberOfPersons, sameStartTime, 3);
		else
			createPersons(numberOfPersons, sameStartTime, 0);
	}

	/**
	 * Fills a population container with the given number of persons. All
	 * persons travel from the left to the right through the network as in
	 * Braess's original paradox.
	 * 
	 * If sameStartTime is true, all agents start their trip at 8 am. If not,
	 * the agents start after each other with one second gaps, the first one at
	 * 8 am.
	 * 
	 * @param numberOfPersons
	 * @param sameStartTime
	 * @param numberOfInitRoutes number of routes that the agents get as initial plans
	 */
	public void createPersons(int numberOfPersons, boolean sameStartTime,
			int numberOfInitRoutes) {
		
		for (int i = 0; i < numberOfPersons; i++) {

			// create a person and a plan container
			Person person = population.getFactory().createPerson(
					Id.createPersonId(i));
			Plan plan = population.getFactory().createPlan();

			// add a start activity at link 0_1
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("0_1"));
			if (sameStartTime) {
				// 8:00 am.
				startAct.setEndTime(8 * 3600);
			} else {
				// 8:00 am. plus i seconds
				startAct.setEndTime(8 * 3600 + i);
			}
			plan.addActivity(startAct);

			// add a leg
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			if (numberOfInitRoutes >= 1) {
				// create a route for the Z path
				List<Id<Link>> pathZ = new ArrayList<>();
				pathZ.add(Id.createLinkId("1_2"));
				pathZ.add(Id.createLinkId("2_3"));
				pathZ.add(Id.createLinkId("3_4"));
				pathZ.add(Id.createLinkId("4_5"));
				Route routeZ = new LinkNetworkRouteImpl(
						Id.createLinkId("0_1"), pathZ, Id.createLinkId("5_6"));
				leg.setRoute(routeZ);
			}
			plan.addLeg(leg);

			// add a drain activity at link 5_6
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("5_6"));
			plan.addActivity(drainAct);

			// store information in population
			person.addPlan(plan);
			population.addPerson(person);

			// copy plan if different routes should be created
			if (numberOfInitRoutes >= 2) {
				// create the second plan
				Plan plan2 = population.getFactory().createPlan();

				// add the same start activity as for the first plan
				plan2.addActivity(startAct);

				// add a leg with the upper path
				Leg legUp = population.getFactory()
						.createLeg(TransportMode.car);
				List<Id<Link>> pathUp = new ArrayList<>();
				pathUp.add(Id.createLinkId("1_2"));
				pathUp.add(Id.createLinkId("2_3"));
				pathUp.add(Id.createLinkId("3_5"));
				Route routeUp = new LinkNetworkRouteImpl(
						Id.createLinkId("0_1"), pathUp, Id.createLinkId("5_6"));
				legUp.setRoute(routeUp);
				plan2.addLeg(legUp);

				// add the same drain activity as for the first plan
				plan2.addActivity(drainAct);

				person.addPlan(plan2);
				
				if (numberOfInitRoutes >= 3){
					// create the third plan
					Plan plan3 = population.getFactory().createPlan();

					// add the same start activity as for the first plan
					plan3.addActivity(startAct);

					// add a leg with the lower path
					Leg legDown = population.getFactory().createLeg(
							TransportMode.car);
					List<Id<Link>> pathDown = new ArrayList<>();
					pathDown.add(Id.createLinkId("1_2"));
					pathDown.add(Id.createLinkId("2_4"));
					pathDown.add(Id.createLinkId("4_5"));
					Route routeDown = new LinkNetworkRouteImpl(
							Id.createLinkId("0_1"), pathDown,
							Id.createLinkId("5_6"));
					legDown.setRoute(routeDown);
					plan3.addLeg(legDown);

					// add the same drain activity as for the first plan
					plan3.addActivity(drainAct);

					person.addPlan(plan3);
				}
			}
		}
	}

}
