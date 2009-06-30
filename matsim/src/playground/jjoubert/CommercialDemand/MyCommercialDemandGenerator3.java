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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.Scenario;
import org.matsim.core.api.ScenarioImpl;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.demandmodeling.primloc.CumulativeDistribution;

import playground.jjoubert.CommercialTraffic.ActivityLocations;
import playground.jjoubert.CommercialTraffic.Chain;
import playground.jjoubert.CommercialTraffic.Vehicle;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MyCommercialDemandGenerator3 {
	// String value that must be set
	final static String PROVINCE = "Gauteng";
	// Mac
//	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
	final static String ROOT = "/home/jjoubert/";
	
	private static final int populationSize = 5000;
	private static final int firstIndex = 100000;
	private static double WITHIN_THRESHOLD = 0.90;
	private static final int numberOfSamples = 10;

	private final static int dimensionStart = 24; 		// vales 00h00m00 - 23h59m59
	private final static int dimensionActivities = 21; 	// index '0' should never be used
	private final static int dimensionDuration = 49; 	// index '0' should never be used

	@SuppressWarnings("unchecked")
	public static void main(String[] args){
		System.out.println();
		System.out.println("*****************************************************************************************");
		System.out.printf("  Generating %d 'plans.xml' files for %s, each with %d commercial agents.\n", numberOfSamples, PROVINCE, populationSize);
		System.out.println("*****************************************************************************************");
		System.out.println();

		// Analyze vehicle chains
		ArrayList<ArrayList<ArrayList<Integer>>> matrix = extractChainProperties();
		// Build CDF for chain start times		
		CumulativeDistribution cdfStartTime = convertMatrixToStartTimeCDF(matrix);
		// Build empty CDF for number of activities
		ArrayList<CumulativeDistribution> cdfNumberOfActivities = new ArrayList<CumulativeDistribution>();
		for(int a = 0; a < matrix.size(); a++){
			CumulativeDistribution cdf = null;
			cdfNumberOfActivities.add(cdf);
		}
		// Build an empty CDF for chain duration
		ArrayList<ArrayList<CumulativeDistribution>> cdfDuration = new ArrayList<ArrayList<CumulativeDistribution>>();
		for(int a = 0; a < matrix.size(); a++){
			ArrayList<CumulativeDistribution> cdfDurationn = new ArrayList<CumulativeDistribution>();
			for(int b = 0; b < matrix.get(a).size(); b++){
				CumulativeDistribution cdf = null;
				cdfDurationn.add(cdf);
			}
			cdfDuration.add(cdfDurationn);
		}
		
		/**
		 * TODO Currently I create points in ArcGIS based on the Kernel Density Estimation (KDE). To 
		 * pick a location, I simply draw a random point from the sample. In future, it might be better
		 * to read the KDE raster into MATSim, and sample directly from it.
		 */
		
		// Read major locations
		ArrayList<Point> majorPoints = readLocations(ROOT + "Commercial/Input/CommercialMajor100000.shp");
		// Read minor locations
		ArrayList<Point> minorPoints = readLocations(ROOT + "Commercial/Input/CommercialMinor100000.shp");
				
		for(int sampleNumber = 1; sampleNumber <= numberOfSamples; sampleNumber++){
			// Initiate the population builder
			Scenario sc = new ScenarioImpl();
			Population population = sc.getPopulation();
			PopulationBuilder pb = population.getPopulationBuilder();

			//TODO Check if it is 'better' to create 'i' truck agents, and split them into dummy agents
			// if the duration is greater than 24 hours - thus probably ending with more than 'i' agents;
			// or create agents UNTIL you have 'i' - including the dummies.
			System.out.printf("Building sample %d of %d population plans...\n", sampleNumber, numberOfSamples);
			int populationComplete = 0;
			int populationLimit = 1;
			int agentId  = firstIndex;
			for(int i = 0; i < populationSize; i++ ){
				Plan plan = new PlanImpl(null);

				// Sample start time
				int startTimeBin = (int) cdfStartTime.sampleFromCDF();
				int startTime = startTimeBin * 3600; // Convert hours to minutes

				// Sample major location and add as first activity
				Point major = majorPoints.get(MatsimRandom.getRandom().nextInt(majorPoints.size()));
				Activity majorActivityStart = new ActivityImpl("major", sc.createCoord(major.getCoordinate().x, major.getCoordinate().y));
				majorActivityStart.setEndTime(startTime);
				plan.getPlanElements().add(majorActivityStart);

				Leg leg = new LegImpl(TransportMode.car);
				plan.addLeg(leg);

				// Sample number of activities, given start time
				int activitiesPerChain = 0;
				if(cdfNumberOfActivities.get(startTimeBin) == null){
					// Build new CDF
					CumulativeDistribution cdfNew = convertMatrixToNumberOfActivitiesCDF(matrix, startTimeBin);
					cdfNumberOfActivities.set(startTimeBin, cdfNew);
				} 
				// Sample from cdfNumberOfActivities
				int numberOfActivitiesBin = (int) cdfNumberOfActivities.get(startTimeBin).sampleFromCDF();
				activitiesPerChain = numberOfActivitiesBin;


				// Sample duration, given start time AND number of activities
				int endTime = 0;
				if(cdfDuration.get(startTimeBin).get(numberOfActivitiesBin) == null){
					// Build new CDF
					CumulativeDistribution cdfNew = convertMatrixToDurationCDF(matrix, startTimeBin, numberOfActivitiesBin);
					cdfDuration.get(startTimeBin).set(numberOfActivitiesBin, cdfNew);
				}
				// Sample from cdfDuration. Limit the end time to 48:00:00 i.e. end of second day
				int durationBin = (int) cdfDuration.get(startTimeBin).get(numberOfActivitiesBin).sampleFromCDF();
				endTime = Math.min(startTime + (Math.max(1, durationBin) * 3600), 172800);

				// Establish time 'gaps' based on the number of activities
				double gap = (endTime - startTime) / (activitiesPerChain + 1); // (Duration) / (n+1)
				double activityEndTime = startTime + gap;

				for(int activity = 0; activity < activitiesPerChain; activity++){
					// Sample minor point
					Point minor = minorPoints.get(MatsimRandom.getRandom().nextInt(minorPoints.size()));
					
					Activity minorActivity = new ActivityImpl("minor", sc.createCoord(minor.getCoordinate().x, minor.getCoordinate().y));
					minorActivity.setEndTime(activityEndTime);

					plan.getPlanElements().add(minorActivity);	
					activityEndTime += gap;	

					plan.addLeg(leg);				
				}
				Activity majorActivityEnd = new ActivityImpl("major", majorActivityStart.getCoord());
				majorActivityEnd.setStartTime(endTime);
				plan.getPlanElements().add(majorActivityEnd);

				PlanWrapper pw = new PlanWrapper(86400, 0);
				ArrayList<Plan> planList = pw.wrapPlan(plan);

				for (Plan pp : planList) {
					// Create a truck agent
					Id id = sc.createId(Long.toString(agentId));
					Person truck = pb.createPerson(id);
					truck.addPlan(pp);
					pp.setPerson(truck);
//					pp.setSelected(true);
					agentId++;
					//TODO This is where I may want to include a ++populationComplete command if I only want a total of
					// populationSize commercial vehicle agents.
				}
				
				// Report progress
				if(++populationComplete == populationLimit){
					System.out.printf("   ... Agents built: %6d\n", populationComplete);
					populationLimit *= 2;
				}
			}
			System.out.printf("   ... Done (%d agents)\n", populationComplete);

			// Write plans.xml file
			System.out.print("   Writing plans to XML file... ");
			PopulationWriter pw = new PopulationWriter(population, ROOT + "Commercial/plans" + 
					PROVINCE + String.valueOf(populationSize) + "_Sample" + sampleNumber + ".xml");
			pw.write();
			System.out.println("Done!");
			System.out.printf("Plans generation: Completed for sample %d of %d\n\n", sampleNumber, numberOfSamples);
		}
	}

	private static CumulativeDistribution convertMatrixToDurationCDF(
			ArrayList<ArrayList<ArrayList<Integer>>> matrix, int bin1,
			int bin2) {
		CumulativeDistribution result = null;
		int total = 0;
		ArrayList<Integer> observations = new ArrayList<Integer>(dimensionDuration);
		ArrayList<Double> probs = new ArrayList<Double>(dimensionDuration);
		for(int a = 0; a < matrix.get(bin1).get(bin2).size(); a++){
			int points = (int) matrix.get(bin1).get(bin2).get(a);
			total += points;
			observations.add(points);
		}
		for(int a = 0; a < matrix.get(bin1).get(bin2).size(); a++){
			double prob = (double)observations.get(a) / (double)total;
			probs.add(prob);
		}
		double [] xs = new double[dimensionDuration+1];
		xs[0] = -0.5;
		for(int a = 1; a < xs.length; a++){
			xs[a] = a - 0.5;
		}
		double [] ys = new double[dimensionDuration+1];
		ys[0] = 0.0;
		for(int b = 1; b < ys.length; b++){
			ys[b] = ys[b-1] + probs.get(b-1);
		}
		ys[ys.length-1] = 1;
		
		result = new CumulativeDistribution(xs,ys);		
		
		return result;
	}


	private static CumulativeDistribution convertMatrixToNumberOfActivitiesCDF(
			ArrayList<ArrayList<ArrayList<Integer>>> matrix, int bin) {
		CumulativeDistribution result = null;
		int total = 0;
		ArrayList<Integer> observations = new ArrayList<Integer>(dimensionActivities);
		ArrayList<Double> probs = new ArrayList<Double>(dimensionActivities);
		for(int a = 0; a < matrix.get(bin).size(); a++){
			int points = 0;
			for(int b = 0; b < dimensionDuration; b++){
				points += matrix.get(bin).get(a).get(b);				
			}
			total += points;
			observations.add(points);
		}
		for(int a = 0; a < matrix.get(bin).size(); a++){
			double prob = (double)observations.get(a) / (double)total;
			probs.add(prob);
		}
		double [] xs = new double[dimensionActivities+1];
		xs[0] = -0.5;
		for(int a = 1; a < xs.length; a++){
			xs[a] = a - 0.5;
		}
		double [] ys = new double[dimensionActivities+1];
		ys[0] = 0.0;
		for(int b = 1; b < ys.length; b++){
			ys[b] = ys[b-1] + probs.get(b-1);
		}
		ys[ys.length-1] = 1;
		
		result = new CumulativeDistribution(xs,ys);		
		
		return result;
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
			double prob = (double)observations.get(a) / (double)total;
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
		ys[ys.length-1] = 1;
		
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
	 * 	<ul>
	 * 	<li> Activity chain start time (00h00m00 : 01h00m00 : 23h00m00)
	 * 	<li> Number of activities per chain (1 : 1 : 20)
	 * 	<li> Chain duration (01h00m00 : 01h00m00 : 48h00m00)
	 * 	</ul>
	 * 
	 * @param args
	 */
	private static ArrayList<ArrayList<ArrayList<Integer>>> extractChainProperties() {

		// Build ArrayList with 'within' vehicles
		String vehicleSource = ROOT + PROVINCE + "/Activities/" + PROVINCE + "VehicleStats.txt";
//		String vehicleSource = ROOT + "/Temp/TempVehicleStats.txt";
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
//		String xmlSource = ROOT + "Temp/XML/";
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
					processLimit *= 2;
				}
			}
		}
		System.out.printf("   ... Done (%d files)\n", filesProcessed);
		return matrix;
	}

}
