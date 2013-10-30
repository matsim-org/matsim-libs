/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.usecases.freight;

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
import org.matsim.contrib.freight.utils.Visualiser;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import util.Solutions;
import algorithms.SchrimpfFactory;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;

/**
 * @author nagel
 *
 */
public class KNFreight4 {

	static final String MATSIM_SA = "/Users/Nagel/southafrica/MATSim-SA/" ;
	static final String QVANHEERDEN_FREIGHT=MATSIM_SA+"/sandbox/qvanheerden/input/freight/" ;
	static final String NETFILENAME=QVANHEERDEN_FREIGHT+"/fromWiki/network.xml" ;
	static final String CARRIERS = QVANHEERDEN_FREIGHT+"/fromWiki/carrier.xml" ;
	static final String VEHTYPES = QVANHEERDEN_FREIGHT+"/fromWiki/vehicleTypes.xml" ;
	static final String ALGORITHM = QVANHEERDEN_FREIGHT+"/fromWiki/algorithm.xml" ;

	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(NETFILENAME);
		
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERS) ;
		
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPES) ;
		
		// assign vehicle types to the carriers (who already have their vehicles (??)):
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		
		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, scenario.getNetwork() ) ;
			NetworkBasedTransportCosts netBasedCosts =
					NetworkBasedTransportCosts.Builder.newInstance( scenario.getNetwork()
							, vehicleTypes.getVehicleTypes().values() ).build() ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build() ;

			VehicleRoutingAlgorithm algorithm = algorithms.VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem,ALGORITHM);
//			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

					VehicleRoutingProblemSolution solution = Solutions.getBest(algorithm.searchSolutions());
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;

			NetworkRouter.routePlan(newPlan,netBasedCosts) ;
			// (maybe not optimal, but since re-routing is a matsim strategy, 
			// certainly ok as initial solution)
			
			carrier.setSelectedPlan(newPlan) ;
			
		}
		new CarrierPlanXmlWriterV2(carriers).write("/Users/nagel/freight-kairuns/output/plannedCarrier.xml") ;
		
		new Visualiser( config, scenario).visualizeLive(carriers) ;
		
//		new Visualiser(config,scenario).makeMVI(carriers,"yourFolder/carrierMVI.mvi",1);
	}

}
