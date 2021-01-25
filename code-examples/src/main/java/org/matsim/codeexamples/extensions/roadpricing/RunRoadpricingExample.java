/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.roadpricing;


import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

public class RunRoadpricingExample {

	public static void main( String [] args ) {

		// load the config:
		Config config ;
		if ( args==null || args.length==0 || args[0]==null ) {
			config = ConfigUtils.loadConfig( "scenarios/equil-extended/config-with-roadpricing.xml" );
			config.controler().setOutputDirectory( "output" );
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setLastIteration( 1 );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		// load the scenario:
		Scenario scenario = ScenarioUtils.loadScenario( config );

		// instantiate the controler:
		Controler controler = new Controler(scenario);

		// use the road pricing module:
		controler.addOverridingModule( new RoadPricingModule() );
//		RoadPricing.configure( controler );
		// yyyyyy switch once available in matsim build

		// run the controler:
		controler.run();


		// ignore:
//		RunRoadPricingExample.main( new String []{
//				"scenarios/equil-extended/config-with-roadpricing.xml"
//				, "--config:controler.outputDirectory=" + "output"
//				, "--config:controler.overwriteFiles=" + OverwriteFileSetting.deleteDirectoryIfExists.name()
//				, "--config:controler.lastIteration=5"
//		} );

	}

}
