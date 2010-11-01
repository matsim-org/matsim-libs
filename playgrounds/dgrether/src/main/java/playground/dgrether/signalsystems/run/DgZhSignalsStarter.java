/* *********************************************************************** *
 * project: org.matsim.*
 * DgZhSignalsStarter
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
package playground.dgrether.signalsystems.run;

import org.matsim.core.controler.Controler;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgZhSignalsStarter {

	/**
	 * @param args
	 */
	public static void main(String[] a) {
		String config = null;
		config = DgPaths.STUDIESDG + "lsaZurich/config.xml";
		String[] args = {config};
		
		Controler c = new Controler(config);
		c.setOverwriteFiles(true);
		c.run();
		
	}

}
