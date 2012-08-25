/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.controllers;

import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestCase;

public class TestInductiveChargingController extends MatsimTestCase {

	public void testBasic(){
		Config config= loadConfig(getClassInputDirectory()+"config.xml");
		InductiveChargingController icc=new InductiveChargingController(config);
		icc.run();
		
		//TODO: add assertions here
		
	}
	
}
