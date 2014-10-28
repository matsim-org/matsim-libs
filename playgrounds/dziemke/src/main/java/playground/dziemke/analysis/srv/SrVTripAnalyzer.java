/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
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

package playground.dziemke.analysis.srv;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.Trip;

/**
 * @author dziemke
 * adapted from TripAnalyzer04
 *
 */
public class SrVTripAnalyzer {

	private final static Logger log = Logger.getLogger(SrVTripAnalyzer.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Parameters
		boolean useWeights = true;			//wt
		boolean onlyCar = false;			//car
		boolean onlyCarAndCarPool = true;	//carp
		boolean onlyHomeAndWork = true;		//hw
		boolean distanceFilter = true;		//dist
		//double minDistance = 0;
		double maxDistance = 100;
	    
		int maxBinDuration = 120;
	    int binWidthDuration = 1;
	    
	    int maxBinTime = 23;
	    int binWidthTime = 1;
	    
	    int maxBinDistance = 60;
	    int binWidthDistance = 1;
	    	    
	    int maxBinSpeed = 60;
	    int binWidthSpeed = 1;
	    
	    
		// Input and output files
		String inputFile = "D:/Workspace/container/srv/input/W2008_Berlin_Weekday.dat";
		
		String outputDirectory = "D:/Workspace/container/srv/output/wd";
		
		if (useWeights == true) {
			outputDirectory = outputDirectory + "_wt";
		}
		
		if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
		
		if (onlyCarAndCarPool == true) {
			outputDirectory = outputDirectory + "_carp";
		}
		
		if (onlyCar == false && onlyCarAndCarPool == false) {
			outputDirectory = outputDirectory + "_all";
		}
				
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		
		if (onlyHomeAndWork == true) {
			outputDirectory = outputDirectory + "_hw";
		}		
				
		outputDirectory = outputDirectory + "/";
		
		
		// parse the input file
		log.info("Parsing " + inputFile + ".");		
		SrVTripParser parser = new SrVTripParser();
		parser.parse(inputFile);
		log.info("Finished parsing.");
		
		
		// create objects
    	int tripCounter = 0;
    	
    	Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	    double aggregateTripDuration = 0.;
	    double aggregateWeightTripDuration = 0.;
	    //int tripDurationCounter = 0;
	    
	    Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
	    double aggregateWeightDepartureTime = 0.;
	    
	    Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
	    double aggregateWeightActivityTypes = 0.;
	    //Map <String, Double> activityTypePreviousMap = new TreeMap <String, Double>();
		
		Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceRouted = 0.;
		double aggregateWeightTripDistanceRouted = 0.;
		//int tripDistanceRoutedCounter = 0;
		
		Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceBeeline = 0.;
		double aggregateWeightTripDistanceBeeline = 0.;
		//int tripDistanceBeelineCounter = 0;
	    
		Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsRouted = 0.;
	    double aggregateWeightTripSpeedRouted = 0.;
	    //int averageTripSpeedRoutedCounter = 0;

	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    double aggregateWeightTripSpeedBeeline = 0.;
	    //int averageTripSpeedBeelineCounter = 0;
	    
	    Map <Integer, Double> averageTripSpeedProvidedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsProvided = 0.;
	    double aggregateWeightTripSpeedProvided = 0.;
	    //int averageTripSpeedProvidedCounter = 0;

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	    Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
	    
	    
	    // do calculations
	    for (Trip trip : parser.getTrips().values()) {
	    	// mode of transport
	    	// reliant on variable "V_HHPKW_F": 0/1
		    int useHouseholdCar = trip.getUseHouseholdCar();
		    // reliant on variable "V_ANDPKW_F": 0/1
		    int useOtherCar = trip.getUseOtherCar();
		    // reliant on variable "V_HHPKW_MF": 0/1
		    int useHouseholdCarPool = trip.getUseHouseholdCarPool();
		    // reliant on variable "V_ANDPKW_MF": 0/1
		    int useOtherCarPool = trip.getUseOtherCarPool();
		    
		    // new
		    String activityEndActType = trip.getActivityEndActType();
		    String activityStartActType = trip.getActivityStartActType();
		    // end new
		    
		    boolean considerTrip = false;
		    if (onlyHomeAndWork == true) {
		    	// Variable has value "home" if activity is "home" and "???" if activity is "work"
		    	if ((activityEndActType.equals("home") && activityStartActType.equals("work")) || 
		    			(activityEndActType.equals("work") && activityStartActType.equals("home"))) {
		    	// if (activityStartActType.equals("home") || activityStartActType.equals("work")) {
		    	// if (activityEndActType.equals("home") && activityStartActType.equals("work")) {
				    if (onlyCar == true) {
				    	if (useHouseholdCar == 1 || useOtherCar == 1) {		 
				    		considerTrip = true;
				    	}
				    } else if (onlyCarAndCarPool == true) {
				    	if (useHouseholdCar == 1 || useOtherCar == 1 || 
				    			useHouseholdCarPool == 1 || useOtherCarPool == 1) {		 
				    		considerTrip = true;
				    	}
				    } else {
				    	considerTrip = true;
				    }
		    	}
		    } else {
		    	if (onlyCar == true) {
			    	if (useHouseholdCar == 1 || useOtherCar == 1) {		 
			    		considerTrip = true;
			    	}
			    } else if (onlyCarAndCarPool == true) {
			    	if (useHouseholdCar == 1 || useOtherCar == 1 || 
			    			useHouseholdCarPool == 1 || useOtherCarPool == 1) {		 
			    		considerTrip = true;
			    	}
			    } else {
			    	considerTrip = true;
			    }
		    }
		    
		    // distance filter
		    double tripDistanceBeeline = trip.getDistanceBeeline();
		    if (distanceFilter == true && tripDistanceBeeline >= maxDistance) {
		    	considerTrip = false;
		    }
		    
//		    if (distanceFilter == true && tripDistanceBeeline <= minDistance) {
//			    considerTrip = false;
//			}
		    
		    
		    if (considerTrip == true) {		    		
		    	tripCounter++;
		   		
		   		// weights
		   		double weight;
		   		if (useWeights == true) {
		   			weight = trip.getWeight();
		   		} else {
		   			weight = 1.;
		   		}
	    		
	    		// calculate travel times and store them in a map
	    		// reliant on variable "V_ANKUNFT": -9 = no data, -10 = implausible
	    		// and on variable "V_BEGINN": -9 = no data, -10 = implausible
		   		// trip.getArrivalTime() / trip.getDepartureTime() yields values in minutes!
		   		double arrivalTimeInMinutes = trip.getArrivalTime();
		   		double departureTimeInMinutes = trip.getDepartureTime();
		   		double departureTimeInHours = departureTimeInMinutes / 60.;
	    		double tripDurationInMinutes = arrivalTimeInMinutes - departureTimeInMinutes;
	    		//double tripDurationInMinutes = trip.getDuration();
	    		double weightedTripDurationInMinutes = tripDurationInMinutes * weight;
	    		double tripDurationInHours = tripDurationInMinutes / 60.;
		    	// there are also 3 cases where time<0; they need to be excluded
		    	if (arrivalTimeInMinutes >= 0 && departureTimeInMinutes >= 0 && tripDurationInMinutes >= 0) {
		    		addToMapIntegerKey(tripDurationMap, tripDurationInMinutes, binWidthDuration, maxBinDuration, weight);
		    		//aggregateTripDuration = aggregateTripDuration + tripDurationInMinutes;
		    		aggregateTripDuration = aggregateTripDuration + weightedTripDurationInMinutes;
		    		aggregateWeightTripDuration = aggregateWeightTripDuration + weight;
		    		//tripDurationCounter++;
		    	}
		    	
		    	
		    	// store departure times in a map
		    	if (departureTimeInHours >= 0) {
		    		addToMapIntegerKey(departureTimeMap, departureTimeInHours, binWidthTime, maxBinTime, weight);
		    		aggregateWeightDepartureTime = aggregateWeightDepartureTime + weight;
		    	}
	
		    	
	    		// store activities in a map
		    	// reliant on variable "V_ZWECK": -9 = no data
		    	// "V_ZWECK" - end of trip = start of activity
		    	String activityType = trip.getActivityStartActType();
				addToMapStringKey(activityTypeMap, activityType, weight);
				aggregateWeightActivityTypes = aggregateWeightActivityTypes + weight;
				
				
				// reliant on variable "V_START_ZWECK": -9 = no data
		    	// "V_START_ZWECK" - start of trip = end of activity
		    	// String activityTypePrevious = trip.getActivityEndActType();
				// addToMapStringKey(activityTypePreviousMap, activityTypePrevious, weight);
				
	
				// In SrV a routed distance (according to some software) is already given
				// reliant on SrV variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
				double tripDistanceRouted = trip.getDistanceRoutedShortest();
				double weightedTripDistanceRouted = weight * tripDistanceRouted;
				if (tripDistanceRouted >= 0.) {
					addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance, maxBinDistance, weight);
					//aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
					aggregateTripDistanceRouted = aggregateTripDistanceRouted + weightedTripDistanceRouted;
					distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);
					aggregateWeightTripDistanceRouted = aggregateWeightTripDistanceRouted + weight;
					//tripDistanceRoutedCounter++;
				}
				
				
	    		// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
	    		//double tripDistanceBeeline = trip.getDistanceBeeline();
	    		double weightedTripDistanceBeeline = weight * tripDistanceBeeline;
	    		if (tripDistanceBeeline >= 0.) {				
	    			addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance, maxBinDistance, weight);
	    			//aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
	    			aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + weightedTripDistanceBeeline;
	    			distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);
	    			aggregateWeightTripDistanceBeeline = aggregateWeightTripDistanceBeeline + weight;
	    			//tripDistanceBeelineCounter++;
	    		}
	    		
	    		
	    		// calculate speeds and and store them in a map
	    		if (tripDurationInHours > 0.) {
	    			// reliant to SrV variable variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
					if (tripDistanceRouted >= 0.) {
	    				double averageTripSpeedRouted = tripDistanceRouted / tripDurationInHours;
						addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed, maxBinSpeed, weight);
						aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;
						aggregateWeightTripSpeedRouted = aggregateWeightTripSpeedRouted + weight;
						//averageTripSpeedRoutedCounter++;
					}
		    	
					// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
		    		if (tripDistanceBeeline >= 0.) {			
		    			double averageTripSpeedBeeline = tripDistanceBeeline / tripDurationInHours;
		    			addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed, maxBinSpeed, weight);
		    			aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;
		    			aggregateWeightTripSpeedBeeline = aggregateWeightTripSpeedBeeline + weight;
		    			//averageTripSpeedBeelineCounter++;
		    		}
	    		} else {
	    			numberOfTripsWithNoCalculableSpeed++;
	    		}
	    		
	    		
	    		// get provided speeds and store them in a map
	    		// reliant on variable "E_GESCHW": -7 = Calculation not possible	    		
	    		double averageTripSpeedProvided = trip.getSpeed();
	    		if (averageTripSpeedProvided >= 0) {
	    			addToMapIntegerKey(averageTripSpeedProvidedMap, averageTripSpeedProvided, binWidthSpeed, maxBinSpeed, weight);
	    			aggregateOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided + averageTripSpeedProvided;
	    			aggregateWeightTripSpeedProvided = aggregateWeightTripSpeedProvided + weight;
	    			//averageTripSpeedProvidedCounter++;
	    		}
		    }
	    }
	    
	    
	    //double averageTime = aggregateTripDuration / tripDurationCounter;
	    double averageTime = aggregateTripDuration / aggregateWeightTripDuration;
	    //double averageTripDistanceRouted = aggregateTripDistanceRouted / tripDistanceRoutedCounter;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / aggregateWeightTripDistanceRouted;
	    //double averageTripDistanceBeeline = aggregateTripDistanceBeeline / tripDistanceBeelineCounter;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / aggregateWeightTripDistanceBeeline;
	    //double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / averageTripSpeedRoutedCounter;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / aggregateWeightTripSpeedRouted;
	    //double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / averageTripSpeedBeelineCounter;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / aggregateWeightTripSpeedBeeline;
	    //double averageOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided / averageTripSpeedProvidedCounter;
	    double averageOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided / aggregateWeightTripSpeedProvided;
	    
	    
	    // write results to files
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    new File(outputDirectory).mkdir();
	    //writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "tripDuration5.txt", binWidthDuration, tripCounter, averageTime);
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "tripDuration.txt", binWidthDuration, aggregateWeightTripDuration, averageTime);
	    //writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "departureTime.txt", binWidthTime, tripCounter, -99);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "departureTime.txt", binWidthTime, aggregateWeightDepartureTime, -99);
	    //writer.writeToFileStringKey(activityTypeMap, outputDirectory + "activityTypes.txt", tripCounter);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "activityTypes.txt", aggregateWeightActivityTypes);
	    //writer.writeToFileStringKey(activityTypePreviousMap, outputDirectory + "activityTypesPrevious.txt", tripCounter);
	    //writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "tripDistanceRouted5.txt", binWidthDistance, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "tripDistanceRouted.txt", binWidthDistance, aggregateWeightTripDistanceRouted, averageTripDistanceRouted);
	    //writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeeline5.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeeline.txt", binWidthDistance, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    //writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "averageTripSpeedRouted5.txt", binWidthSpeed, tripCounter, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "averageTripSpeedRouted.txt", binWidthSpeed, aggregateWeightTripSpeedRouted, averageOfAverageTripSpeedsRouted);
	    //writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeeline5.txt", binWidthSpeed, tripCounter, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeeline.txt", binWidthSpeed, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    //writer.writeToFileIntegerKey(averageTripSpeedProvidedMap, outputDirectory + "averageTripSpeedProvided5.txt", binWidthSpeed, tripCounter, averageOfAverageTripSpeedsProvided);
	    writer.writeToFileIntegerKey(averageTripSpeedProvidedMap, outputDirectory + "averageTripSpeedProvided.txt", binWidthSpeed, aggregateWeightTripSpeedProvided, averageOfAverageTripSpeedsProvided);
	    
	    
	    //----------------------------------------------------------------------------------------------------------------------
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "tripDurationCumulative.txt", binWidthDuration, aggregateWeightTripDuration, averageTime);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeelineCumulative.txt", binWidthDistance, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeelineCumulative.txt", binWidthSpeed, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    //----------------------------------------------------------------------------------------------------------------------
	    
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "beeline.txt", tripCounter);

	    
	    // return number of trips that have no calculable speed
	    System.out.println("Number of trips that have no calculable speed is: " + numberOfTripsWithNoCalculableSpeed);
	}


	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue,
			int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
//		int floorOfLastBin = limitOfLastBin / binWidth;
//		// Math.floor returns next lower integer number (but as a double value)
//		int floorOfValue = (int)Math.floor(inputValueBin);
//		if (floorOfValue < 0) {
//			System.err.println("Lower end of bin may not be smaller than zero!");
//		}
//		
//		if (floorOfValue >= floorOfLastBin) {
//			floorOfValue = floorOfLastBin;
//		}
//		
//		if (!map.containsKey(floorOfValue)) {
//			map.put(floorOfValue, weight);
//		} else {
//			double value = map.get(floorOfValue);
//			value = value + weight;
//			map.put(floorOfValue, value);
//		}
		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			System.err.println("Lower end of bin may not be smaller than zero!");
		}
		
//		if (ceilOfValue >= floorOfLastBin) {
//			ceilOfValue = floorOfLastBin;
//		}
				
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, weight);
		} else {
			double value = map.get(ceilOfValue);
			value = value + weight;
			map.put(ceilOfValue, value);
		}			
	}
	
	
	private static void addToMapStringKey(Map <String, Double> map, String caption, double weight) {
		if (!map.containsKey(caption)) {
			map.put(caption, weight);
		} else {
			double value = map.get(caption);
			value = value + weight;
			map.put(caption, value);
		}
	}
	
}
