package playground.dressler.control;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;

import playground.dressler.ea_flow.Flow;
//
public class ExampleMain {

	public static void main(final String[] args) {

		FlowCalculationSettings settings = new FlowCalculationSettings();

		NetworkImpl network = null;
		HashMap<Node, Integer> demands = null;

		// TODO
		/* Now get the network and demands from somewhere!
		 * Positive demand for sources, negative for sinks.
		 * One good way is to read a plansfile.xml and set the demand on startlink.getToNode()
		 * This can be done as follows: */

		// ScenarioImpl scenario = ...
		// network = scenario.getNetwork();
		// demands = MultiSourceEAF.parsePopulation(scenario);

		settings.setNetwork(network);
		settings.setDemands(demands);

		/* Choose a supersink, if desired. This disables all other sinks, including shelters! */
		// settings.supersink = network.getNodes().get(new IdImpl("en1"));

		/* Choose the scale
		 * Link travel times are getLength()/getFreespeed() rounded to timeStep
		 * Link capacities are getCap()/getCapperiod(),
		 *   adjusted for timeStep and multiplied by flowFactor
		 */

		// settings.timeStep = 1; // default 1, lengths as given
		// settings.flowFactor = 1.0; // default 1.0, capacities as given


		/* Choose parameters for the search itself.
		 * The search with shelters is the (safe) default.
		 * For unlimited sinks the following options are recommended.
		 */
		 // settings.useSinkCapacities = false;
		 // settings.searchAlgo = FlowCalculationSettings.SEARCHALGO_REVERSE;
		 // settings.useRepeatedPaths = true;
		 // settings.trackUnreachableVertices = true;

		/*
		 * to disable an edge during a certain time, adjust the following
		 */

		// settings.whenAvailable = new HashMap<Link, Interval>();
		// when link is available in time [a, b)  (it is unavailable starting at b) use
		// settings.whenAvailable.put(link, new Interval(a,b));
		// "null" means always available


		/* --------- the actual work starts --------- */

		Flow fluss;

		boolean settingsOkay = settings.prepare();
		settings.printStatus();

		if(!settingsOkay) {
			System.out.println("Something was bad, aborting.");
			return;
		}

		fluss = MultiSourceEAF.calcEAFlow(settings,null);

		/* --------- the actual work is done --------- */


		/* output to screen */
		int[] arrivals = fluss.arrivals();
		long totalcost = 0;
		for (int i = 0; i < arrivals.length; i++) {
			totalcost += i*arrivals[i];
		}

		System.out.println("Total cost: " + totalcost);
		System.out.println("Collected " + fluss.getPaths().size() + " paths.");

		System.out.println(fluss.arrivalsToString());
		System.out.println(fluss.arrivalPatternToString());
		System.out.println("unsatisfied demands:");
		for (Node node : fluss.getDemands().keySet()){
			int demand = fluss.getDemands().get(node);
			if (demand > 0) {
				System.out.println("node:" + node.getId().toString()+ " demand:" + demand);
			}
		}

		/* the flow on a link at time t can be accessed with the following */
		// int somevalue = fluss.getFlow(link).getFlowAt(t);

		/* The following will (hopefully) create a population out of the flow units
		 */

		
		PopulationCreator popcreator = new PopulationCreator(settings);
			
		Population output = popcreator.createPopulation(fluss.getPaths());
	}

}
