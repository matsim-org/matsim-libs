/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.andreas.P2;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.schedule.PTransitRouterImplFactory;


/**
 * Entry point, registers all necessary hooks
 * 
 * @author aneumann
 */
public class PControler{

	private final static Logger log = Logger.getLogger(PControler.class);

	public static void main(final String[] args) {
		
		PBox pBox = new PBox(10);
		
		PScenarioImpl scenario = new PScenarioImpl(ConfigUtils.loadConfig("F:/p/config.xml"));
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		
		PTransitRouterImplFactory pFact = new PTransitRouterImplFactory(pBox, controler);
		controler.addControlerListener(pFact);		
		controler.setTransitRouterFactory(pFact);

		controler.run();
	}		
}