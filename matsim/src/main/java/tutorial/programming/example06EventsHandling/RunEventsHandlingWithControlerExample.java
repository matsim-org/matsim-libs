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
package tutorial.programming.example06EventsHandling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunEventsHandlingWithControlerExample {

	public static final String outputDirectory = "output/example7" ;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config ;
		if ( args==null || args.length==0 ) {
			config = ConfigUtils.loadConfig( "examples/tutorial/programming/example7-config.xml" ) ;
		} else {
			config = ConfigUtils.loadConfig( args[0] ) ;
		}
		
		config.controler().setOutputDirectory(outputDirectory);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		//Create an instance of the controler
		Controler controler = new Controler(config);
		
		// add the events handlers
		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.addEventHandlerBinding().toInstance( new MyEventHandler1() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler2( ) );
				this.addEventHandlerBinding().toInstance( new MyEventHandler3() );
				this.addEventHandlerBinding().toInstance( new CongestionDetectionEventHandler( scenario.getNetwork() )  );
			}
		});
		
		//call run() to start the simulation
		controler.run();
	}

}
