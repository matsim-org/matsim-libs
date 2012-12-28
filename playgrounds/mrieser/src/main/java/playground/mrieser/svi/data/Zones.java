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

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author mrieser / senozon
 */
public class Zones {

	private final List<SimpleFeature> zones = new ArrayList<SimpleFeature>();
	private final GeometryFactory geoFac = new GeometryFactory();
	
	public Zones() {
	}

	public void addZone(final SimpleFeature feature) {
		this.zones.add(feature);
	}
	
	public List<SimpleFeature> getAllZones() {
		return this.zones;
	}
	
	public SimpleFeature getContainingZone(final double x, final double y) {
		Point p = this.geoFac.createPoint(new Coordinate(x, y));

		for (SimpleFeature f : this.zones) {
			Object g = f.getDefaultGeometry();
			if (g instanceof Geometry) {
				if (!((Geometry) g).disjoint(p)) {
					return f;
				}
			}
		}
		return null;
	}
	
}
