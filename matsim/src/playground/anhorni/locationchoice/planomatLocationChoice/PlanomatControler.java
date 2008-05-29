/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControler.java
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

package playground.anhorni.locationchoice.planomatLocationChoice;

import org.matsim.controler.Controler;

public class PlanomatControler {

	public static void main(final String[] args) {
		final Controler controler = new Controler(args);
		controler.addControlerListener(new PlanomatControlerListener());
		controler.run();
		System.exit(0);
	}
}
