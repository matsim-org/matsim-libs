/* *********************************************************************** *
 * project: org.matsim.*
 * RunCapeTownAccessibilityOnHexGrid.java
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

/**
 * 
 */
package playground.southafrica.projects.erAfrica;

import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

/**
 *
 * @author jwjoubert
 */
public class RunCapeTownAccessibilityOnHexGrid {
	final private static Logger LOG = Logger.getLogger(RunCapeTownAccessibilityOnHexGrid.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunCapeTownAccessibilityOnHexGrid.class.toString(), args);
		String shapefile = args[0];
		String facilitiesFile = args[1];
		String activityType = args[2];
		
		ActivityFacilitiesImpl facilities = getHexGridFacilities(shapefile, activityType);
		
		//FIXME
		Scenario sc = null;
		
		AccessibilityCalculator ac = new AccessibilityCalculator(sc, facilities);
		run(ac);
		
		Header.printFooter();
	}
	
	public static void run(AccessibilityCalculator ac){
		
	}
	
	
	
	public static ActivityFacilitiesImpl getHexGridFacilities(String shapefile, String activityType){
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		
		if(features.size() > 1){
			LOG.warn("More than one SimpleFeature... don't know the impact.");
		}
		MultiPolygon mp = null;
		for(SimpleFeature feature : features){
			Object o = feature.getDefaultGeometry();
			if(o instanceof MultiPolygon){
				mp = (MultiPolygon)o;
			}
		}

		GeneralGrid grid = new GeneralGrid(2000.0, GridType.HEX);
		grid.generateGrid(mp);
		
		int idCounter = 0;
		for(Point p : grid.getGrid().values()){
			Id<ActivityFacility> id = Id.create(idCounter++, ActivityFacility.class); 
			Coord c = CoordUtils.createCoord(p.getX(), p.getY());
			ActivityFacility facility = facilities.getFactory().createActivityFacility(id, c);
			facility.addActivityOption(facilities.getFactory().createActivityOption(activityType));
			facilities.addActivityFacility(facility);
		}
		
		return (ActivityFacilitiesImpl) facilities;
	}

}
