
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.peoplemoverVWExample;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.temperature.TemperatureChangeConfigGroup;

import vwExamples.utils.CreateEDRTVehiclesAndChargers;

/**
 * @author axer
 */

public class RunDrtScenarioBatchBS_eDRT {

    // Class to create the controller
	//	public static Controler createControlerWithSingleModeDrt(Config config, boolean otfvis) {
//		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
//		config.checkConsistency();
	//		return DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);
//	}

    public static void main(String[] args) {

        // Enable or Disable rebalancing
        String runId = "1.0_eDRT_umf_run5";
        boolean rebalancing = true;

        String inbase = "D:\\Matsim\\Axer\\BSWOB2.0_Scenarios\\";

        final Config config = ConfigUtils.loadConfig(inbase + "conf_BS_DRT_100pct_eDRT.xml", new DrtConfigGroup(),
                new DvrpConfigGroup(), new OTFVisConfigGroup(), new EvConfigGroup(), new TemperatureChangeConfigGroup());

        TemperatureChangeConfigGroup tcg = (TemperatureChangeConfigGroup) config.getModules().get(TemperatureChangeConfigGroup.GROUP_NAME);
        tcg.setTempFile(inbase + "\\temp\\temperatures.csv");

        //config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
        // Overwrite existing configuration parameters
        config.plans().setInputFile(inbase + "\\plans\\vw219_100pct_eDRT.xml.gz");
        config.controler().setLastIteration(2); // Number of simulation iterations
        config.controler().setWriteEventsInterval(2); // Write Events file every x-Iterations
        config.controler().setWritePlansInterval(2); // Write Plan file every x-Iterations
        config.qsim().setStartTime(0);

        String networkFilePath = inbase + "network\\vw219_SpeedCal.xml";
        String shapeFilePath = inbase + "\\shp\\parking-bs.shp";
        String shapeFeature = "NO"; //shapeFeature is used to read the shapeFilePath. All zones in shapeFile are used to generate a drt service area
        String drtTag = "drt"; //drtTag is assigned to roads that should be used by the drt service
        //Adding drtTag to the network in order to define a service area
        vwExamples.utils.serviceAreaShapeToNetwork.run(networkFilePath, shapeFilePath, shapeFeature, drtTag);
        config.network().setInputFile(inbase + "\\network\\drtServiceAreaNetwork.xml.gz");

        // This part allows to change dynamically DRT config parameters
        DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);

        drt.setPrintDetailedWarnings(true);
        //Parameters to setup the DRT service
        drt.setMaxTravelTimeBeta(500.0);
        drt.setMaxTravelTimeAlpha(1.3);
        drt.setMaxWaitTime(500.0);
        drt.setStopDuration(15.0);


        //Create the virtual stops for the drt service
        //VirtualStops are dynamically generated
        vwExamples.utils.CreateStopsFromGrid.run(config.network().getInputFile(), 400.0, drtTag);
        drt.setTransitStopFile(inbase + "\\network\\virtualStops.xml");
        drt.setMaxWalkDistance(800.0);

        config.controler().setRunId(runId);
        //10 percent Scenario
//			config.qsim().setFlowCapFactor(0.12);
//			config.qsim().setStorageCapFactor(0.24);


        config.qsim().setFlowCapFactor(0.85);
        config.qsim().setStorageCapFactor(1.00);

        config.controler().setOutputDirectory(inbase + "\\output\\" + runId); // Define dynamically the the
        // output path

        // For each demand scenario we are using a predefined drt vehicle fleet size
        //int fleetSize = 50;
        //CreatePeopleMoverVehicles.run(inbase+"/network/drtServiceAreaNetwork.xml.gz", fleetSize, drtTag);

        //Define infrastructure for eDRT (vehicles, depots and chargers)
        CreateEDRTVehiclesAndChargers vehiclesAndChargers = new CreateEDRTVehiclesAndChargers();
        Map<Id<Link>, Integer> depotsAndVehicles = new HashMap<>();
        depotsAndVehicles.put(Id.createLinkId(40158), 90); //BS HBF
        depotsAndVehicles.put(Id.createLinkId(8097), 90); //Zentrum SO
        depotsAndVehicles.put(Id.createLinkId(13417), 90); //Zentrum N
        depotsAndVehicles.put(Id.createLinkId(14915), 90); //Flugplatz

        vehiclesAndChargers.CHARGER_FILE = inbase + "\\chargers\\chargers.xml.gz";
        vehiclesAndChargers.NETWORKFILE = inbase + "\\network\\drtServiceAreaNetwork.xml.gz";
        vehiclesAndChargers.DRT_VEHICLE_FILE = inbase + "\\fleets\\fleet.xml.gz";
        vehiclesAndChargers.E_VEHICLE_FILE = inbase + "\\fleets\\eFleet.xml.gz";
        vehiclesAndChargers.drtTag = drtTag;
        vehiclesAndChargers.SEATS = 6;
        vehiclesAndChargers.MAX_START_CAPACITY_KWH = 60;
        vehiclesAndChargers.MIN_START_CAPACITY_KWH = 60;
        vehiclesAndChargers.BATTERY_CAPACITY_KWH = 60;
        vehiclesAndChargers.run(depotsAndVehicles);

        drt.setVehiclesFile(inbase + "\\fleets\\fleet.xml.gz");
        drt.setIdleVehiclesReturnToDepots(true);
        drt.setOperationalScheme("stopbased");


        EvConfigGroup eDrt = (EvConfigGroup) config.getModules().get(EvConfigGroup.GROUP_NAME);
        eDrt.setChargersFile(inbase + "\\chargers\\chargers.xml.gz");
        eDrt.setVehiclesFile(inbase + "\\fleets\\eFleet.xml.gz");
        eDrt.setAuxDischargeTimeStep(10);
        eDrt.setAuxDischargingSimulation(EvConfigGroup.AuxDischargingSimulation.seperateAuxDischargingHandler);
        eDrt.setTimeProfiles(true);


        // Define the MATSim Controler
        // Based on the prepared configuration this part creates a controller that runs
		//Controler controler = createControlerWithSingleModeDrt(config, otfvis);

        Controler controler = electric.edrt.run.RunEDrtScenario.createControler(config);

        if (rebalancing == true) {

            // Every x-seconds the simulation calls a re-balancing process.
            // Re-balancing has the task to move vehicles into cells or zones that fits
            // typically with the demand situation
            // The technically used re-balancing strategy is then installed/binded within
            // the initialized controler
            System.out.println("Rebalancing Online");

            MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();

            rebalancingParams.setInterval(600);
            rebalancingParams.setCellSize(2000);
            rebalancingParams.setTargetAlpha(0.5);
            rebalancingParams.setTargetBeta(0.5);
            rebalancingParams.setMaxTimeBeforeIdle(300);
            rebalancingParams.setMinServiceTime(3600);
            drt.addParameterSet(rebalancingParams);

        }


        // Change the routing module in this way, that agents are forced to go to their
        // closest bus stop.
        // If we would remove this part, agents are searching a bus stop which lies in
        // the direction of their destination but is maybe far away.
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // addRoutingModuleBinding(DvrpConfigGroup.get(config).getMode())
                //.to(ClosestStopBasedDrtRoutingModule.class);
                // Link travel times are iterativly updated between iteration
                // tt[i] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
                // Remark: Small alpha leads to more smoothing and longer lags in reaction.
                // Default alpha is 0.05. Which means i.e. 0.3 is not smooth in comparison to
                // 0.05
                DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.15);
                DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(600);
                //bind(RelocationWriter.class).asEagerSingleton();
                //addControlerListenerBinding().to(RelocationWriter.class);

            }
        });

        // We finally run the controller to start MATSim
        controler.run();


        // }
    }
}
