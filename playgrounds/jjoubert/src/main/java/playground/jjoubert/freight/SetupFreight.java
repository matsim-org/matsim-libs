/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.jjoubert.freight;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class SetupFreight {
	static final Logger LOG = Logger.getLogger(SetupFreight.class); 
	
	/* Setup files for jwjoubert. */
	static final String MATSIM_SA = "/Users/jwjoubert/Documents/workspace/";
	static final String QVANHEERDEN_FREIGHT = MATSIM_SA + "sandbox-qvanheerden/input/freight/" ;
	static final String NETFILENAME = MATSIM_SA + "data-nmbm/network/NMBM_Network_CleanV7.xml.gz"  ;
	static final String CARRIERS = QVANHEERDEN_FREIGHT + "myGridSim/carrier.xml" ;
	static final String VEHTYPES = QVANHEERDEN_FREIGHT + "myGridSim/vehicleTypes.xml" ;
	static final String ALGORITHM = QVANHEERDEN_FREIGHT + "myGridSim/initialPlanAlgorithm.xml" ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/* Set up basic MATSim stuff. */ 
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("/Users/jwjoubert/Documents/Temp/freight-runs/output/");
		config.controler().setLastIteration(10);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).parse(NETFILENAME);

		/* Set up freight specific stuff. */
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERS);
		
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPES);
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes);
		
		/* Create a plan for each carrier. The plan is the result of a VRP instance solved. */
		for(Carrier carrier : carriers.getCarriers().values()){
			/* Build a Vehicle Routing Problem (VRP). */
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
			NetworkBasedTransportCosts vrpTransportCosts = 
					NetworkBasedTransportCosts.Builder.newInstance(scenario.getNetwork(), vehicleTypes.getVehicleTypes().values()).build();
			vrpBuilder.setRoutingCost(vrpTransportCosts);
			VehicleRoutingProblem vrp = vrpBuilder.build();
			
			/* Set up the VRP algorithm. */
			VehicleRoutingAlgorithm algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, ALGORITHM);
			
			/* Solve the VRP instances, and create a plan from it. */
			VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
			CarrierPlan carrierPlan = MatsimJspritFactory.createPlan(carrier, solution);
			NetworkRouter.routePlan(carrierPlan, vrpTransportCosts);
			
			carrier.setSelectedPlan(carrierPlan);
		}
		
		/* Write the carrier plans to file. */
		new CarrierPlanXmlWriterV2(carriers).write(config.controler().getOutputDirectory() + "CarrierPlans.xml");
		
//		new Visualiser(config, scenario).visualizeLive(carriers);
		
		RunFreight.runFreight(scenario, carriers);
	}

}
