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
package playground.kai.run;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.signals.data.SignalsData;


/**
 * Configures and runs MATSim with traffic signals. 
 * 
 * @author dgrether
 *
 */
public class KNSignalSystemsExample {

	/**
	 * @param args is ignored
	 */
	public static void main(String[] args) {
		
		String inputDir = "./../../matsim/examples/equil-extended/";
		
		File f = new File("t");
		System.out.println(f.getAbsolutePath());
		Config config = ConfigUtils.loadConfig(inputDir + "config.xml") ;
		
		config.controler().setLastIteration(0); // use higher values if you want to iterate
		
		config.network().setInputFile(inputDir + "network.xml");
		
		config.plans().setInputFile(inputDir + "plans100.xml");
		
		// the following makes the contrib load  the signalSystems files, but not to do anything with them:
		// (this switch will eventually go away)
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);

		// these are the paths to the signal systems definition files:
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(inputDir + "signalSystems_v2.0.xml");
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(inputDir + "signalGroups_v2.0.xml");
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(inputDir + "signalControl_v2.0.xml");
		
//		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
//		config.controler().setLinkToLinkRoutingEnabled(true);

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		// ---

		// add the signals module to the simulation:
		Controler c = new Controler( scenario );
		c.addOverridingModule(new SignalsModule());
		
		//do it, do it, do it, now
		c.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		c.run();
	}

}
