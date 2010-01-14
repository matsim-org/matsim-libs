/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeMatrix.java
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

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author illenberger
 *
 */
public class TravelTimeMatrix {

	private double[][] matrix;
	
	private TObjectIntHashMap<ZoneLegacy> zoneIndices;
	
	private Set<ZoneLegacy> zones;
	
	public TravelTimeMatrix(Set<ZoneLegacy> zones) {
		this.zones = zones;
		zoneIndices = new TObjectIntHashMap<ZoneLegacy>();
		int idx = 0;
		for(ZoneLegacy zone : zones) {
			zoneIndices.put(zone, idx);
			idx++;
		}
		matrix = new double[zoneIndices.size()][zoneIndices.size()];
		for(int i = 0; i < zoneIndices.size(); i++) {
			for(int j = 0; j < zoneIndices.size(); j++) {
				matrix[i][j] = Double.POSITIVE_INFINITY;
			}
		}
		
	}
	
	public Set<ZoneLegacy> getZones() {
		return zones;
	}
	
	public double getTravelTime(ZoneLegacy origin, ZoneLegacy destination) {
		int i = getIndex(origin);
		int j = getIndex(destination);
		return matrix[i][j];
	}
	
	public void setTravelTime(ZoneLegacy origin, ZoneLegacy destination, double tt) {
		int i = getIndex(origin);
		int j = getIndex(destination);
		matrix[i][j] = tt;
	}
	
	private int getIndex(ZoneLegacy zone) {
		return zoneIndices.get(zone);
	}
	
	public static void toFile(TravelTimeMatrix matrix, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		List<ZoneLegacy> zones = new ArrayList<ZoneLegacy>(matrix.getZones());
	
		for(ZoneLegacy z_i : zones) {
			writer.write("\t");
			writer.write(z_i.getId().toString());
		}
		writer.newLine();
		for(ZoneLegacy z_i : zones) {
			writer.write(z_i.getId().toString());
			for(ZoneLegacy z_j : zones) {
				writer.write("\t");
				writer.write(Double.toString(matrix.getTravelTime(z_i, z_j)));
			}
			writer.newLine();
		}
		writer.close();
	}
	
	public static TravelTimeMatrix createFromFile(Set<ZoneLegacy> zones, String filename) throws IOException {
		TravelTimeMatrix matrix = new TravelTimeMatrix(zones);
		
		Map<Id, ZoneLegacy> mapping = new HashMap<Id, ZoneLegacy>();
		for(ZoneLegacy zone : zones)
			mapping.put(zone.getId(), zone);
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		String[] ids = line.split("\t");
		while((line = reader.readLine()) != null) {
			String[] row = line.split("\t");
			Id origId = new IdImpl(row[0]);
			ZoneLegacy z_i = mapping.get(origId);
			for(int i = 1; i < row.length; i++) {
				Id destId = new IdImpl(ids[i]);
				ZoneLegacy z_j = mapping.get(destId);
				double tt = Double.parseDouble(row[i]);
				matrix.setTravelTime(z_i, z_j, tt);
			}
		}
		
		return matrix;
	}
}
