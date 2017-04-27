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
package playground.dgrether;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * start a simulation with fixed-time signals and visualize it with otfvis.
 * 
 * @author dgrether, tthunig
 */
public class DgController {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig( args[0]) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler c = new Controler( scenario );
		c.addOverridingModule(new OTFVisFileWriterModule());
		
		// add the signals module
//		// note: This will check (in DgSylviaSignalModelFactory) if the controllerIdentifier equals sylvia..., otherwise the default
//		// (fixed time) signal controller will be used.  kai & theresa, oct'14
//		boolean alwaysSameMobsimSeed = false;
//		SylviaSignalsModule sylviaSignalsModule = new SylviaSignalsModule();
//		sylviaSignalsModule.setAlwaysSameMobsimSeed(alwaysSameMobsimSeed);
//		c.addOverridingModule(sylviaSignalsModule);
		c.addOverridingModule(new SignalsModule());
		/* sylvia moved to playground tthunig. If you want to use sylvia use e.g. TtBasicController in playground tthunig. theresa, apr'17 */
		
		c.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		c.run();
	}

}
