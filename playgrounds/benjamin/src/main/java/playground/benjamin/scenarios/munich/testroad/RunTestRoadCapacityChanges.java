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
package playground.benjamin.scenarios.munich.testroad;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;


/**
 * @author benjamin
 *
 */
public class RunTestRoadCapacityChanges {

	static String inputPath = "../../detailedEval/teststrecke/sim/input/";
	static String configName = "_config_capacityChanges.xml";
	// String configName = "_config.xml";

	static String enterLinkId = "592536888";
	static String leaveLinkId = "590000822";
	static int startCapacity = 1200;
	static int stepSize = 50;
	
	
	
	static int [] days = {
			20090707,
			20090708,
			20090709
	};
	
	public static void main(String[] args) {


		for(int day : days){
			String config = inputPath + day + configName;
			Controler controler = new Controler(config);
			controler.setOverwriteFiles(true);
			Scenario scenario = controler.getScenario();
			controler.addControlerListener(new UpdateCapacityControlerListener(scenario, enterLinkId, leaveLinkId, startCapacity, stepSize));
			controler.run();
		}
	}
}