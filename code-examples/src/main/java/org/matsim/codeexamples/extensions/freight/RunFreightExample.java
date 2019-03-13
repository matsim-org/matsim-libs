/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.freight;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;


/**
 * @see org.matsim.contrib.freight
 */
public class RunFreightExample {

	private static URL scenarioUrl ;
	static{
		scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
	}

	public static void main(String[] args){

		// ### config stuff: ###
		Config config = createConfig();

		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Building the Carriers, running jsprit for solving the VRP:
		final Carriers carriers = jspritRun( config, scenario.getNetwork() );
		scenario.addScenarioElement( FreightUtils.CARRIERS, carriers );

		//final MATSim configurations and start of the MATSim-Run:
		final Controler controler = new Controler( scenario ) ;
		Freight.configure( controler );

		// run the matsim controler:
		controler.run();
	}

	private static Config createConfig() {
		Config config = ConfigUtils.loadConfig( IOUtils.newUrl(scenarioUrl, "config.xml" ) );

		config.controler().setOutputDirectory("./output/freight");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		new OutputDirectoryHierarchy( config.controler().getOutputDirectory(), config.controler().getRunId(), config.controler().getOverwriteFileSetting() ) ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );
		// (the directory structure is needed for jsprit output, which is before the controler starts.  Maybe there is a better alternative ...)

		config.global().setRandomSeed(4177);

		config.controler().setLastIteration(0);
		// yyyyyy iterations currently do not work; needs to be fixed.

		return config;
	}

	private static Carriers jspritRun(Config config, Network network) {

		//create or load carrier vehicle types
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader( vehicleTypes ).readURL( IOUtils.newUrl(scenarioUrl, "vehicleTypes.xml" ) ) ;

		//create or laod the carrier(s) including assignment of vehicle types to the carrier(s)
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2( carriers ).readURL( IOUtils.newUrl(scenarioUrl, "singleCarrierFiveActivitiesWithoutRoutes.xml" ) ) ;

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader( carriers ).loadVehicleTypes( vehicleTypes ) ;

		//### Output before jsprit run
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/jsprit_unplannedCarriers.xml") ;

		//Solving the VRP (generate carrier's tour plans)
		generateCarrierPlans(network, carriers, vehicleTypes, config);

		//### Output after jsprit run
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/jsprit_plannedCarriers.xml") ;

		return carriers;
	}


	/**
	 * Creates and solves the Vehicle Routing Problem using jsprit
	 * @param network
	 * @param carriers
	 * @param vehicleTypes
	 * @param config
	 */
	private static void generateCarrierPlans(Network network, Carriers carriers, CarrierVehicleTypes vehicleTypes, Config config) {
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );

		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;

		//One independent VRP for each carrier.
		for ( Carrier carrier : carriers.getCarriers().values() ) {
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder( carrier, network ) ;
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem vrp = vrpBuilder.build() ;

			//Build algorithm out of the box
			final Jsprit.Builder algoBuilder = Jsprit.Builder.newInstance( vrp );
			algoBuilder.setProperty( Jsprit.Parameter.THREADS,"5" ) ;
			VehicleRoutingAlgorithm vra = algoBuilder.buildAlgorithm();

			//			// or read it from file
//			final URL algorithmURL = IOUtils.newUrl(scenarioUrl, "algorithm_v2.xml");
//			VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, algorithmURL);
//			//TODO initial soultion needed
			
			vra.getAlgorithmListeners().addListener(new StopWatch(), Priority.HIGH);
			vra.setMaxIterations(100);
			VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

			//Route the tour plan in MATSim
			CarrierPlan newPlan = MatsimJspritFactory.createPlan(carrier, solution) ;
			NetworkRouter.routePlan(newPlan,netBasedCosts) ;

			carrier.setSelectedPlan(newPlan) ;

			//Plot of jesprit solution
			Plotter plotter = new Plotter(vrp,solution);
			plotter.plot(config.controler().getOutputDirectory()+ "/jsprit_solution_" + carrier.getId().toString() +".png", carrier.getId().toString());

			//Print results in console
			//SolutionPrinter.print(vrp,solution,Print.VERBOSE);

		}
	}


}
