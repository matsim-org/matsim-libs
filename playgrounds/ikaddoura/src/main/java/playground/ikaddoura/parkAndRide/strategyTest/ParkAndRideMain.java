/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.strategyTest;

import org.matsim.core.controler.Controler;

/**
 * @author Ihab
 *
 */
public class ParkAndRideMain {
	
	public static void main(String[] args) {
			String config = "../../shared-svn/studies/ihab/parkAndRide/input/test_config.xml";
			Controler controler = new Controler(config);
			controler.setOverwriteFiles(true);
			controler.addControlerListener(new ParkAndRideControlerListener(controler));
			controler.run();
	}
}
	
