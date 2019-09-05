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

/**
 *
 */
package vwExamples.robotaxiVWExample;

import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.run.RunRobotaxiExample;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

//Author: Steffen Axer
//This script runs a Robotaxi Batch Simulation for the complete research area WOB-GF-BS.
//The available fleet size is changed in each iteration to analyze the service quality for a given percentage of taxi users
//The percentage of taxi users has been pre-definied by modifying the input plans.
//Variable "pct" definies the percentage of users (WOB-WOB) that are switching to the taxi service

public class RunTaxiBatchScenario {

    public static void main(String[] args) {

//		List<Double> valueList = Arrays.asList(0.1, 0.3, 0.5);

        for (int i = 600; i < 610; i += 10) {
            //laedt eine "Basisconfig" inkl. der Nicht-Standard-Config-Gruppen für Taxis etc.
			Config config = ConfigUtils.loadConfig("D:/Axer/MatsimDataStore/WOB_Taxi_ServiceQuality/config.xml",
					new MultiModeTaxiConfigGroup(), new TaxiFareConfigGroup(), new DvrpConfigGroup(),
					new OTFVisConfigGroup());


            //ueberschreibt den Wert für die Flottendatei

            TaxiConfigGroup tcg = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME);
            tcg.setTaxisFile("D:\\Axer\\MatsimDataStore\\WOB_Taxi_ServiceQuality\\taxifleets\\fleet_" + i + ".xml");
            Double pct = 0.1;
            String runId = "run" + pct.toString() + "_" + i;
            config.controler().setRunId(runId);
            //We are now iterating 5 times!
            config.controler().setLastIteration(5);
            config.controler().setWriteEventsInterval(1);
            config.controler().setWritePlansInterval(1);
            config.controler().setOutputDirectory("D:/Axer/MatsimDataStore/WOB_Taxi_ServiceQuality/optFleet/output/" + runId);
            config.plans().setInputFile("D:/Axer/MatsimDataStore/WOB_Taxi_ServiceQuality/population/carReplaceOnly/run124.100.WOB_taxi_" + pct.toString() + ".xml.gz");

            // erstellt den Controler aus dem Robotaxiexample auf basis der aktuellen Config
            Controler controler = RunRobotaxiExample.createControler(config, false);

            controler.run();
        }
    }


}
