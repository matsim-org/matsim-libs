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
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class DgController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig( args[0]) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler c = new Controler( scenario );
		c.addOverridingModule(new OTFVisModule());
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();

		final DgSylviaControlerListenerFactory signalsFactory = new DgSylviaControlerListenerFactory(sylviaConfig);
		// note: This will check (in DefaultSignalModelFactory) if the controllerIdentifier equals sylvia..., otherwise the default
		// (fixed time) signal controller will be used.  kai & theresa, oct'14
		
		signalsFactory.setAlwaysSameMobsimSeed(false);

        c.addControlerListener(signalsFactory.createSignalsControllerListener());

		c.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		c.run();
	}

}
