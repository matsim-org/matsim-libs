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
import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectIntHashMap;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.socialnetworks.gis.io.ZoneLayerSHP;
import org.matsim.contrib.socnetgen.socialnetworks.statistics.Correlations;
import org.matsim.contrib.socnetgen.util.MatsimCoordUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class Plz8OsmCompare {

	private static final String BUILDING_COUNT = "building_count";
	
	private static MathTransform transform;
	/**
	 * @param args
	 * @throws FactoryException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws FactoryException, IOException {
		transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		
		ZoneLayer<Map<String, Object>> zoneLayer = ZoneLayerSHP.read("/home/johannes/gsv/Marktzellen_2011/Grenzen+Sachdaten/PLZ8_10w_XXL_region.shp");
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile("/home/johannes/gsv/osm/facilities.shop.xml");
		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		TObjectIntHashMap<String> employees = readEmployees("/home/johannes/gsv/Marktzellen_2011/Mitarbeiter_Einkauf.csv");

		countFacilities(zoneLayer, facilities);
		
		TDoubleArrayList values1 = new TDoubleArrayList(zoneLayer.getZones().size());
		TDoubleArrayList values2 = new TDoubleArrayList(zoneLayer.getZones().size());
		
		int zoneNotFound = 0;
		for(Zone<Map<String, Object>> zone : zoneLayer.getZones()) {
			String id = (String) zone.getAttribute().get("PLZ8");
			int cntEmpl = employees.get(id);
			Integer cntFac = (Integer) zone.getAttribute().get(BUILDING_COUNT);
			if(cntFac == null) {
				cntFac = 0;
				zoneNotFound++;
			}
			
			values1.add(cntEmpl);
			values2.add(cntFac);
		}
		
		System.err.println(String.format("%s zones not found.", zoneNotFound));
		
		TDoubleDoubleHashMap stats = Correlations.mean(values1.toNativeArray(), values2.toNativeArray());
		StatsWriter.writeHistogram(stats, "employees", "facilities", "/home/johannes/gsv/osm/shop-compare.txt");
//		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/osm/shop-compare.txt"));
//		writer.write("employees\tfacilities");
//		writer.newLine();
//		for(int i = 0; i < values1.size(); i++) {
//			writer.write(String.valueOf(values1.get(i)));
//			writer.write("\t");
//			writer.write(String.valueOf(values2.get(i)));
//			writer.newLine();
//		}
//		writer.close();
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
				count++;
				
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
	
	private static TObjectIntHashMap<String> readEmployees(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		
		TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";", -1);
			
			String id = tokens[0];
			int count = Integer.parseInt(tokens[4]);
			
			map.put(id, count);
		}
		reader.close();
		
		return map;	
	}
}
