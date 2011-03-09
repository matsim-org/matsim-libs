/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerKMLWriter.java
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
package playground.johannes.socialnetworks.gis.io;

import gnu.trove.TObjectDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.io.Colorizable;

import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class ZoneLayerKMLWriter {

	private Colorizable colorizer;
	
	public void setColorizer(Colorizable colorizer) {
		this.colorizer = colorizer;
	}
	
	public void writeWithColor(ZoneLayer<Double> layer, String filename) {
		TObjectDoubleHashMap<Geometry> colors = new TObjectDoubleHashMap<Geometry>();
		
		for(Zone<Double> zone : layer.getZones()) {
			if(zone.getAttribute() != null)
				colors.put(zone.getGeometry(), zone.getAttribute());
		}
		
		colorizer = new NumericAttributeColorizer(colors);
		
		write(layer, filename);
	}
	
	public void write(ZoneLayer<?> layer, String filename) {
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		for(Zone<?> zone : layer.getZones()) {
			geometries.add(zone.getGeometry());
		}
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		writer.setColorizable(colorizer);
		writer.write(geometries, filename);
	}
}
