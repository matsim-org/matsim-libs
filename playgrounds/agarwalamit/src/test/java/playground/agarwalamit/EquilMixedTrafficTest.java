/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Just setting up the equil scenario for mixed traffic conditions.
 * 
 * @author amit
 */

public class EquilMixedTrafficTest {
	
	private static final String EQUIL_DIR = "../../matsim/examples/equil-mixedTraffic/";
	
	@Test
	public void run() {
		//see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters 
		
		Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addTravelTimeBinding("bicycle").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());
			}
		});
		
		controler.run();
	}
}
