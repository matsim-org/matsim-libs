/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.duncan.archive;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioLoaderImpl;


public class MyControler1 {

	public static void main(final String[] args) {

		String configFile;
		if ( args.length==0 ) {
			configFile = "./src/playground/duncan/myconfig1.xml";
		} else {
			configFile = args[0];
		}
		ScenarioImpl scenario = (ScenarioImpl) new ScenarioLoaderImpl(configFile).loadScenario();

		final Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
