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

package org.matsim.codeexamples.freight.runFreightExample;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.management.InvalidAttributeValueException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.analysis.toolbox.StopWatch;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;

import static org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles;


public class RunFreightExample {

	private static URL scenarioUrl ;
	static{
		scenarioUrl = ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ) ;
	}

	public static void main(String[] args) throws InvalidAttributeValueException {

		// ### config stuff: ###
		Config config = createConfig();

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "dump");

		// ### scenario stuff: ###
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Building the Carriers with jsprit, incl jspritOutput KT 03.12.2014
		Carriers carriers = jspritRun(config, scenario.getNetwork());

		//final MATSim configurations and start of the MATSim-Run:
		matsimRun(scenario, carriers);
	}

	private static Config createConfig() {
		final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
		final URL configURL = IOUtils.newUrl(url, "config.xml");
		Config config = ConfigUtils.loadConfig(configURL  );
		config.controler().setOverwriteFileSetting( overwriteExistingFiles );
		config.global().setRandomSeed(4177);
		config.controler().setOutputDirectory("./output/");

		//Vehicles should not start at midnight. They should start after first "start" activity.
		config.plans().setActivityDurationInterpretation( tryEndTimeThenDuration );

		return config;
	}

	private static Carriers jspritRun(Config config, Network network) throws InvalidAttributeValueException {
		CarrierVehicleTypes vehicleTypes = createVehicleTypes();

		Carriers carriers = createCarriers(vehicleTypes);

		generateCarrierPlans(network, carriers, vehicleTypes, config); // Hier erfolgt Lösung des VRPs

		//### Output nach Jsprit Iteration
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/jsprit_plannedCarriers.xml") ;
		return carriers;
	}

	private static Carriers createCarriers(CarrierVehicleTypes vehicleTypes) {
		Carriers carriers = new Carriers() ;
		final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
		final URL carrierURL = IOUtils.newUrl(url, "singleCarrier.xml");
		new CarrierPlanXmlReaderV2(carriers).readURL(carrierURL) ;

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;
		return carriers;
	}

	private static CarrierVehicleTypes createVehicleTypes() {
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
		final URL vehicleTypesURL = IOUtils.newUrl(url, "vehicleTypes.xml");
		new CarrierVehicleTypeReader(vehicleTypes).readURL(vehicleTypesURL) ;
		return vehicleTypes;
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

			//				//Build algorithm out of the box
			//				VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp).setProperty(Jsprit.Parameter.THREADS, "5").buildAlgorithm();
			// or read it from file
			final URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");
			final URL algortihmURL = IOUtils.newUrl(url, "algorithm_v2.xml");
			VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, algortihmURL);

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


	//Ausgangspunkt für die MATSim-Simulation
	private static void matsimRun(Scenario scenario, Carriers carriers) {
		final Controler controler = new Controler( scenario ) ;

		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2(scenario);
		CarrierPlanStrategyManagerFactory planStrategyManagerFactory =  createMyStrategymanager(); //Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015

		CarrierModule listener = new CarrierModule(carriers, planStrategyManagerFactory, scoringFunctionFactory) ;
		listener.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(listener) ;
		controler.run();
	}


	//Benötigt, da listener kein "Null" als StrategyFactory mehr erlaubt, KT 17.04.2015
	//Da keine Strategy notwendig, hier zunächst eine "leere" Factory
	private static CarrierPlanStrategyManagerFactory createMyStrategymanager(){
		return new CarrierPlanStrategyManagerFactory() {
			@Override
			public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
				return null;
			}
		};
	}



	private static void writeAdditionalRunOutput(Config config, Carriers carriers) {
		// ### some final output: ###
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml") ;
		new CarrierPlanXmlWriterV2(carriers).write( config.controler().getOutputDirectory() + "/output_carriers.xml.gz") ;
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controler().getOutputDirectory() + "/output_vehicleTypes.xml");
		new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(config.controler().getOutputDirectory() + "/output_vehicleTypes.xml.gz");
	}


}
}
