/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis.shp2matsim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

public class Shp2NetworkModification {
	
	private static final Logger LOGGER = Logger.getLogger( Shp2NetworkModification.class );

	public static void addModeToLinksStartingOrEndingInShp(Scenario scenario, String modeToAdd, String shpFile) {
		
		LOGGER.info("Adding drt mode to network...");
		
		List<Geometry> serviceAreaGeometries = ShpGeometryUtils.loadShapeFile(shpFile);

		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				LOGGER.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if ( ShpGeometryUtils.isCoordInGeometries(link.getFromNode().getCoord(), serviceAreaGeometries)
						|| ShpGeometryUtils.isCoordInGeometries(link.getToNode().getCoord(), serviceAreaGeometries) ) {
					
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					allowedModes.add(modeToAdd);
					link.setAllowedModes(allowedModes);
				}

			} else if (link.getAllowedModes().contains(TransportMode.pt)) {
				// skip pt links
			} else {
				throw new RuntimeException("Aborting...");
			}
		}
	}
}
