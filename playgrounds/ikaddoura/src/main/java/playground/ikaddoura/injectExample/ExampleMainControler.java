/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.injectExample;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
* 
*/
public class ExampleMainControler {

	private static final String configFile = "./test/input/playground/ikaddoura/bottleneck/config.xml";
	
	private static final Logger log = Logger.getLogger(ExampleMainControler.class);
	
	public static void main(String[] args) {
		
		final Config config = ConfigUtils.loadConfig(configFile);				
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		controler.addOverridingModule( new AbstractModule() {
			
			@Override public void install() {

				// Use case I
				
				this.bind(IKWriter.class); // bind a class which provides some functionality
				
				this.bind(IKEventHandler.class).asEagerSingleton(); // bind the implemented event handler which needs to be filled during the simulation
				this.addEventHandlerBinding().to(IKEventHandler.class); // add the implemented event handler
				
//				this.bind(IKControlerListener.class); // not necessary
				this.addControlerListenerBinding().to(IKControlerListener.class); // add the implemented controler listener
				
				
				// Use case II
				
				// a combined event handler and controler listener has to be added twice and bound as singleton.
				this.bind(IKControlerListenerAndEventHandler.class).asEagerSingleton();
				this.addControlerListenerBinding().to(IKControlerListenerAndEventHandler.class);
				this.addEventHandlerBinding().to(IKControlerListenerAndEventHandler.class);
				
			}

		}) ;
				
		controler.run();
		
		log.info("Run completed.");
		
	}	
}

