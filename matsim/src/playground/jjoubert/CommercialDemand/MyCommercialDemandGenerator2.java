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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

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

import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.Vehicle;
import playground.jjoubert.CommercialTraffic.ActivityLocations;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MyCommercialDemandGenerator2 {
	
	private static final int populationSize = 10;
	private static final int firstIndex = 100000;
	private static final String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	private static double WITHIN_THRESHOLD = 0.90;
	private static final String PROVINCE = "Gauteng";

	private final static int dimensionStart = 24; 		// vales 00h00m00 - 23h59m59
	private final static int dimensionActivities = 21; 	// index '0' should never be used
	private final static int dimensionDuration = 49; 	// index '0' should never be used

	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		
		// Analyze vehicle chains
		ArrayList<ArrayList<ArrayList<Integer>>> matrix = extractChainProperties();
		// Build CDF for chain start times		
		CumulativeDistribution cdfStartTime = convertMatrixToStartTimeCDF(matrix);
		
		
		// Read major locations
		ArrayList<Point> majorPoints = readLocations(ROOT + "CommercialDemand/InputData/CommercialMajor10000.shp");
		/**
		 * TODO Currently I create points in ArcGIS based on the Kernel Density Estimation (KDE), 10000 in this
		 * case. To pick a location, I simply draw a random point from the sample. In future, it might be better
		 * to read the KDE raster into MATSim, and sample directly from it.
		 */
		
		// Read minor locations
		ArrayList<Point> minorPoints = readLocations(ROOT + "CommercialDemand/InputData/CommercialMinor10000.shp");

		// Initiate the population builder
		BasicScenario sc = new BasicScenarioImpl();		
		BasicPopulation population = sc.getPopulation();
		BasicPopulationBuilder pb = population.getPopulationBuilder();

		//TODO Check if it is 'better' to create 'i' truck agents, and split them into dummy agents
		// if the duration is greater than 24 hours - thus probably ending with more than 'i' agents;
		// or create agents UNTIL you have 'i' - including the dummies.
		for(int i = 0; i < populationSize; i++ ){
			// Sample start time
			int startTime = (int) cdfStartTime.sampleFromCDF() * 3600; // Convert hours to minutes
			
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
			majorActivityStart.setEndTime(startTime);
			plan.getPlanElements().add(majorActivityStart);

			BasicLeg leg = pb.createLeg(TransportMode.car);
			plan.addLeg(leg);

			//TODO Sample number of activities, given start time
			int activitiesPerChain = 0;
			//TODO Sample duration, given start time AND number of activities
			int endTime = 0;
			
			// Establish time 'gaps' based on the number of activities
			double gap = (endTime - startTime) / (activitiesPerChain + 1); // (Duration) / (n+1)
			
			for(int activity = 0; activity < activitiesPerChain; activity++){
				// Sample minor point
				Point minor = minorPoints.get(MatsimRandom.getRandom().nextInt(minorPoints.size()));
				BasicActivity minorActivity = pb.createActivityFromCoord("minor", 
						sc.createCoord(minor.getCoordinate().x, minor.getCoordinate().y));
				minorActivity.setEndTime(endTime);
				
				plan.getPlanElements().add(minorActivity);	
				endTime += gap;	
				
				plan.addLeg(leg);				
			}
			BasicActivity majorActivityEnd = pb.createActivityFromCoord("major", majorActivityStart.getCoord());
			majorActivityEnd.setStartTime(endTime);
			plan.getPlanElements().add(majorActivityEnd);
			
			//TODO Now I have a nice long chain. If the chain ends before 24:00:00, no problem. If, on the other
			// hand, the chain ends after 24:00:00, I need to find the last activity of the day, make it the "major"
			// activity for that day; duplicate it as the first home activity ending at 00:00:00 for a new dummy
			// agent, and add the remaining activities to the new dummy agent. Note: the activity end times must be 
			// adjusted for the remaining activities, and the new dummy agent must be checked to see if the new end 
			// chain end time is before 24:00:00.		
			
		}
		
		// Write plans.xml file
	}


	private static CumulativeDistribution convertMatrixToStartTimeCDF(
			ArrayList<ArrayList<ArrayList<Integer>>> matrix) {
		
		CumulativeDistribution result = null;
		int total = 0;
		ArrayList<Integer> observations = new ArrayList<Integer>(dimensionStart);
		ArrayList<Double> probs = new ArrayList<Double>(dimensionStart);
		for(int a = 0; a < matrix.size(); a++){
			int points = 0;
			for(int b = 0; b < dimensionActivities; b++){
				for(int c = 0; c < dimensionDuration; c++){
					points += matrix.get(a).get(b).get(c);
				}
			}
			total += points;
			observations.add(points);
		}
		for(int a = 0; a < matrix.size(); a++){
			double prob = observations.get(a) / total;
			probs.add(prob);
		}
		double [] xs = new double[dimensionStart+1];
		xs[0] = -0.5;
		for(int a = 1; a < xs.length; a++){
			xs[a] = a - 0.5;
		}
		double [] ys = new double[dimensionStart+1];
		ys[0] = 0.0;
		for(int b = 1; b < ys.length; b++){
			ys[b] = ys[b-1] + probs.get(b-1);
		}
		
		result = new CumulativeDistribution(xs,ys);		
		
		return result;
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
	
	/**
	 * The class processes commercial vehicle chains and classifies each so as to
	 * establish a multi-dimensional probability matrix. The matrix can then be
	 * used to generate a synthetic sample of commercial vehicle agents.
	 * 
	 * The characteristics extracted are:
	 * 	- Activity chain start time (00h00m00 : 01h00m00 : 23h00m00)
	 * 	- Number of activities per chain (1 : 1 : 20)
	 * 	- Chain duration (01h00m00 : 01h00m00 : 48h00m00)
	 * @param args
	 */
	private static ArrayList<ArrayList<ArrayList<Integer>>> extractChainProperties() {

		// Build ArrayList with 'within' vehicles
		String vehicleSource = ROOT + PROVINCE + "/Activities/" + PROVINCE + "VehicleStats.txt";
		ArrayList<Integer> withinVehicles = 
			ExtractWithinActivityDurations.buildWithinVehicleIdList(vehicleSource, WITHIN_THRESHOLD);
		
		// Build the three-dimensional array

		ArrayList<ArrayList<ArrayList<Integer>>> matrix = new ArrayList<ArrayList<ArrayList<Integer>>>();
		for(int a = 0; a < dimensionStart; a++){
			ArrayList<ArrayList<Integer>> matrixx = new ArrayList<ArrayList<Integer>>();
			for(int b = 0; b < dimensionActivities; b++){
				ArrayList<Integer> matrixxx = new ArrayList<Integer>(dimensionDuration);
				for(int c = 0; c < dimensionDuration; c++){
					matrixxx.add(0);
				}
				matrixx.add(matrixxx);
			}
			matrix.add(matrixx);
		}		
		
		// Process XML files
		String xmlSource = ROOT + PROVINCE + "/XML/";
		File xmlFolder = new File(xmlSource);
		assert( xmlFolder.isDirectory() ) : "The XML source is not a valid folder!";
		
		System.out.println("Processing XML files...");
		int filesProcessed = 0;
		int processLimit = 1;
		for (File file : xmlFolder.listFiles()) {
			if(file.isFile() && !file.getName().startsWith(".")){
				// Check if the vehicle is considered 'within'
				int vehicleNumber = Integer.parseInt(file.getName().substring(0, file.getName().indexOf(".")));
				if(withinVehicles.contains(vehicleNumber)){
					// Convert XML file to Vehicle
					Vehicle vehicle = ActivityLocations.convertVehicleFromXML(
							ActivityLocations.readVehicleStringFromFile(vehicleNumber, xmlSource + file.getName()));
					
					// Analyze each chain
					for (Chain chain : vehicle.getChains()) {
						// Chain start time
						Integer index1 = null;
						GregorianCalendar chainStart = chain.getActivities().get(0).getEndTime();
						index1 = chainStart.get(Calendar.HOUR_OF_DAY);
						
						// Number of activities
						Integer index2 = null;
						index2 = Math.min(20, chain.getActivities().size() - 2); // Do not count the two major activities at either end of the chain.
						
						// Chain duration
						Integer index3 = null;
						GregorianCalendar chainEnd = chain.getActivities().get(chain.getActivities().size() - 1).getStartTime();
						Long durationMilliseconds = chainEnd.getTimeInMillis() - chainStart.getTimeInMillis();
						Integer durationHours = Math.round(durationMilliseconds / (1000 * 60 * 60) );
						index3 = Math.min(47, durationHours);						
						
						assert( (index1 != null) && (index2 != null) && (index3 != null) ) : "One of the indices are null!!";
						int dummy = matrix.get(index1).get(index2).get(index3) + 1;
						matrix.get(index1).get(index2).set(index3, dummy);						
					}
				}
				// Update progress
				if(++filesProcessed == processLimit){
					System.out.printf("   ... Files processed: %6d\n", filesProcessed);
					processLimit*=2;
				}
			}
		}
		System.out.printf("   ... Done (%d files)\n", filesProcessed);
		return matrix;
	}


}
