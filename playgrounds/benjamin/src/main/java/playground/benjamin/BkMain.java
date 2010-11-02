/* *********************************************************************** *
 * project: org.matsim.*
 * BkMain.java
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
package playground.benjamin;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;

/**
 * @author benjamin
 *
 */
public class BkMain {

	public static void main(String[] args) {

		String [] configDay1 = {"../../detailedEval/teststrecke/sim/input/20090707_config.xml"};
		String [] configDay2 = {"../../detailedEval/teststrecke/sim/input/20090708_config.xml"};
		String [] configDay3 = {"../../detailedEval/teststrecke/sim/input/20090709_config.xml"};

		Controler controlerDay1 = new Controler(configDay1);
		controlerDay1.setOverwriteFiles(true);
		Scenario scenarioDay1 = controlerDay1.getScenario();
		controlerDay1.addControlerListener(new BkControlerListener(scenarioDay1));
		controlerDay1.run();
		
		Controler controlerDay2 = new Controler(configDay2);
		controlerDay2.setOverwriteFiles(true);
		Scenario scenarioDay2 = controlerDay2.getScenario();
		controlerDay2.addControlerListener(new BkControlerListener(scenarioDay2));
		controlerDay2.run();
		
		Controler controlerDay3 = new Controler(configDay3);
		controlerDay3.setOverwriteFiles(true);
		Scenario scenarioDay3 = controlerDay3.getScenario();
		controlerDay3.addControlerListener(new BkControlerListener(scenarioDay3));
		controlerDay3.run();
	}
}
