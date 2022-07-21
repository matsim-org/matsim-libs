/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.extensions.matsimApplication;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import picocli.CommandLine;

@CommandLine.Command( header = ":: MyScenario ::", version = "1.0")
@MATSimApplication.Prepare( {RunMATSimAppCommandExample.class})

/**
 * @author nagel
 *
 */
public class RunMatsimApplicationExample extends MATSimApplication {

	public static void main(String[] args) {
//		MATSimApplication.run( RunMatsimApplicationExample.class, args );

//		MATSimApplication.run( RunMatsimApplicationExample.class, "help");
//		MATSimApplication.run( RunMatsimApplicationExample.class, "gui");
//		MATSimApplication.run( RunMatsimApplicationExample.class, "prepare");
		MATSimApplication.run( RunMatsimApplicationExample.class, "prepare", "example", "--input=\"abc\"");
	}

	public RunMatsimApplicationExample( ) {
		super( "scenarios/equil/config.xml");
	}

	@Override
	protected Config prepareConfig( Config config) {
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		return config;
	}

	@Override
	protected void prepareScenario( Scenario scenario ) {

	}

	@Override
	protected void prepareControler( Controler controler ) {
		controler.addOverridingModule( new OTFVisFileWriterModule() );
	}

	
}
