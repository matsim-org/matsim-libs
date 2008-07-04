/* *********************************************************************** *
 * project: org.matsim.*
 * Shape2Zonelayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.demandmodeling;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.world.ZoneLayer;


import com.vividsolutions.jts.geom.Coordinate;

public class ShapeFile2ZoneLayer {
	private String idAttribute = "ID";
	private String nameAttribute = "Name";
	private String areaAttribute = "Area";
	
	public void shp2ZoneLayer(final String shapeFileName, final ZoneLayer layer) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(shapeFileName);
		for (Object o: fs.getFeatures()) {
			Feature feature = (Feature) o;
			
			Coordinate c = feature.getBounds().centre();
			Object id = feature.getAttribute(this.idAttribute);
			Object name = feature.getAttribute(this.nameAttribute);
			Object area = feature.getAttribute(this.areaAttribute);
			if (id == null) {
				throw new IllegalArgumentException("There is at least one feature that does not have an ID set.");
			}
			
			layer.createZone(id.toString(), Double.toString(c.x), Double.toString(c.y), null, null, null, null, area == null ? null : area.toString(), name == null ? null : name.toString());
		}
	}
	
	public final void setIdAttributeName(final String attributeName) {
		this.idAttribute = attributeName;
	}

	public final void setNameAttributeName(final String attributeName) {
		this.nameAttribute = attributeName;
	}
	
	public final void setAreaAttributeName(final String attributeName) {
		this.areaAttribute = attributeName;
	}
}
