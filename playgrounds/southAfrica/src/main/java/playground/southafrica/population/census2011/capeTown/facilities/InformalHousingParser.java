/* *********************************************************************** *
 * project: org.matsim.*
 * InformalHousingParser.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.population.census2011.capeTown.facilities;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;

/**
 * Class to parse the shapefile containing informal settlements as 
 * provided by the City of Cape Town.
 *  
 * @author jwjoubert
 */
public class InformalHousingParser {
	final private static Logger LOG = Logger.getLogger(InformalHousingParser.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(InformalHousingParser.class.toString(), args);
		
		String shapefile = args[0];
		String facilitiesFile = args[1];
		
		ShapeFileReader sr = new ShapeFileReader();
		sr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sr.getFeatureSet();
		
		/* Hard code the coordinate conversion. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("SA_Lo19", "SA_Lo19");
		
		LOG.info("Converting each settlement into informal housing units... (" + features.size() + " in total)");
		Counter counter = new Counter("  settlement # ");
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();
		for(SimpleFeature sf : features){
			Object oArea = sf.getDefaultGeometry();
			if(oArea instanceof MultiPolygon){
				MultiPolygon area = (MultiPolygon)oArea;
				
				/* Get ID. */
				String id = sf.getAttribute("OBJECTID").toString();
				
				/* Check each of the counts. */
				Object oSourceA = sf.getAttribute("SW_GPS_CNT");
				double sourceA = 0;
				if(oSourceA instanceof Double){
					sourceA = (Double)oSourceA;
				}
				
				Object oSourceB = sf.getAttribute("CG_ROOF_CN");
				double sourceB = 0;
				if(oSourceB instanceof Double){
					sourceB = (Double)oSourceB;
				}
				
				Object oSourceC = sf.getAttribute("ISM_STR_CN");
				double sourceC = 0;
				if(oSourceC instanceof Double){
					sourceC = (Double)oSourceC;
				}
				
				int count = (int) Math.round(sourceA + sourceB + sourceC);
				
				/* Create an informal housing facility for each unit. */
				ActivityOption option = factory.createActivityOption(FacilityTypes.HOME);
				option.setCapacity(1.0);
				for(int j = 0; j < count; j++){
					Point p = getRandomInteriorPoint(area);
					Coord c = ct.transform(CoordUtils.createCoord(p.getX(), p.getY()));
					ActivityFacility f = factory.createActivityFacility(
							Id.create("inf_" + id + "_" + j, ActivityFacility.class), c);
					f.addActivityOption(option);
					facilities.addActivityFacility(f);
				}
				
			} else{
				/* Ignore the area. */
			}
			
			counter.incCounter();
		}
		counter.printCounter();
		
		new FacilitiesWriter(facilities).write(facilitiesFile);
		
		Header.printFooter();
	}
	
	private static Point getRandomInteriorPoint(MultiPolygon mp){
		GeometryFactory gf = new GeometryFactory();
		Point p = null;
		
		Geometry envelope = mp.getEnvelope();
		do{
			Coordinate[] ca = envelope.getCoordinates();
			double x = ca[0].x + Math.random()*(ca[2].x - ca[0].x);
			double y = ca[0].y + Math.random()*(ca[2].y - ca[0].y);
			Point pp = gf.createPoint(new Coordinate(x, y));
			if(mp.covers(pp)){
				p = pp;
			}
		} while(p == null);
				
		return p;
	}

}
