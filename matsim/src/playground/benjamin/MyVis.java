/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.run.OTFVis;

/**
 *
 */
public class MyVis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		String otffile = "../matsim/test/input/playground/benjamin/BKickRouterTestIATBR/network.xml";
		String otffile = BkPaths.RUNBASE + "run749/it.2000/749.2000.Zurich.otfvis.mvi";

		OTFVis.main(new String[] {otffile});
	}

}
