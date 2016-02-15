/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.duncan.archive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Shp2Facilities {
	private static final Logger log = Logger.getLogger(Shp2Facilities.class);
	
	private static Collection<SimpleFeature> getPolygons(final SimpleFeatureSource n, final MutableScenario scenario) {
		final Collection<SimpleFeature> polygons = new ArrayList<SimpleFeature>(); // not needed

		ActivityFacilities facilities = scenario.getActivityFacilities();
		facilities.setName("workplaces");
		long cnt = 0 ;
		
		SimpleFeatureIterator it = null;
		try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final SimpleFeature feature = it.next();
			
			String str ;
			
			str = "AREA" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			str = "LU_CODE" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			str = "LU_DESCRIP" ;
			System.out.println( str + ": " + feature.getAttribute(str) ) ; 

			final MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
//				continue;
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			Point center = polygon.getCentroid();
			Coord coord = new Coord(center.getX(), center.getY());
			
			Id<ActivityFacility> id = Id.create( cnt , ActivityFacility.class) ;
			cnt++ ;
			
			ActivityFacilityImpl facility = ((ActivityFacilitiesImpl) facilities).createAndAddFacility(id, coord ) ;
			
			facility.createAndAddActivityOption( (String) feature.getAttribute("LU_CODE") ) ;
			facility.createAndAddActivityOption( (String) feature.getAttribute("LU_DESCRIP") ) ;

		}
		it.close();
		
		new FacilitiesWriter(facilities).write("/home/nagel/landuse.xml.gz") ;

		return polygons; // not needed
	}
	
	public static void main(final String [] args) {
		final String shpFile = "/Users/nagel/shared-svn/studies/north-america/ca/metro-vancouver/facilities/shp/landuse.shp";
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Collection<SimpleFeature> zones = null;
		try {
			zones = getPolygons(ShapeFileReader.readDataFile(shpFile), scenario);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
	}
	

}
