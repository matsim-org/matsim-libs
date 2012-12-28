/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.data;

import java.util.Collection;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author mrieser / senozon
 */
public class ShapeZonesReader {

	private final Zones zones;
	public ShapeZonesReader(final Zones zones) {
		this.zones = zones;
	}
	
	public void readShapefile(final String filename) {
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(filename);
		for (SimpleFeature f : features) {
			this.zones.addZone(f);
		}
	}
}
