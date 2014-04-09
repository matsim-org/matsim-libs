/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.misc;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.sna.gis.CRSUtils;

/**
 * @author johannes
 *
 */
public class GeoToolsTest {

	/**
	 * @param args
	 * @throws FactoryException 
	 */
	public static void main(String[] args) throws FactoryException {
		CoordinateReferenceSystem crs1 = CRSUtils.getCRS(31467);
		CoordinateReferenceSystem crs2 = CRSUtils.getCRS(4326);
		
		MathTransform transform = CRS.findMathTransform(crs1, crs2);
		
		System.out.println(transform.toString());

	}

}
