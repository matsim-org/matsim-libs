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
package tutorial.programming.trafficSignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;


/**
 * @author dgrether
 *
 */
public class RunSignalSystemsExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// ---
		
		Config config = ConfigUtils.loadConfig("examples/equil-extended/config.xml") ;
		
		config.controler().setLastIteration(0); // use higher values if you want to iterate
		
		config.network().setInputFile("examples/equil-extended/network.xml");
		
		config.plans().setInputFile("examples/equil-extended/plans100.xml");
		
		// the following makes matsim _load_ the signalSystems files, but not to do anything with them:
		// (this switch will eventually go away)
		config.scenario().setUseSignalSystems(true);
		
		// these are the paths to the signal systems definition files:
		config.signalSystems().setSignalSystemFile("examples/equil-extended/signalSystems_v2.0.xml");
		config.signalSystems().setSignalGroupsFile("examples/equil-extended/signalGroups_v2.0.xml");
		config.signalSystems().setSignalControlFile("examples/equil-extended/signalControl_v2.0.xml");
		
		
		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---

		Controler c = new Controler( scenario );

		// add the signals controller to the simulation:
		final SignalsControllerListenerFactory signalsFactory = new DefaultSignalsControllerListenerFactory() ;
        c.addControlerListener(signalsFactory.createSignalsControllerListener());

        c.setOverwriteFiles(true);
		
		c.run();
	}

}
