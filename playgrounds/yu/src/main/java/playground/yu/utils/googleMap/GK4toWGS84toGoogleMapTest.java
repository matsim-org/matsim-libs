/* *********************************************************************** *
 * project: org.matsim.*
 * GK4toWGS84toGoogleMapTest.java
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
package playground.yu.utils.googleMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

/**
 * @author yu
 * 
 */
public class GK4toWGS84toGoogleMapTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GK4toWGS84 gw = new GK4toWGS84();

		Coord a = gw.transform(new CoordImpl(4594025.087395295,
				5825760.165139644));
		Coord b = gw.transform(new CoordImpl(4593995.654536505,
				5825691.89755958));
		Coord c = gw.transform(new CoordImpl(4593982.135623334,
				5825660.741790127));

		System.out.println("http://maps.google.com/maps/api/staticmap?" +
		// "center="
		// + a.getY()/* latitude */
		// + "," + a.getX()/* longitude */
		// + "&" +
		// "zoom=16" +
				"size=1024x768&markers=color:blue|label:A|" + a.getY()/* latitude */
				+ "," + a.getX()/* longitude */+ "&markers=color:blue|label:B|"
				+ b.getY()/* latitude */
				+ "," + b.getX()/* longitude */+ "&markers=color:blue|label:C|"
				+ c.getY()/* latitude */
				+ "," + c.getX()/* longitude */
				+ "&path=color:0x0000ff|weight:5|" + a.getY() + "," + a.getX()
				+ "|" + b.getY() + "," + b.getX() + "|" + c.getY() + ","
				+ c.getX() + "&sensor=false");

	}
}
