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
package saleem.p0;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.VariableIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class P0Controller {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("H:\\Mike Work\\input\\config.xml");
//		Config config = ConfigUtils.loadConfig("/home/saleem/P0/input/config.xml");
		config.network().setTimeVariantNetwork(true);
        Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		NetworkFactoryImpl nf = (NetworkFactoryImpl) scenario.getNetwork().getFactory();
		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
		controler.addControlerListener(new GenericP0ControlListener(scenario, (NetworkImpl) scenario.getNetwork()));
		controler.run();
	}

}
