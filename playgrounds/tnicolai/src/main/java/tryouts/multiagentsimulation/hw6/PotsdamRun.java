/* *********************************************************************** *
 * project: org.matsim.*
 * PotsdamRun.java
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

/**
 * 
 */
package tryouts.multiagentsimulation.hw6;

import org.matsim.core.api.experimental.controller.Controller;

/**
 * @author thomas
 *
 */
public class PotsdamRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Controller controller = new Controller("./tnicolai/configs/brandenburg/hw6/config-potsdam_pt_after.xml");
		controller.setOverwriteFiles(true);
		controller.run();
	}

}

