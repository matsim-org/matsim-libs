/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package tutorial.mobsim.simpleAdaptiveSignalEngine;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * Load the scenario from the daganzo example,
 * add the adaptive signal example from the same package
 * and run the simulation.
 * 
 * @author nagel
 */
public class RunSimpleAdaptiveSignalExample {

	private static String outputDir = "output/simpleAdaptiveSignalEngineExample/";
	
	/**
	 * @param args if not null it gives the output directory for the intergreens file
	 */
	public static void main(String[] args) {
		if (args != null){
			// use the given output if args is not null
			outputDir = args[0];
		}
		// load the daganzo example
		final Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("daganzo"), "config.xml"));
		config.controler().setOutputDirectory(outputDir);
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.vspExperimental().setWritingOutputEvents(true);
		// remove unmaterialized module from the config
		config.removeModule("otfvis");
		final Controler controler = new Controler(config);
		
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				/* bind the adaptive signal as mobsim listener (to be able to create the signals before simulation)
				 * and as events handler (to get information about agent behavior) 
				 * but both times as the same instance (asEagerSingleton) */
				bind(SimpleAdaptiveSignal.class).asEagerSingleton();
				addMobsimListenerBinding().to(SimpleAdaptiveSignal.class);
				addEventHandlerBinding().to(SimpleAdaptiveSignal.class);
			}
		});
		
		controler.run();	
	}

}
