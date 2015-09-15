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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

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
		boolean onlyHomeAndWork = false;	//hw
		boolean distanceFilter = true;		//dist
		boolean ageFilter = false;
		
		double minDistance = 0;
		double maxDistance = 100;
		
		Integer minAge = 80;
		Integer maxAge = 119;	
		
		int maxBinDuration = 120;
	    int binWidthDuration = 1;
	    
	    int maxBinTime = 23;
	    int binWidthTime = 1;
	    
	    int maxBinDistance = 60;
	    int binWidthDistance = 1;
	    	    
	    int maxBinSpeed = 60;
	    int binWidthSpeed = 1;
	    
	    
		// Input and output files
		String inputFileTrips = "../../data/srv/input/W2008_Berlin_Weekday.dat";
		String inputFilePersons = "../../data/srv/input/P2008_Berlin2.dat";
		
		String networkFile = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		String shapeFile = "../../data/srv/input/RBS_OD_STG_1412/RBS_OD_STG_1412.shp";
				
		String outputDirectory = "../../data/srv/output/wd_neu";
		
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
				
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}

		outputDirectory = outputDirectory + "/";
		
		
		// parse trip file
		log.info("Parsing " + inputFileTrips + ".");		
		SrVTripParser tripParser = new SrVTripParser();
		tripParser.parse(inputFileTrips);
		log.info("Finished parsing trips.");
		
		// parse person file
		log.info("Parsing " + inputFilePersons + ".");		
		SrVPersonParser personParser = new SrVPersonParser();
		personParser.parse(inputFilePersons);
		log.info("Finished parsing persons.");
		
		
		// create objects
		
		// new ones...
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenario);
		networkReader.parse(networkFile);
		
		String fromCRS = "EPSG:31468"; // GK4
		String toCRS = "EPSG:31468"; // GK4
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
		
		
		
		
		
		// existing ones...
    	int tripCounter = 0;
    	
    	Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	    double aggregateTripDuration = 0.;
	    double aggregateWeightTripDuration = 0.;
	    
	    Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
	    double aggregateWeightDepartureTime = 0.;
	    
	    Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
	    double aggregateWeightActivityTypes = 0.;
		
		Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceRouted = 0.;
		double aggregateWeightTripDistanceRouted = 0.;
		
		Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceBeeline = 0.;
		double aggregateWeightTripDistanceBeeline = 0.;
	    
		Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsRouted = 0.;
	    double aggregateWeightTripSpeedRouted = 0.;

	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    double aggregateWeightTripSpeedBeeline = 0.;
	    
	    Map <Integer, Double> averageTripSpeedProvidedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsProvided = 0.;
	    double aggregateWeightTripSpeedProvided = 0.;

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	    Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
	    
	    
	    // Go through all trips
	    for (Trip trip : tripParser.getTrips().values()) {

	    	// filters
	    	boolean considerTrip = false;

	    	// mode of transport and activity type
	    	// reliant on variable "V_HHPKW_F": 0/1
	    	int useHouseholdCar = trip.getUseHouseholdCar();
	    	// reliant on variable "V_ANDPKW_F": 0/1
	    	int useOtherCar = trip.getUseOtherCar();
	    	// reliant on variable "V_HHPKW_MF": 0/1
	    	int useHouseholdCarPool = trip.getUseHouseholdCarPool();
	    	// reliant on variable "V_ANDPKW_MF": 0/1
	    	int useOtherCarPool = trip.getUseOtherCarPool();

	    	String activityEndActType = trip.getActivityEndActType();
	    	String activityStartActType = trip.getActivityStartActType();

	    	if (onlyHomeAndWork == true) {
	    		if ((activityEndActType.equals("home") && activityStartActType.equals("work")) || 
	    				(activityEndActType.equals("work") && activityStartActType.equals("home"))) {
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


	    	// distance
	    	double tripDistanceBeeline = trip.getDistanceBeeline();
	    	if (distanceFilter == true) {
	    		if (tripDistanceBeeline >= maxDistance) {
	    			considerTrip = false;
	    		}
	    		if (tripDistanceBeeline <= minDistance) {
	    			considerTrip = false;
	    		}
	    	}

	    	
	    	// age
	    	String personId = trip.getPersonId().toString();
	    	
	    	if (ageFilter == true) {
	    		int age = (int) personParser.getPersonAttributes().getAttribute(personId, "age");
	    		if (age < minAge) {
	    			considerTrip = false;
	    		}
	    		if (age > maxAge) {
	    			considerTrip = false;
	    		}
	    	}


	    	// use all filtered trips to construct plans and do calculations 
	    	if (considerTrip == true) {		    		
	    		
	    		// create plans
	    		Id<Person> id = Id.create(personId, Person.class);
	    		
	    		if (!population.getPersons().containsKey(id)) {
	    			Person person = populationFactory.createPerson(id);
	    			Plan plan = populationFactory.createPlan();
	    			person.addPlan(plan);
	    			population.addPerson(person);
	    		}
	    	
	    		Person person = population.getPersons().get(id);
	    		System.out.println(person.getPlans().size());
	    		Plan plan = person.getPlans().get(0);
	    		
	    		// TODO substitute zone by something better
	    		// Id<Zone> departureZoneId = trip.getDepartureZoneId();
	    				    		
	    		// TODO create coordinates
	    		double x = 4590000;
	    		double y = 5820000;
				Coord departureCoordinates = new Coord(x, y);
	    		
	    		// TODO add appropriate coordinate transformation
	    		// TODO give activity proper name
	    		Activity activity = populationFactory.createActivityFromCoord("", ct.transform(departureCoordinates));
	    		double departureTimeInMinutes = trip.getDepartureTime();
	    		activity.setEndTime(departureTimeInMinutes * 60);
	    		plan.addActivity(activity);
	    		
	    		// TODO make mode adjustable
	    		Leg leg = populationFactory.createLeg("car");
	    		plan.addLeg(leg);
	    		
	    		// TODO add last activity
	    		
	    		
	    		
	    		
	    		
	    		// do calculations
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
	    		//double departureTimeInMinutes = trip.getDepartureTime();
	    		double departureTimeInHours = departureTimeInMinutes / 60.;
	    		double tripDurationInMinutes = arrivalTimeInMinutes - departureTimeInMinutes;
	    		//double tripDurationInMinutes = trip.getDuration();
	    		double weightedTripDurationInMinutes = tripDurationInMinutes * weight;
	    		double tripDurationInHours = tripDurationInMinutes / 60.;
	    		// there are also three cases where time < 0; they need to be excluded
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


	    		// In SrV, a routed distance (according to some software) is already given
	    		// reliant on SrV variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
	    		double tripDistanceRouted = trip.getDistanceRoutedShortest();
	    		double weightedTripDistanceRouted = weight * tripDistanceRouted;
	    		if (tripDistanceRouted >= 0.) {
	    			addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance, maxBinDistance, weight);
	    			aggregateTripDistanceRouted = aggregateTripDistanceRouted + weightedTripDistanceRouted;
	    			distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);
	    			aggregateWeightTripDistanceRouted = aggregateWeightTripDistanceRouted + weight;
	    		}


	    		// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
	    		//double tripDistanceBeeline = trip.getDistanceBeeline();
	    		double weightedTripDistanceBeeline = weight * tripDistanceBeeline;
	    		if (tripDistanceBeeline >= 0.) {				
	    			addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance, maxBinDistance, weight);
	    			aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + weightedTripDistanceBeeline;
	    			distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);
	    			aggregateWeightTripDistanceBeeline = aggregateWeightTripDistanceBeeline + weight;
	    		}


	    		// calculate speeds and and store them in a map
	    		if (tripDurationInHours > 0.) {
	    			// reliant to SrV variable variable "E_LAENGE_KUERZEST"; -7 = calculation not possible
	    			if (tripDistanceRouted >= 0.) {
	    				double averageTripSpeedRouted = tripDistanceRouted / tripDurationInHours;
	    				addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed, maxBinSpeed, weight);
	    				aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;
	    				aggregateWeightTripSpeedRouted = aggregateWeightTripSpeedRouted + weight;
	    			}

	    			// reliant on variable "V_LAENGE": -9 = no data, -10 = implausible
	    			if (tripDistanceBeeline >= 0.) {			
	    				double averageTripSpeedBeeline = tripDistanceBeeline / tripDurationInHours;
	    				addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed, maxBinSpeed, weight);
	    				aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;
	    				aggregateWeightTripSpeedBeeline = aggregateWeightTripSpeedBeeline + weight;
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
	    		}
	    	}
	    }
	    
	    
	    // calculate averages (taking into account weights if applicable)
	    double averageTime = aggregateTripDuration / aggregateWeightTripDuration;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / aggregateWeightTripDistanceRouted;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / aggregateWeightTripDistanceBeeline;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / aggregateWeightTripSpeedRouted;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / aggregateWeightTripSpeedBeeline;
	    double averageOfAverageTripSpeedsProvided = aggregateOfAverageTripSpeedsProvided / aggregateWeightTripSpeedProvided;
	    
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "tripDuration.txt", binWidthDuration, aggregateWeightTripDuration, averageTime);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "departureTime.txt", binWidthTime, aggregateWeightDepartureTime, -99);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "activityTypes.txt", aggregateWeightActivityTypes);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "tripDistanceRouted.txt", binWidthDistance, aggregateWeightTripDistanceRouted, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeeline.txt", binWidthDistance, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "averageTripSpeedRouted.txt", binWidthSpeed, aggregateWeightTripSpeedRouted, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeeline.txt", binWidthSpeed, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedProvidedMap, outputDirectory + "averageTripSpeedProvided.txt", binWidthSpeed, aggregateWeightTripSpeedProvided, averageOfAverageTripSpeedsProvided);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "tripDurationCumulative.txt", binWidthDuration, aggregateWeightTripDuration, averageTime);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "tripDistanceBeelineCumulative.txt", binWidthDistance, aggregateWeightTripDistanceBeeline, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "averageTripSpeedBeelineCumulative.txt", binWidthSpeed, aggregateWeightTripSpeedBeeline, averageOfAverageTripSpeedsBeeline);
//	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "beeline.txt", tripCounter);

	    
	    // return number of trips that have no calculable speed
	    log.warn("Number of trips that have no calculable speed is: " + numberOfTripsWithNoCalculableSpeed);
	    
	    
	    // add final activity to plans
	    for (Person person : population.getPersons().values()) {
	    	Plan plan = person.getPlans().get(0);
	    	Activity firstActivity = (Activity) plan.getPlanElements().get(0);
	    	Coord homeCoord = firstActivity.getCoord();
	    	Activity lastActivity = populationFactory.createActivityFromCoord("", ct.transform(homeCoord));
	    	plan.addActivity(lastActivity);	    	
	    }	    
	    
	    // write population
	    MatsimWriter popWriter = new PopulationWriter(population, scenario.getNetwork());
	    popWriter.write(outputDirectory + "plans.xml");

	}


	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
		int ceilOfLastBin = limitOfLastBin / binWidth;		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			System.err.println("Lower end of bin may not be smaller than zero!");
		}
				
		if (ceilOfValue >= ceilOfLastBin) {
			ceilOfValue = ceilOfLastBin;
		}
						
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
