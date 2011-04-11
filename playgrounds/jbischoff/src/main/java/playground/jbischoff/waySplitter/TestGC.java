/* *********************************************************************** *
 * project: org.matsim.*
 * TestGC.java
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

package playground.jbischoff.waySplitter;

import org.matsim.api.core.v01.Coord;

/**
 * @author jbischoff
 *
 */

public class TestGC {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		JBGoogleGeocode jgg = new JBGoogleGeocode();
		Coord xy = jgg.readGC("asdasas");
		Coord xZy = jgg.readGC("BERLIN");
		System.out.println(xy);
		System.out.println(xZy);	
	}

}

