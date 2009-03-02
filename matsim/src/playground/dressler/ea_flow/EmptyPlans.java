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


import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriterV5;
import org.matsim.router.PlansCalcRoute;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.world.World;

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
				
		//read network
		NetworkLayer network = new NetworkLayer();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		networkReader.readFile(networkfile);
	//	Node sink = network.getNode(sinkid);
		
		//Population population = new Population(Population.NO_STREAMING);
		PopulationImpl population = new PopulationImpl(PopulationImpl.NO_STREAMING);
			
		new MatsimPopulationReader(population,network).readFile(plansfile);
		network.connect();
		

		if(_debug){
			System.out.println("reading input done");
		}




		Config config = Gbl.createConfig(new String[] {});

		World world = Gbl.getWorld();
		world.setNetworkLayer(network);
		world.complete();

		CharyparNagelScoringFunctionFactory factory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		PlansCalcRoute router = new PlansCalcRoute(network, new FakeTravelTimeCost(), new FakeTravelTimeCost());
		//PlansCalcRoute router = new PlansCalcRouteDijkstra(network, new FakeTravelTimeCost(), new FakeTravelTimeCost(), new FakeTravelTimeCost());
		for (Object O_person : population.getPersons().values()) {
			Person person = (Person) O_person;
			Plan plan = person.getPlans().get(0);
			router.run(plan);
		}

		PopulationWriterV5 popwriter = new PopulationWriterV5(population);

		try {
			popwriter.writeFile(outputplansfile);
		} catch (Exception e) {
			e.printStackTrace();
		}								


		if(_debug){
			System.out.println("done");
		}
	}

}
