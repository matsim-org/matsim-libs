/* *********************************************************************** *
 * project: org.matsim.*
 * MultiSourceEAF.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


/**
 *
 */
package playground.dressler.ea_flow;


import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

/**
 * @author Manuel Schneider
 *
 */
public class EmptyPlans {

	/**
	 * debug flag
	 */
	private static boolean _debug = true;


	public static void debug(boolean debug){
		_debug=debug;
	}



	/**
	 * main method to create shortest path plans from arbitrary plans
	 * @param args b
	 *
	 */
	public static void main(String[] args) {

		if(_debug){
			System.out.println("starting to read input");
		}

		String networkfile = null;
		networkfile = "/homes/combi/Projects/ADVEST/padang/network/padang_net_evac.xml";
		//networkfile = "/Users/manuel/Documents/meine_EA/manu/manu2.xml";
		//networkfile = "./examples/meine_EA/swissold_network_5s.xml";
		//networkfile = "./examples/meine_EA/siouxfalls_network_5s_euclid.xml";


		String plansfile = null;
		//plansfile = "/homes/combi/Projects/ADVEST/padang/plans/padang_plans_10p.xml.gz";
		plansfile = "./examples/meine_EA/padang_plans_100p_flow_2s.xml";
		//plansfile ="/homes/combi/Projects/ADVEST/code/matsim/examples/meine_EA/siouxfalls_plans_simple.xml";
		//plansfile = "/homes/combi/dressler/V/Project/testcases/swiss_old/matsimevac/swiss_old_plans_evac.xml";


		String outputplansfile = null;
		//outputplansfile = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans_10p_5s.xml";
		//outputplansfile = "./examples/meine_EA/swissold_plans_5s_demands_100.xml";
		//outputplansfile = "./examples/meine_EA/padang_plans_100p_flow_2s.xml";
		outputplansfile = "./examples/meine_EA/padang_plans_100p_flow_2s_empty.xml";

		//String sinkid = "supersink";

		ScenarioImpl scenario = new ScenarioImpl();
		//read network
		NetworkLayer network = scenario.getNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(networkfile);
	//	Node sink = network.getNode(sinkid);

		PopulationImpl population = scenario.getPopulation();

		new MatsimPopulationReader(scenario).readFile(plansfile);
		network.connect();


		if(_debug){
			System.out.println("reading input done");
		}




		Config config = Gbl.createConfig(new String[] {});

//		World world = Gbl.getWorld();
//		world.setNetworkLayer(network);
//		world.complete();

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		PlansCalcRoute router = new PlansCalcRoute(config.plansCalcRoute(), network, new FakeTravelTimeCost(), new FakeTravelTimeCost());
		//PlansCalcRoute router = new PlansCalcRouteDijkstra(network, new FakeTravelTimeCost(), new FakeTravelTimeCost(), new FakeTravelTimeCost());
		for (Person person : population.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			router.run(plan);
		}

		new PopulationWriter(population, network).writeFile(outputplansfile);

		if(_debug){
			System.out.println("done");
		}
	}

}
