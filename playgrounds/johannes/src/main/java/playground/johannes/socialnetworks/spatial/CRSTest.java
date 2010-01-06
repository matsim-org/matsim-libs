/* *********************************************************************** *
 * project: org.matsim.*
 * CRSTest.java
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
package playground.johannes.socialnetworks.spatial;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author illenberger
 *
 */
public class CRSTest {

	/**
	 * @param args
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 */
	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {
		int srid = 3452;
		CRSAuthorityFactory   factory = CRS.getAuthorityFactory(false);
		CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("EPSG:4326");
		System.out.println(crs.getName());

	}

}
