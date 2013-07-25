/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.utils.misc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.io.Paths;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author thomas
 *
 */
public class RandomLocationDistributor {
	
	private static final Logger log = Logger.getLogger(RandomLocationDistributor.class);
	public static final String ZONE_ID = "ZONE_ID";
	
	private Random random 		= null;
	private String shapefile 	= null;
	private double radius 		= 0.;
	private boolean isShapefile = false;
	private Map<Id, SimpleFeature> featureMap = null;
	
	/**
	 * constructor
	 * @param shapefile
	 * @param radius
	 */
	public RandomLocationDistributor(String shapefile, double radius) {
		this.random = new Random();
		this.shapefile = shapefile;
		this.radius = radius;

		if (this.shapefile != null && Paths.pathExsits(this.shapefile)) {
			// try to create a map of zone features from the shape file
			this.featureMap = initShapeFeatures(this.shapefile);
			// set true if successful
			if (this.featureMap != null)
				this.isShapefile = true;
		} else
			log.info("No shape-file given. Using radius (" + radius
					+ " meter) and the zone centroid to distribute locations.");
	}

	/**
	 * inits zone features
	 * 
	 * @param shapefile
	 * @return
	 */
	private Map<Id, SimpleFeature> initShapeFeatures(String shapefile) {
		Map<Id, SimpleFeature> featureMap = new HashMap<Id, SimpleFeature>();

		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(shapefile)) {
			featureMap.put(new IdImpl(feature.getAttribute(ZONE_ID).toString()), feature);
		}
		return featureMap;
	}
	
	public Coord getRandomLocation(Id zoneID, Coord coord){
		if(isShapefile){
			SimpleFeature feature = this.featureMap.get( zoneID );
			return getRandomPointInFeature(feature);
		}
		else
			return getRandomPointInRadius(coord);
	}
	
	/**
	 * distributing location within a circle defined by a given zone centroid and radius
	 * 
	 * @param rnd
	 * @param zoneCoordinate
	 * @param radius
	 * @return
	 */
	private Coord getRandomPointInRadius(Coord zoneCoordinate){
		Coord p = null;
		double x, y;
		double distance;
		do {
			x = (zoneCoordinate.getX() - radius) + (random.nextDouble() * 2 * radius);
			y = (zoneCoordinate.getY() - radius) + (random.nextDouble() * 2 * radius);
			p = new CoordImpl(x, y);
			
			distance = NetworkUtil.getEuclidianDistance(zoneCoordinate, p);
			
		} while ( distance > radius );
		return p;
	}
	
	/**
	 * distributing locations within a zone given by a shapefile
	 * 
	 * @param rnd
	 * @param feature
	 * @return
	 */
	private Coord getRandomPointInFeature(SimpleFeature feature) {
		Point p = null;
		double x, y;
		do {
			x = feature.getBounds().getMinX() + random.nextDouble() * (feature.getBounds().getMaxX() - feature.getBounds().getMinX());
			y = feature.getBounds().getMinY() + random.nextDouble() * (feature.getBounds().getMaxY() - feature.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!((Geometry) feature.getDefaultGeometry()).contains(p));
		return new CoordImpl(x,y);
	}

	/**
	 * for testing
	 * @param args
	 */
	public static void main(String[] args) {
		
		String shapeFile = "/Users/thomas/Development/opus_home/data/brussels_zone/shapefiles/zone.shp";
		String networkFile = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/belgium_incl_borderArea_clean_simple_epsg31300projection.xml.gz";
		int year = 2001;
		double radius = 300.; // meter
		ReadFromUrbanSimModel readFromUrbanSim = new ReadFromUrbanSimModel(year, shapeFile, radius);
		
		RandomLocationDistributor rld;
		
		// use this as switch
		boolean isShapefileApproach = false;
		
		try{
			InternalConstants.MATSIM_4_OPUS_TEMP = "/Users/thomas/Development/opus_home/matsim4opus/tmp/";
			BufferedWriter writer = IOUtils.getBufferedWriter(InternalConstants.MATSIM_4_OPUS_TEMP + "shootTest.csv");
			writer.write("zone_id, x, y, counter");
			writer.newLine();
			
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			((NetworkConfigGroup)scenario.getConfig().getModule(NetworkConfigGroup.GROUP_NAME)).setInputFile(networkFile);
			ScenarioUtils.loadScenario(scenario);
			
			if(isShapefileApproach)
				rld = new RandomLocationDistributor(shapeFile, radius);
			else
				rld = new RandomLocationDistributor(null, radius);
			
			ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
			readFromUrbanSim.readFacilitiesZones(zones);
			Iterator<? extends ActivityFacility> zoneIterator = zones.getFacilities().values().iterator();
			
			while(zoneIterator.hasNext()){
				ActivityFacility zone = zoneIterator.next();
				Coord zoneCoordinate = zone.getCoord();

				for(int i = 0; i < 10; i++){ // creates 10 test shoots per zone
					Coord c = rld.getRandomLocation(zone.getId(), zoneCoordinate);
					log.info(String.valueOf(zone.getId()) + "," + String.valueOf(c.getX()) + "," + String.valueOf(c.getY()) + "," + String.valueOf(i));
					writer.write(String.valueOf(zone.getId()) + "," + String.valueOf(c.getX()) + "," + String.valueOf(c.getY()) + "," + String.valueOf(i));
					writer.newLine();
				}
			}

			writer.flush();
			writer.close();
			log.info("... done!");
		} catch(IOException e){ e.printStackTrace(); }
	}
}
