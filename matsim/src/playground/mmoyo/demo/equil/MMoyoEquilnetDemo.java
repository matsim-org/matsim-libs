/* *********************************************************************** *
 * project: org.matsim.*
 * EquilnetDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.demo.equil;

import java.util.EnumSet;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.scenario.ScenarioLoader;
import playground.mmoyo.TransitSimulation.MMoyoTransitControler;
import org.matsim.run.OTFVis;

/**copy of marcel.pt.demo.equilNet.EquilnetDemo.java to test the ptRouter in the simulation*/
public class MMoyoEquilnetDemo {

	private final ScenarioImpl scenario = new ScenarioImpl();

	private void prepareConfig() {
		Config config = this.scenario.getConfig();
		config.network().setInputFile("examples/equil/network.xml");
		config.plans().setInputFile("examples/equil/plans100.xml");
		
		config.controler().setOutputDirectory("./output/transitEquil2");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controler().addParam("routingAlgorithmType", "AStarLandmarks");

		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityPriority_0", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0", "12:00:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_0", "18:00:00");

		config.charyparNagelScoring().addParam("activityType_1", "w");
		config.charyparNagelScoring().addParam("activityPriority_1", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_1", "08:00:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_1", "06:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_1", "07:00:00");

		config.simulation().setEndTime(30.0*3600);

		config.strategy().addParam("maxAgentPlanMemorySize", "5");

		config.strategy().addParam("ModuleProbability_1", "0.1");
		config.strategy().addParam("Module_1", "TimeAllocationMutator");
		config.strategy().addParam("ModuleProbability_2", "0.1");
		config.strategy().addParam("Module_2", "ReRoute");
		config.strategy().addParam("ModuleProbability_3", "0.1");
		config.strategy().addParam("Module_3", "ChangeLegMode");
		config.strategy().addParam("ModuleProbability_4", "0.1");
		config.strategy().addParam("Module_4", "SelectExpBeta");

		Module changeLegModeModule = config.createModule("changeLegMode");
		changeLegModeModule.addParam("modes", "car,pt");

		Module transitModule = config.createModule("transit");
		transitModule.addParam("transitScheduleFile", "src/playground/marcel/pt/demo/equilnet/transitSchedule.xml");
		transitModule.addParam("vehiclesFile", "src/playground/marcel/pt/demo/equilnet/vehicles.xml");
		transitModule.addParam("transitModes", "pt");
	}

	private void runControler() {
		new ScenarioLoader(this.scenario).loadScenario();
		MMoyoTransitControler c = new MMoyoTransitControler(this.scenario);
		
		////////////////////////////////////////////////////
		c.setOverwriteFiles(true);   //temporarily overwrite
		////////////////////////////////////////////////////

		c.run();
	}

	public void run() {
		prepareConfig();
		runControler();
	}

	public static void main(final String[] args) {
		double startTime = System.currentTimeMillis();
		new MMoyoEquilnetDemo().run();
		System.out.println("duration:" + (System.currentTimeMillis()-startTime));
	
		/*
		String net = "examples/equil/network.xml";
		String plans = "output/transitEquil2/output_plans.xml.gz";
		OTFVis.main(new String[]{net, plans});
		*/
	}
   
	
}
