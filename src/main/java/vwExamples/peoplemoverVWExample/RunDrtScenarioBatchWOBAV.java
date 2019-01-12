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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Arrays;
import java.util.List;

//import org.matsim.contrib.drt.analysis.zonal.DrtZonalModule;
//import org.matsim.contrib.drt.optimizer.rebalancing.DemandBasedRebalancingStrategy;


/**
 * @author axer
 */

public class RunDrtScenarioBatchWOBAV {

    //Class to create the controller
    public static Controler createControler(Config config, boolean otfvis) {
        config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
        config.checkConsistency();
        return DrtControlerCreator.createControler(config, otfvis);
    }

    public static void main(String[] args) {

        //Enable or Disable rebalancing

        boolean rebalancing = true;

        //Defines a list of potential demandScenarios which will be iterated.
        //The demandScenarios need to be already prepared and stored.
        //This script generates automatically pathnames with the help of demandScenarios
//		List<Double> demandScenarios = Arrays.asList(0.05,0.1,0.15);
//		List<Double> demandScenarios = Arrays.asList(0.1);
        //This list stores our runIds that are used to save the simulations
        //runIds are used for consistent simulation identification. Our runIds are maintained in an excel file:


        List<Integer> stopTimeList = Arrays.asList(15);
//		List<Integer> setMaxTravelTimeBetaList = Arrays.asList(500);

        //for (Double demandScenario : demandScenarios){
        int instanceNumber = 1;
        for (int i = 1; i <= instanceNumber; i++) {
            //For each demandScenario we are generating a new config file
            //Some config parameters will be taken from the provided config file
            //Other config parameters will be generated or modified dynamically within this loop
            //Define the path to the config file and enable / disable otfvis
            final Config config = ConfigUtils.loadConfig("D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\WOB\\input\\config.xml", new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

            //Scenario scenario = ScenarioUtils.loadScenario(config);

            VehicleType avType = new VehicleTypeImpl(Id.create("drt", VehicleType.class));
            double avFactor = 0.1;
            avType.setFlowEfficiencyFactor(avFactor);

            //OTFVis is an open source, OpenGL-based visualizer for looking at MATSim scenarios and output.
            //With this switch we could disable/enable the visualization.
            boolean otfvis = false;

            //Overwrite existing configuration parameters
            config.plans().setInputFile("population/vw219_it_" + i + "_sampleRate1.0replaceRate_1.0_pt_drt.xml.gz");
            config.controler().setLastIteration(5); //Number of simulation iterations
            config.controler().setWriteEventsInterval(1); //Write Events file every x-Iterations
            config.controler().setWritePlansInterval(1); //Write Plan file every x-Iterations
            config.qsim().setFlowCapFactor(0.85);
            config.qsim().setStorageCapFactor(1.0);

            config.network().setInputFile("network/network_area_wob_withDRT_links.xml.gz");


            //This part allows to change dynamically DRT config parameters
            DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
            //DRT optimizer searches only the x-most closed vehicles.
            //Handling more vehicles cost more time and will induce more empty trip mileage, because faraway vehicles are also considered to service the customer request
            //drt.setkNearestVehicles(90);


            //Use custom stop duration
            drt.setMaxTravelTimeBeta(500);
            drt.setMaxTravelTimeAlpha(1.5);
            drt.setMaxWaitTime(500);
            drt.setStopDuration(15);
            drt.setTransitStopFile("../input/virtualstops/stopsGrid_400m.xml");
            drt.setMaxWalkDistance(500.0);


            String runId = "it_" + i + "_sampleRate1.0replaceRate_1.0_pt_drt_600veh_AV_" + avFactor;
            config.controler().setRunId(runId);

            config.controler().setOutputDirectory("D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\WOB\\output\\" + runId); //Define dynamically the the output path


            //For each demand scenario we are using a predefined drt vehicle fleet size
            drt.setVehiclesFile("fleets/fleet_600.xml.gz");


            //Define the MATSim Controler
            //Based on the prepared configuration this part creates a controller that runs
            Controler controler = createControler(config, otfvis);


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

            //Change the routing module in this way, that agents are forced to go to their closest bus stop.
            //If we would remove this part, agents are searching a bus stop which lies in the direction of their destination but is maybe far away.
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
//					addRoutingModuleBinding(DvrpConfigGroup.get(config).getMode())
//					        .to(ClosestStopBasedDrtRoutingModule.class);
                    // Link travel times are iterativly updated between iteration
                    // tt[i] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
                    // Remark: Small alpha leads to more smoothing and longer lags in reaction. Default alpha is 0.05. Which means i.e. 0.3 is not smooth in comparison to 0.05
                    DvrpConfigGroup.get(config).setTravelTimeEstimationAlpha(0.15);
                    DvrpConfigGroup.get(config).setTravelTimeEstimationBeta(600);
                }
            });


            //We finally run the controller to start MATSim
            controler.run();

        }
        //}
    }
}


