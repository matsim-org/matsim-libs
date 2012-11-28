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

package playground.christoph.withinday2;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class EquilTest extends MatsimTestCase {

	public void testScenario() {
		Config config = this.loadConfig(this.getInputDirectory() + "config.xml");
		config.multiModal().setMultiModalSimulationEnabled(true);
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new MyControlerListener()) ;
		controler.run();
	}

}
