/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.tmp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class DemostrateCoordConversion {
	final private static Logger LOG = Logger.getLogger(DemostrateCoordConversion.class);

	public static void main(String[] args) {
		String cx = args[0];
		String cy = args[1];
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");

		Coord cOld = new Coord(Double.parseDouble(cx), Double.parseDouble(cy));
		
		Coord cNew = ct.transform(cOld);
		LOG.info("Old coord: " + cOld.toString());
		LOG.info("New coord: " + cNew.toString());
	}

}
