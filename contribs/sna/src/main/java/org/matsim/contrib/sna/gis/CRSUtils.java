/* *********************************************************************** *
 * project: org.matsim.*
 * CRSUtils.java
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
package org.matsim.contrib.sna.gis;

import java.util.HashMap;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Utility-class providing functionality related to coordinate reference systems.
 * 
 * @author illenberger
 *
 */
public class CRSUtils {

	private static final Map<Integer, CoordinateReferenceSystem> crsMappings = new HashMap<Integer, CoordinateReferenceSystem>();
	
	/**
	 * Retrieves the coordinate reference system from the EPSG database.
	 * 
	 * @param srid the spatial reference id.
	 * 
	 * @return a coordinate reference system.
	 */
	public static CoordinateReferenceSystem getCRS(int srid) {
		CoordinateReferenceSystem crs = crsMappings.get(srid);
		if(crs == null) {
			/*
			 * TODO: There seems to be an issue with the order of latitude and
			 * longitude information but i have no idea when this applies.
			 * joh01/10
			 */
			CRSAuthorityFactory factory = CRS.getAuthorityFactory(false);
			try {
				crs = factory.createCoordinateReferenceSystem("EPSG:" + srid);
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		}
		
		return crs;
	}

	/**
	 * Returns the spatial reference id for a given coordinate reference system.
	 * If the coordinate reference system has multiple identifiers one is
	 * randomly selected.
	 * 
	 * @param crs
	 *            a coordinate reference system.
	 * 
	 * @return the spatial reference id for the coordinate reference system or
	 *         <tt>0</tt> if the coordinate reference system has no identifiers.
	 */
	public static int getSRID(CoordinateReferenceSystem crs) {
		/*
		 * Randomly get one identifier.
		 */
		Identifier identifier = (Identifier)(crs.getIdentifiers().iterator().next()); 
		if(identifier == null) {
			return 0;
		} else {
			return Integer.parseInt(identifier.getCode());
		}
	}
}
