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
package playground.kai.usecases.cannibalization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class KNCannibalization {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		
		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.class ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule(new PModule() ) ;
		
		// punish minibus if it takes traffic away from S-Bahn
		// (1) run iterations with existing pt system and memorize resulting plans as initial plans
		// (2) person taking minibus will pay the usual BVG fare (how is BVG fare encoded?)
		// (3) minibus will receive penalty for each passenger that cometh from S-Bahn
		//    * but how exactly is "coming from S-Bahn" defined?
		
		controler.run();
	}

}
