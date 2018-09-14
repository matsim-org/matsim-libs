/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.fixedTimeSignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;


/**
 * Configures and runs MATSim with traffic signals from input files
 * and visualizes it with OTFVis. 
 * 
 * @author dgrether, tthunig
 */
public class RunSignalSystemsExampleWithHoles {
	
	public static void main(String[] args) {
		run(true);
	}

	static void run(boolean useOTFVis) {
		// load a config (without signal information)
		Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil-extended"), "config.xml"));
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists);
		
		// use higher values if you want to iterate
		config.controler().setLastIteration(0); 
		
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans2000.xml.gz");
		
		// simulate traffic dynamics with holes (default would be without)
		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		config.qsim().setNodeOffset(5.);
        config.qsim().setUsingFastCapacityUpdate(false);
		
		// add the signal config group to the config file
		SignalSystemsConfigGroup signalConfig = 
				ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class ) ;
		
		// the following makes the contrib load the signal input files, but not to do anything with them
		// (this switch will eventually go away)
		signalConfig.setUseSignalSystems(true);

		// set the paths to the signal systems definition files
		signalConfig.setSignalSystemFile("signalSystems_v2.0.xml");
		signalConfig.setSignalGroupsFile("signalGroups_v2.0.xml");
		signalConfig.setSignalControlFile("signalControl_v2.0.xml");
		
//		// here is how to also use intergreen and amber times:
//		signalConfig.setUseIntergreenTimes(true);
//		signalConfig.setIntergreenTimesFile(intergreenTimesFile);
//		// set a suitable action for the case that intergreens are violated
//		signalConfig.setActionOnIntergreenViolation(SignalSystemsConfigGroup.ActionOnIntergreenViolation.WARN);
//		signalConfig.setUseAmbertimes(true);
//		signalConfig.setAmberTimesFile(amberTimesFile);
		
//		// here is how to switch on link to link travel times if lanes are used:
//		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
//		config.controler().setLinkToLinkRoutingEnabled(true);
		
		if (useOTFVis) {
			// add the OTFVis config group
			OTFVisConfigGroup otfvisConfig = 
					ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			// make links visible beyond screen edge
			otfvisConfig.setScaleQuadTreeRect(true); 
			otfvisConfig.setColoringScheme(ColoringScheme.byId);
			otfvisConfig.setAgentSize(240);
		}

		// --- create the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		/* load the information about signals data (i.e. fill the SignalsData object)
		 * and add it to the scenario as scenario element */
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		// --- create the controler
		Controler c = new Controler( scenario );

		/* add the signals module to the simulation
		 * such that SignalsData is not only contained in the scenario 
		 * but also used in the simulation */
		c.addOverridingModule(new SignalsModule());
		
		/* add the visualization module to the simulation
		 * such that it is used */
		if ( useOTFVis ) {
			c.addOverridingModule( new OTFVisWithSignalsLiveModule() );
		}
		
		// run the simulation
		c.run();
	}
}
