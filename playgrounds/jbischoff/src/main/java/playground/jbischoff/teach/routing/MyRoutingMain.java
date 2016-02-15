/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.teach.routing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *	Example input data can be found in the MATSim tutorial 2015, 
 *	https://isis.tu-berlin.de/course/view.php?id=5682
 *
 */
public class MyRoutingMain {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("input/routerconfig.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
			bindLeastCostPathCalculatorFactory().to(MatsimClassLeastCostPathCalculatorFactory.class);	
			}
		}
		
				
		);
		
	controler.run();
	}
	
}
