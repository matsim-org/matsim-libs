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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author illenberger
 *
 */
public class ZoneLayerDouble extends ZoneLayer {

	private TObjectDoubleHashMap<Zone> values;
	/**
	 * @param zones
	 */
	public ZoneLayerDouble(Set<Zone> zones) {
		super(zones);
		values = new TObjectDoubleHashMap<Zone>();
	}
	
	public double getValue(Coord c) {
		Zone zone = getZone(c);
		if(zone == null)
			return Double.NaN;
		else
			return values.get(zone);
	}
	
	public double getValue(Zone zone) {
		return values.get(zone);
	}
	
	public void setValue(Zone zone, double value) {
		values.put(zone, value);
	}

	public void toFile(String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("zone_id\tvalue");
		TObjectDoubleIterator<Zone> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			writer.write(it.key().getId().toString());
			writer.write("\t");
			writer.write(Double.toString(it.value()));
			writer.newLine();
		}
		writer.close();
	}
	
	public static ZoneLayerDouble createFromFile(Set<Zone> zones, String filename) throws IOException {
		ZoneLayerDouble layer = new ZoneLayerDouble(zones);
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			String id = tokens[0];
			double val = Double.parseDouble(tokens[1]);
			layer.setValue(layer.getZone(new IdImpl(id)), val);
		}
		return layer;
	}
}
