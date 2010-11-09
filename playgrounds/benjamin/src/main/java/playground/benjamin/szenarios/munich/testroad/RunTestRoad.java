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
package playground.benjamin.szenarios.munich.testroad;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;


/**
 * @author benjamin
 *
 */
public class RunTestRoad {

	public static void main(String[] args) {

		String inputpath = "../../detailedEval/teststrecke/sim/input/";
//		String configName = "_config.xml";
		String configName = "_config_capacityChanges.xml";
		int [] days = {20090707, 20090708, 20090709};
		
		for(int day : days){
			String config = inputpath + day + configName;
			Controler controler = new Controler(config);
			controler.setOverwriteFiles(true);
			Scenario scenario = controler.getScenario();
			controler.addControlerListener(new UpdateCapacityControlerListener(scenario));
			controler.run();
		}
	}
}