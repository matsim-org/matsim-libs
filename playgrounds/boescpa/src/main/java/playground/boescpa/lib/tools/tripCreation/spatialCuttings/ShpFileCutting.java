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

package playground.boescpa.lib.tools.tripCreation.spatialCuttings;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

/**
 * Spatial cutting strategy for trip processing.
 * 
 * Shp File returns TRUE for all trips with start and/or end link
 * inside the shape provided with the "cuttingShpFile".
 * 
 * @author pboesch
 */
public class ShpFileCutting implements SpatialCuttingStrategy {
	
	private final String shpFile;
	
	public ShpFileCutting(String cuttingShpFile) {
		this.shpFile = cuttingShpFile;
	}

	@Override
	public boolean spatiallyConsideringTrip(Network network, Id startLink, Id endLink) {
		// TODO implement cut with an shp-File... [endLink could be null!!!]
		return false;
	}

}
