/* *********************************************************************** *
 * project: org.matsim.*
 * ValueZoneLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.geotools.feature.IllegalAttributeException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author illenberger
 *
 */
public class ZoneLayerDouble extends ZoneLayerLegacy {

	private TObjectDoubleHashMap<ZoneLegacy> values;
	/**
	 * @param zones
	 */
	public ZoneLayerDouble(Set<ZoneLegacy> zones) {
		super(zones);
		values = new TObjectDoubleHashMap<ZoneLegacy>();
	}
	
	public double getValue(Coord c) {
		ZoneLegacy zone = getZone(c);
		if(zone == null)
			return Double.NaN;
		else
			return values.get(zone);
	}
	
	public double getValue(ZoneLegacy zone) {
		return values.get(zone);
	}
	
	public void setValue(ZoneLegacy zone, double value) {
		values.put(zone, value);
	}

	public void toFile(String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("zone_id\tvalue");
		TObjectDoubleIterator<ZoneLegacy> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			writer.write(it.key().getId().toString());
			writer.write("\t");
			writer.write(Double.toString(it.value()));
			writer.newLine();
		}
		writer.close();
	}
	
	public static ZoneLayerDouble createFromFile(Set<ZoneLegacy> zones, String filename) throws IOException {
		ZoneLayerDouble layer = new ZoneLayerDouble(zones);
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			String id = tokens[0];
			double val = Double.parseDouble(tokens[1]);
			ZoneLegacy z = layer.getZone(new IdImpl(id));
			layer.setValue(z, val);
			try {
				z.getFeature().setAttribute("inhabitant", new Integer((int) val));
			} catch (IllegalAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return layer;
	}
}
