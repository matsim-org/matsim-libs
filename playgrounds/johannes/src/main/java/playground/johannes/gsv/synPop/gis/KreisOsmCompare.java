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

package playground.johannes.gsv.synPop.gis;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class KreisOsmCompare {

	private static final String BUILDING_COUNT = "building_count";
	
	private static MathTransform transform;
	/**
	 * @param args
	 * @throws FactoryException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws FactoryException, IOException {
		transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		
		ZoneLayer<Map<String, Object>> zoneLayer = ZoneLayerSHP.read("/home/johannes/gsv/osm/kreisCompare/zones_zone.SHP");
		
//		String[] types = new String[]{"W","A","S","B","H","F","E","D","U","R","T","V","P"};
		String[] types = new String[]{"F", "U", "R", "T", "V"};
		TObjectIntHashMap<String> attractivness = readAttractivness("/home/johannes/gsv/osm/kreisCompare/StrukturAttraktivitaet.csv", types);
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile("/home/johannes/gsv/osm/facilities/netz2030/leisure.netz2030.xml");
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		

		countFacilities(zoneLayer, facilities);
		
		TDoubleArrayList values1 = new TDoubleArrayList(zoneLayer.getZones().size());
		TDoubleArrayList values2 = new TDoubleArrayList(zoneLayer.getZones().size());
	
		double sum1 = 0;
		double sum2 = 0;
		
		int zoneNotFound = 0;
		for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			if (zone.getAttribute().get("NUTS0_CODE").equals("DE")) {
				String id = String.valueOf(zone.getAttribute().get("NO"));
				int attrac = attractivness.get(id);
				Integer cntFac = (Integer) zone.getAttribute().get(BUILDING_COUNT);
				if (cntFac == null) {
					cntFac = 0;
					zoneNotFound++;
				}

				values1.add(attrac);
				values2.add(cntFac);
				
				sum1 += attrac;
				sum2 += cntFac;
			}
		}

		Set<Zone<Map<String, Object>>> zones = new HashSet<>();
		for (Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			if (zone.getAttribute().get("NUTS0_CODE").equals("DE")) {
				String id = String.valueOf(zone.getAttribute().get("NO"));
				int attrac = attractivness.get(id);
				Integer cntFac = (Integer) zone.getAttribute().get(BUILDING_COUNT);
				if (cntFac == null) {
					cntFac = 0;
				}
				
				double val1 = attrac/sum1;
				double val2 = cntFac/sum2;
				
				
				Zone<Map<String, Object>> newZone = new Zone<>(zone.getGeometry());
				newZone.setAttribute(new HashMap<String, Object>());
				newZone.getAttribute().put("ATTRACT_ERR", (val2 - val1)/val1);
				zones.add(newZone);
			}
		}

		ZoneLayer<Map<String, Object>> newZoneLayer = new ZoneLayer<>(zones);
		newZoneLayer.overwriteCRS(CRSUtils.getCRS(4326));
		ZoneLayerSHP.writeWithAttributes(newZoneLayer, "/home/johannes/gsv/osm/kreisCompare/diff.leisure.shp");
		
		System.err.println(String.format("%s zones not found.", zoneNotFound));
		
		TDoubleDoubleHashMap stats = Correlations.mean(values1.toArray(), values2.toArray());
		StatsWriter.writeHistogram(stats, "attractivity", "facilities", "/home/johannes/gsv/osm/kreisCompare/leisure.mean.txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/osm/kreisCompare/leisure.txt"));
		writer.write("attractivity\tfacilities");
		writer.newLine();
		for(int i = 0; i < values1.size(); i++) {
			writer.write(String.valueOf(values1.get(i)));
			writer.write("\t");
			writer.write(String.valueOf(values2.get(i)));
			writer.newLine();
		}
		writer.close();
	}

	private static void countFacilities(ZoneLayer<Map<String, Object>> zoneLayer, ActivityFacilities facilities) {
		int cntNullZones = 0;
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Point p = transformCoord(facility.getCoord());
			Zone<Map<String, Object>> zone = zoneLayer.getZone(p);
			if (zone != null) {
				Integer count = (Integer) zone.getAttribute().get(BUILDING_COUNT);
				if (count == null) {
					count = new Integer(0);
					
				}
//				count++;
				count += facility.getActivityOptions().size();
				
				zone.getAttribute().put(BUILDING_COUNT, count);
			} else {
				cntNullZones++;
			}
		}

		System.err.println(String.format("%s zones not found.", cntNullZones));
	}
	
	private static Point transformCoord(Coord coord) {
		return CRSUtils.transformPoint(MatsimCoordUtils.coordToPoint(coord), transform);
	}
	
	public static TObjectIntHashMap<String> readAttractivness(String filename, String[] types) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();

		TIntArrayList indices = new TIntArrayList();
		String tokens[] = line.split("\t", -1);
		for (int i = 0; i < tokens.length; i++) {
			for (String type : types) {
				if (tokens[i].startsWith(type)) {
					indices.add(i);
				}
			}
		}
		
		TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
		
		while((line = reader.readLine()) != null) {
			tokens = line.split("\t", -1);
			
			String id = tokens[0];
			int count = 0;//Integer.parseInt(tokens[idx]);
			for(int i = 0; i < indices.size(); i++) {
				count += Integer.parseInt(tokens[indices.get(i)]);
			}
			
			map.put(id, count);
		}
		reader.close();
		
		return map;	
	}
}
