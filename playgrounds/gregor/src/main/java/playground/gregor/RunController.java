/* *********************************************************************** *
 * project: org.matsim.*
 * RunController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunController {
	
	public static void main(String [] args) {
		String config = "/Users/laemmel/devel/GRIPS/input/config.xml";
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, config);
		Scenario sc = ScenarioUtils.createScenario(c);
		ScenarioUtils.loadScenario(sc);
		Controler cntr = new Controler(sc);
//		cntr.setOverwriteFiles(true);
//		TollHandler tollHandler = new TollHandler(cntr.getScenario());
//		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
//		cntr.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
//		cntr.addControlerListener(new MarginalCostPricing( (ScenarioImpl) cntr.getScenario(), tollHandler ));
		cntr.run();
	}

}
