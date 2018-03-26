
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

package vwExamples.initalruns.berlin;

import java.util.Arrays;
import java.util.List;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
//import org.matsim.contrib.drt.optimizer.rebalancing.DemandBasedRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import peoplemover.ClosestStopBasedDrtRoutingModule;
import vwExamples.peoplemoverVWExample.CustomRebalancing.DemandBasedRebalancingStrategyMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.RelocationWriter;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalDemandAggregatorMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalIdleVehicleCollectorMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalRelocationAggregatorMy;

/** * @author axer */

public class RunCleanBerlin {
	

	public static void main(String[] args) {
		

	final Config config = ConfigUtils.loadConfig("D:\\Axer\\MatsimDataStore\\Berlin_DRT\\input\\config.xml");
						
	//Overwrite existing configuration parameters
	config.plans().setInputFile("population/be_251.output_plans_selected_1.0upsample.xml.gz");
	config.network().setInputFile("network/modifiedNetwork.xml.gz");
	String runId = "CleanBerlin";
	config.controler().setRunId(runId);	
	config.controler().setOutputDirectory("D:\\Axer\\MatsimDataStore\\CleanBerlin\\output\\"+runId); //Define dynamically the the output path
	Controler controler = new Controler(config);
	controler.run();
			

	}
}

