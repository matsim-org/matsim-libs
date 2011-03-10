/* *********************************************************************** *
 * project: matsim
 * FacilitiesToSHP.java
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

package playground.christoph.gis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/*
 * Create a SHP File from a given Facilities File.
 * The SHP File contains the coordinates as well as the capacities
 * of all used ActivityOptions.
 */
public class FacilitiesToSHP {

	final private static Logger log = Logger.getLogger(FacilitiesToSHP.class);
	
	private ActivityFacilitiesImpl facilities;
	private Set<String> activityOptions;
	
	private static String facilitiesFile = "../../matsim/mysimulations/kt-zurich/facilities/10pct/facilities.xml.gz";
	private String shpFile = "../../matsim/mysimulations/kt-zurich/facilities/10pct/facilities.shp";
	
	private CoordinateTransformation transformator = TransformationFactory.getCoordinateTransformation("EPSG:21781", "EPSG:4326");	// CH1903LV03 to WGS84
	private CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
	
	public static void main(String[] args) throws Exception {		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((ScenarioImpl)scenario).readFile(facilitiesFile);
		new FacilitiesToSHP(scenario);
	}
	
	public FacilitiesToSHP(Scenario scenario) throws Exception {
		facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		
		identifyActivityOptions();
		
		writeFacilitiesToSHP();
	}
	
	/*
	 * Identifies all used ActivityOptions
	 */
	private void identifyActivityOptions() {
		
		activityOptions = new HashSet<String>();
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				activityOptions.add(activityOption.getType().intern());
			}
		}
		log.info("Found " + activityOptions.size() + " ActivityOptions:");
		int i = 1;
		for (String activityOption : activityOptions) {
			log.info(i++ + ": " + activityOption);
		}
	}
	
	private void writeFacilitiesToSHP() throws Exception {
		
		GeometryFactory geoFac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		
		/*
		 * Basic Facility Attributes
		 */
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, crs);
		AttributeType id = AttributeTypeFactory.newAttributeType("Id", String.class);
		AttributeType desc = AttributeTypeFactory.newAttributeType("desc", String.class);
		
		/*
		 * ActivityOptions - at the moment we ignore opening times
		 */
		// create a list and sort it
		List<String> activityOptions = new ArrayList<String>(this.activityOptions);
		Collections.sort(activityOptions);
		
//		AttributeType[] activityOptionType = new AttributeType[activityOptions.size()];
		AttributeType[] capacities = new AttributeType[activityOptions.size()];
		for (int i = 0; i < activityOptions.size(); i++) {
//			activityOptionType[i] = AttributeTypeFactory.newAttributeType("ActivityOption: " + activityOptions.get(i), String.class);
//			capacities[i] = AttributeTypeFactory.newAttributeType("Capacity (" + activityOptions.get(i) + ")", Double.class);
			capacities[i] = AttributeTypeFactory.newAttributeType(activityOptions.get(i), Double.class);
		}
		
		AttributeType[] array = new AttributeType[3 + activityOptions.size()];
		array[0] = geom;
		array[1] = id;
		array[2] = desc;
		
		for (int i = 0; i < activityOptions.size(); i++) {
			array[i + 3] = capacities[i];
		}
		
		FeatureType ftNode = FeatureTypeBuilder.newFeatureType(array, "facility");
				
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Id facilityId = facility.getId();
			Coord transformedCoord = transformator.transform(facility.getCoord());
			
			Coordinate coord = new Coordinate(transformedCoord.getX(), transformedCoord.getY());
			Point point = geoFac.createPoint(coord);
			
			String description = ((ActivityFacilityImpl) facility).getDesc();
			
			double[] capacityValues = new double[activityOptions.size()];

			// by default: capacity = 0.0
			for (int i = 0; i < activityOptions.size(); i++) capacityValues[i] = 0.0;
			
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				int index = activityOptions.indexOf(activityOption.getType().intern());
				capacityValues[index] = activityOption.getCapacity();
			}
			
			Object[] object = new Object[3 + activityOptions.size()];
			object[0] = point;
			object[1] = facilityId.toString();
			object[2] = description;
			
			for (int i = 0; i < activityOptions.size(); i++) {
				object[i + 3] = capacityValues[i];
			}
			
			Feature ft = ftNode.create(object, "facilities");
			features.add(ft);
		}
		
		ShapeFileWriter.writeGeometries(features, shpFile);
	}
}
