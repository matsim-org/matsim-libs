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
 * before calling the method createPersons(...)
 * 
 * @author tthunig
 */
public class TtCreateBraessPopulation {

	private Population population;
	private Network network;
	
	private int numberOfPersons;
	
	private boolean simulateInflowCap7 = false;
	private boolean simulateInflowCap8 = false;
	private boolean simulateInflowCap9 = false;
	
	public TtCreateBraessPopulation(Population pop, Network net) {
		this.population = pop;
		this.network = net;
		
		prepareFields();
	}

	/**
	 * Checks whether the network simulates inflow capacity at links 2_3, 2_4
	 * and 4_5 or not.
	 * 
	 * If the network contains nodes 7, 8 or 9, it simulates inflow capacity;
	 * otherwise it doesn't.
	 * 
	 * The boolean simulateInflowCap is necessary for creating initial plans in
	 * createPersons(...)
	 */
	private void prepareFields() {
		
		if (this.network.getNodes().containsKey(Id.createNodeId(7)))
			this.simulateInflowCap7 = true;
		if (this.network.getNodes().containsKey(Id.createNodeId(8)))
			this.simulateInflowCap8 = true;
		if (this.network.getNodes().containsKey(Id.createNodeId(9)))
			this.simulateInflowCap9 = true;
	}
	
	/**
	 * Calls createPersons(int, Double) with an initial plan
	 * score of null.
	 * 
	 * @param numberOfInitRoutes
	 */
	public void createPersons(int numberOfInitRoutes) {
		
		createPersons(numberOfInitRoutes, null);
	}
	
	/**
	 * Calls createPersons(boolean, Double) with an initial plan
	 * score of null.
	 * 
	 * @param createAllRoutes
	 */
	public void createPersons(boolean createAllRoutes) {
		
		createPersons(createAllRoutes, null);
	}
	
	/**
	 * Calls createPersons(int, int) with the correct number of initial
	 * routes.
	 * 
	 * @param createAllRoutes
	 * @param initPlanScore
	 */
	public void createPersons(boolean createAllRoutes, Double initPlanScore) {
		
		if (createAllRoutes)
			createPersons(3, initPlanScore);
		else
			createPersons(0, initPlanScore);
	}

	/**
	 * Fills a population container with the given number of persons. All
	 * persons travel from the left to the right through the network as in
	 * Braess's original paradox.
	 * 
	 * All agents start after each other in one second gaps, the first one at 8
	 * am.
	 * 
	 * If numberOfInitRoutes is zero, all agents are initialized with no initial
	 * routes. If it is three they are initialized with all three routes in this
	 * scenario, whereby every second agent gets the upper and every other agent
	 * the lower route as initial selected route.
	 * 
	 * @param numberOfInitRoutes
	 *            number of routes that the agents get as initial plans
	 * @param initPlanScore
	 *            initial score for all plans the persons will get
	 */
	public void createPersons(int numberOfInitRoutes, Double initPlanScore) {
		
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person and a plan container
			Person person = population.getFactory().createPerson(
					Id.createPersonId(i));
			Plan plan = population.getFactory().createPlan();

			// add a start activity at link 0_1
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("0_1"));
		
			// 8:00 am. plus i seconds
			startAct.setEndTime(8 * 3600 + i);
		
			plan.addActivity(startAct);

			// add a leg
			Leg leg = population.getFactory().createLeg(TransportMode.car);
			if (numberOfInitRoutes >= 1) {
				// create a route for the Z path
				List<Id<Link>> pathZ = new ArrayList<>();
				pathZ.add(Id.createLinkId("1_2"));
				if (!this.simulateInflowCap7){
					pathZ.add(Id.createLinkId("2_3"));
				}
				else{
					pathZ.add(Id.createLinkId("2_7"));
					pathZ.add(Id.createLinkId("7_3"));
				}
				pathZ.add(Id.createLinkId("3_4"));
				if (!this.simulateInflowCap9){
					pathZ.add(Id.createLinkId("4_5"));
				}
				else{
					pathZ.add(Id.createLinkId("4_9"));
					pathZ.add(Id.createLinkId("9_5"));
				}
				Route routeZ = new LinkNetworkRouteImpl(
						Id.createLinkId("0_1"), pathZ, Id.createLinkId("5_6"));
				leg.setRoute(routeZ);
			}
			plan.addLeg(leg);
			
			// set an initial plan score
			plan.setScore(initPlanScore);

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
				if (!this.simulateInflowCap7){
					pathUp.add(Id.createLinkId("2_3"));
				}
				else{
					pathUp.add(Id.createLinkId("2_7"));
					pathUp.add(Id.createLinkId("7_3"));
				}
				pathUp.add(Id.createLinkId("3_5"));
				Route routeUp = new LinkNetworkRouteImpl(
						Id.createLinkId("0_1"), pathUp, Id.createLinkId("5_6"));
				legUp.setRoute(routeUp);
				plan2.addLeg(legUp);
				
				// set an initial plan score
				plan2.setScore(initPlanScore);
				
				// add the same drain activity as for the first plan
				plan2.addActivity(drainAct);

				person.addPlan(plan2);
				
				// select plan2 for every second person (with even id)
				if (i % 2 == 0){
					person.setSelectedPlan(plan2);
				}

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
					if (!this.simulateInflowCap8){
						pathDown.add(Id.createLinkId("2_4"));
					}
					else{
						pathDown.add(Id.createLinkId("2_8"));
						pathDown.add(Id.createLinkId("8_4"));
					}
					if (!this.simulateInflowCap9){
						pathDown.add(Id.createLinkId("4_5"));
					}
					else{
						pathDown.add(Id.createLinkId("4_9"));
						pathDown.add(Id.createLinkId("9_5"));
					}
					Route routeDown = new LinkNetworkRouteImpl(
							Id.createLinkId("0_1"), pathDown,
							Id.createLinkId("5_6"));
					legDown.setRoute(routeDown);
					plan3.addLeg(legDown);

					// set an initial plan score
					plan3.setScore(initPlanScore);

					// add the same drain activity as for the first plan
					plan3.addActivity(drainAct);

					person.addPlan(plan3);
					
					// select plan3 for every second person (with odd id)
					if (i % 2 == 1){
						person.setSelectedPlan(plan3);
					}
				}
			}
		}
	}

	public void setNumberOfPersons(int numberOfPersons) {
		this.numberOfPersons = numberOfPersons;
	}

}
