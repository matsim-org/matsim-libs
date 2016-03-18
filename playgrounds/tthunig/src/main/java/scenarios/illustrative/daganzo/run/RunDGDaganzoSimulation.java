/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package scenarios.illustrative.daganzo.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

/**
 * Run a simulation of the daganzo scenario of DG
 * 
 * @author tthunig
 *
 */
public class RunDGDaganzoSimulation {

	private static final String BASE_DIR = "../../matsim/examples/daganzo/";
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(BASE_DIR + "config.xml");
		config.controler().setOutputDirectory("../../../runs-svn/daganzo/DGScenario/");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		
//		config.plans().setInputFile(BASE_DIR + "plans_long_route_selected.xml.gz");
		config.plans().setInputFile(BASE_DIR + "plans_short_route_selected.xml.gz");
		
		config.network().setInputFile(BASE_DIR + "network21.xml");
//		config.network().setInputFile(BASE_DIR + "network22.xml");
		
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.vspExperimental().setWritingOutputEvents(true);
		
		// remove unmaterialized module
		config.removeModule("otfvis");
		
		config.controler().setLastIteration(100);
		
		final Controler controler = new Controler(config);
		
		controler.run();
	}

}
