/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.moneyTravelDisutility;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.moneyTravelDisutility.data.BerlinAgentFilter;

/**
 * 
 * @author ikaddoura
 *
 */

public class RunExample {
	private static final Logger log = Logger.getLogger(RunExample.class);

	private static String configFile = "/Users/ihab/Desktop/test/config.xml";
	private static double sigma = 0.;

	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			throw new RuntimeException("Not yet implemented. Aborting...");
		}
				
		RunExample runner = new RunExample();
		runner.run();
	}

	public void run() {
						
		log.info("Loading scenario...");
		
		Config config = ConfigUtils.loadConfig(configFile);		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		log.info("Loading scenario... Done.");

		// money travel disutility
		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
		
		factory.setSigma(sigma);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				
				this.bind(AgentFilter.class).to(BerlinAgentFilter.class);
								
				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
				this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
			}
		}); 
			
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		log.info("Simulation run completed.");
	}
	
}
