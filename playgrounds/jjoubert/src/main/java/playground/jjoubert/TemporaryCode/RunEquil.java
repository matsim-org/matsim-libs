/* *********************************************************************** *
 * project: org.matsim.*
 * RunEquil.java
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

package playground.jjoubert.TemporaryCode;

import org.matsim.core.controler.Controler;

public class RunEquil {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler("/Users/johanwjoubert/Desktop/Temp/Equil/config.xml");
		c.setCreateGraphs(false);
		c.setOverwriteFiles(true);
		c.run();
	}

}
