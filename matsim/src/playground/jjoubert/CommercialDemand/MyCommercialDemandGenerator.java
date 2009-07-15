/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGenerator.java
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

package playground.jjoubert.CommercialDemand;

import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.demandmodeling.primloc.CumulativeDistribution;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MyCommercialDemandGenerator {
	
	private static final int populationSize = 5000;
	private static final int firstIndex = 100000;
	private static final String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/CommercialDemand/";

	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		
		// Read major locations
		ArrayList<Point> majorPoints = readLocations(ROOT + "InputData/CommercialMajor10000.shp");
		/**
		 * TODO Currently I create points in ArcGIS based on the Kernel Density Estimation (KDE), 10000 in this
		 * case. To pick a location, I simply draw a random point from the sample. In future, it might be better
		 * to read the KDE raster into MATSim, and sample directly from it.
		 */
		
		// Read minor locations
		ArrayList<Point> minorPoints = readLocations(ROOT + "InputData/CommercialMinor10000.shp");

		// Initiate the population builder
		BasicScenario sc = new BasicScenarioImpl();		
		BasicPopulation population = sc.getPopulation();
		BasicPopulationBuilder pb = population.getBuilder();

		
		// Read cumulative distribution function (CDF) for number of activities per chain
		CumulativeDistribution numberOfActivitiesCDF = CumulativeDistribution.readDistributionFromFile(ROOT + "InputData/numberOfActivitiesCDF.txt");

		// Read cumulative distribution function (CDF) for the activity duration
		@SuppressWarnings("unused")
		CumulativeDistribution activityDurationCDF = CumulativeDistribution.readDistributionFromFile(ROOT + "InputData/activityDurationCDF.txt");
		
		for(int i = 0; i < populationSize; i++ ){
			// Create a truck agent
			Id id = sc.createId(Long.toString(firstIndex + i));
			BasicPerson truck = pb.createPerson(id);
			population.getPersons().put(id, truck);
			
			BasicPlan plan = pb.createPlan(truck);
			truck.getPlans().add(plan);
						
			// Sample major location and add as first activity
			Point major = majorPoints.get(MatsimRandom.getRandom().nextInt(majorPoints.size()));
			BasicActivity majorActivityStart = pb.createActivityFromCoord("major", 
					sc.createCoord(major.getCoordinate().x, major.getCoordinate().y));
			majorActivityStart.setEndTime(28800); // 08:00:00 (HH:MM:SS)
			plan.getPlanElements().add(majorActivityStart);
			
			BasicLeg leg = pb.createLeg(TransportMode.car);
			plan.addLeg(leg);
			
			// Draw a sample to determine the number of 'minor' activities for this agent's chain
			int activitiesPerChain = (int) numberOfActivitiesCDF.sampleFromCDF();
			
			// Establish time 'gaps' based on the number of activities
			double gap = (79199 - 28800) / (activitiesPerChain + 1); // (21:59:59 - 08:00:00) / (n+1)
			double endTime = 28800 + gap;
			
			for(int activity = 0; activity < activitiesPerChain; activity++){
				// Sample minor point
				Point minor = minorPoints.get(MatsimRandom.getRandom().nextInt(minorPoints.size()));
				BasicActivity minorActivity = pb.createActivityFromCoord("minor", 
						sc.createCoord(minor.getCoordinate().x, minor.getCoordinate().y));
				minorActivity.setEndTime(endTime);
//				double activityDuration = activityDurationCDF.sampleFromCDF();
				
				plan.getPlanElements().add(minorActivity);	
				endTime += gap;	
				
				plan.addLeg(leg);				
			}
			BasicActivity majorActivityEnd = pb.createActivityFromCoord("major", majorActivityStart.getCoord());
			majorActivityEnd.setStartTime(79200);
			plan.getPlanElements().add(majorActivityEnd);
			
		}
		
		// Write plans.xml file
		PopulationWriter pw = new PopulationWriter(population, ROOT + "plans.xml");
		pw.write();
	}


	private static ArrayList<Point> readLocations( String source ) {
		
		FeatureSource fs = null;
		ArrayList<Point> points = new ArrayList<Point>();
		Point p = null;
		try {	
			fs = ShapeFileReader.readDataFile( source );
			for(Object o: fs.getFeatures() ){
				Geometry geo = ((Feature)o).getDefaultGeometry();
				if(geo instanceof Point){
					p = (Point)geo;
					points.add(p);
				} else{
					System.err.println("The shapefile is not made up of only points!");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return points;
	}

}
