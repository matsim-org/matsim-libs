/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package commercialtraffic;/*
							* created by jbischoff, 03.05.2019
							*/

import commercialtraffic.commercialJob.CommercialTrafficConfigGroup;
import commercialtraffic.commercialJob.CommercialTrafficModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

import static org.matsim.core.scenario.ScenarioUtils.loadScenario;

public class RunCommercialTraffic_H_AWS {
	public static void main(String[] args) {
		String runId = args[0];//"vw281_0.1_CT_KEP.0.1";
		
		String base = args[1];
		String inputDir = "//input//";

		Config config = ConfigUtils.loadConfig(base + inputDir + "config_0.1_CT.xml", new CommercialTrafficConfigGroup());
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		config.parallelEventHandling().setNumberOfThreads(Integer.parseInt(args[2]));
		config.qsim().setNumberOfThreads(Integer.parseInt(args[3]));
		config.strategy().setFractionOfIterationsToDisableInnovation(0.75); //Fraction to disable Innovation
		
		//RECREATE ACTIVITY PARAMS 
		{
		config.planCalcScore().getActivityParams().clear();
		// activities:
		for ( long ii = 1 ; ii <= 30; ii+=1 ) {

					config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii ).setTypicalDuration( ii*3600 ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii ).setTypicalDuration( ii*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii).setTypicalDuration( ii*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );

					config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii ).setTypicalDuration( ii*3600 ) );

		}
		
		config.planCalcScore().addActivityParams( new ActivityParams( "home" ).setTypicalDuration( 14*3600 ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "work").setTypicalDuration( 8*3600 ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "leisure" ).setTypicalDuration( 1*3600 ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "shopping").setTypicalDuration( 1*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(21. * 3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "other" ).setTypicalDuration( 1*3600 ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "education").setTypicalDuration(8*3600 ).setOpeningTime(8. * 3600. ).setClosingTime(18. * 3600. ) );
		}

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks );
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.controler().setLastIteration(Integer.parseInt(args[4]));
		config.controler().setOutputDirectory(base + "//output//"+ runId);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// config.qsim().setVehiclesSource(VehiclesSource.defaultVehicle);
		// vsp defaults
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
//		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		

		config.network().setInputFile(base+inputDir + "Network//network_editedPt.xml.gz");
		config.plans().setInputFile(base+inputDir + "Population//populationWithCTdemand.xml.gz");
		config.transit().setTransitScheduleFile(base+inputDir + "Network//transitSchedule.xml.gz");
		config.transit().setVehiclesFile(base+inputDir + "Network//transitVehicles.xml.gz");
		
        CommercialTrafficConfigGroup commercialTrafficConfigGroup = ConfigUtils.addOrGetModule(config, CommercialTrafficConfigGroup.class);
        commercialTrafficConfigGroup.setFirstLegTraveltimeBufferFactor(1.5);
        
        
        FreightConfigGroup ctcg = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		ctcg.setCarriersFile(base+inputDir+"Carrier//carrier_definition.xml");
		ctcg.setCarriersVehicleTypesFile(base+inputDir+"Carrier//carrier_vehicletypes.xml");
		ctcg.setTravelTimeSliceWidth(3600);
		
		Scenario scenario = loadScenario(config);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
		adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());

		Controler controler = new Controler(scenario);
		config.controler().setRunId(runId);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
			}
		});

		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.addOverridingModule(new CommercialTrafficModule());

		controler.run();

	}

	private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
		if (flowCapacityFactor < 1.0) {
			for (Link l : network.getLinks().values()) {
				if (l.getAllowedModes().contains(TransportMode.pt)) {
					l.setCapacity(l.getCapacity() / flowCapacityFactor);
				}
			}
		}
	}
}
