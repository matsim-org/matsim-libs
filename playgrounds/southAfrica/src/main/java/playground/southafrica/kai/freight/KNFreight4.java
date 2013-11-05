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
package playground.southafrica.kai.freight;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

import util.Solutions;
import basics.VehicleRoutingAlgorithm;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblemSolution;

/**
 * @author nagel
 *
 */
public class KNFreight4 {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(KNFreight4.class) ;

	private static final String MATSIM_SA = "/Users/Nagel/southafrica/MATSim-SA/" ;

	private static final String QVANHEERDEN_FREIGHT=MATSIM_SA+"/sandbox/qvanheerden/input/freight/" ;

	//	private static final String NETFILENAME=QVANHEERDEN_FREIGHT+"/scenarioFromWiki/network.xml" ;
	//	private static final String CARRIERS = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/carrier.xml" ;
	//	private static final String VEHTYPES = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/vehicleTypes.xml" ;
	//	private static final String ALGORITHM = QVANHEERDEN_FREIGHT+"/scenarioFromWiki/algorithm.xml" ;

	private static final String NETFILENAME = MATSIM_SA + "data/areas/nmbm/network/NMBM_Network_CleanV7.xml.gz"  ;
//	private static final String NETFILENAME = MATSIM_SA + "data/areas/nmbm/network/NMBM_Network_FullV7.xml.gz"  ;
	private static final String CARRIERS = QVANHEERDEN_FREIGHT + "myGridSim/carrier.xml" ;
	private static final String VEHTYPES = QVANHEERDEN_FREIGHT + "myGridSim/vehicleTypes.xml" ;
	private static final String ALGORITHM = QVANHEERDEN_FREIGHT + "myGridSim/initialPlanAlgorithm.xml" ;
	//	private static final String ALGORITHM = QVANHEERDEN_FREIGHT + "myGridSim/algorithm.xml" ;


	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		if ((args == null) || (args.length == 0)) {
			config.controler().setOutputDirectory("/Users/nagel/freight-kairuns/output/");
		} else {
			System.out.println( "args[0]:" + args[0] );
			config.controler().setOutputDirectory( args[0] );
		}
		config.controler().setLastIteration(0);
		
		config.network().setInputFile(NETFILENAME);
//		config.network().setTimeVariantNetwork(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);

//		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
//		for ( Link link : scenario.getNetwork().getLinks().values() ) {
//			double speed = link.getFreespeed() ;
//			final double threshold = 5./3.6;
//			if ( speed > threshold ) {
//				{
//					NetworkChangeEvent event = cef.createNetworkChangeEvent(7.*3600.) ;
//					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  threshold ));
//					event.addLink(link);
//					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//				}
//				{
//					NetworkChangeEvent event = cef.createNetworkChangeEvent(9.*3600.) ;
//					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE,  speed ));
//					event.addLink(link);
//					((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
//				}
//			}
//		}

		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).read(CARRIERS) ;

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).read(VEHTYPES) ;

		// assign vehicle types to the carriers (who already have their vehicles (??)):
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, scenario.getNetwork() ) ;
			NetworkBasedTransportCosts netBasedCosts = NetworkBasedTransportCosts.Builder.newInstance( 
					scenario.getNetwork(), vehicleTypes.getVehicleTypes().values() ).build() ;
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

		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "plannedCarrier.xml") ;

//		new Visualiser( config, scenario).visualizeLive(carriers) ;

		//		new Visualiser(config,scenario).makeMVI(carriers,"yourFolder/carrierMVI.mvi",1);

		KNFreight3.run(scenario, carriers);

	}

}
