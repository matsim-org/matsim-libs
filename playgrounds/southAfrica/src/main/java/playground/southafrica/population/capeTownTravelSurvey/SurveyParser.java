/* *********************************************************************** *
 * SurveyParser.java
 * project: org.matsim.*
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
package playground.southafrica.population.capeTownTravelSurvey;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.vividsolutions.jts.geom.Point;

import playground.southafrica.population.capeTownTravelSurvey.HouseholdEnums.MonthlyIncome;
import playground.southafrica.population.capeTownTravelSurvey.PersonEnums.Disability;
import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;
import playground.southafrica.utilities.grid.GeneralGrid;

/**
 * Class to parse the different elements/files from the travel survey for the
 * City of Cape Town of 2012.
 * 
 * @author jwjoubert
 */
public class SurveyParser {
	final private static Logger LOG = Logger.getLogger(SurveyParser.class);
	private static int numberOfUnknownActivityLocations = 0;
	private static int numberOfFixableActivityLocations = 0;
	private Scenario sc;
	private Map<Id<Person>, List<String[]>> tripMap;
	
	private Map<String,MyZone> zoneMap;
	private Map<String, TreeMap<String, Double>> propabilityMap;
	
	/* Density grids. */
	Map<String,GeneralGrid> gridMap;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SurveyParser.class.toString(), args);
		
		SurveyParser sp = new SurveyParser(args[0], Integer.parseInt(args[1]));
		sp.parsePersons(args[2]);
		sp.parseHouseholds(args[3]);
		sp.parseDerivedHouseholdAssets(args[4]);
		sp.parseDiaryTrips(args[5]);
		
		sp.writePopulation(args[6], sp.getScenario());
		
		Scenario surveySc = sp.filterScenarioToSurveyRespondents();
		sp.cleanUpScenario(surveySc);
		sp.writePopulation(args[7], surveySc);
		
		
		
		/* Finish off with some basic population statistics. */
		LOG.info("---------------------------------------");
		LOG.info(" Number of households: " + surveySc.getHouseholds().getHouseholds().size());
		LOG.info("    Number of persons: " + surveySc.getPopulation().getPersons().size());
		LOG.info("---------------------------------------");
		PopulationUtils.printActivityStatistics(args[7] + (args[7].endsWith("/") ? "" : "/") + "population.xml");
		
		/* Some arbitrary checks. Remove once done. */
		LOG.info("              Number of observed zones: " + sp.zoneMap.size());
		LOG.info("  Number of unknown activity locations: " + numberOfUnknownActivityLocations);
		LOG.info("Number of (fixable) activity locations: " + numberOfFixableActivityLocations);
		
		Header.printFooter();
	}

	public SurveyParser(String shapefile, int idField) {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.tripMap = new HashMap<>();
		this.zoneMap = new HashMap<>();
		this.gridMap = new HashMap<>();
		this.propabilityMap = new TreeMap<>();
		
		/* Parse the transport zone shapefile, and add each zone to the Map. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, idField);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read transport zones from " + shapefile);
		}
		for(MyZone zone : mfr.getAllZones()){
			zoneMap.put(zone.getId().toString(), zone);
		}
	}
	
	
	/**
	 * Parsing the persons file of the survey. Unless a numerical value is 
	 * parsed, the codes from {@link PersonEnums} are used for the different
	 * categories. 
	 * 
	 * @param sc
	 * @param filename
	 * @return
	 */
	public void parsePersons(String filename){
		LOG.info("Parsing persons from " + filename);
		int counter = 0;
		
		ObjectAttributes pa = sc.getPopulation().getPersonAttributes();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				counter++;
				String[] sa = line.split(",");
				if(sa.length == 65){
					/* Create the person. */
					PopulationFactory pf = sc.getPopulation().getFactory();
					Id<Person> id = Id.createPersonId(sa[1] + "_" + sa[2]);
					Person person = pf.createPerson(id);
					
					/* All all the person attributes. */
					pa.putAttribute(id.toString(), 
							"yearOfBirth", 
							sa[3].equals("") ? "Unknown" : sa[3]);
					pa.putAttribute(id.toString(), 
							"gender", 
							PersonEnums.Gender.parseFromCode(sa[4].equals("") ? 0 : 
								Integer.parseInt(sa[4])).getDescription());
					pa.putAttribute(id.toString(), 
							"education",
							PersonEnums.Education.parseFromCode(sa[5].equals("") ? 0 : 
								Integer.parseInt(sa[5])).getDescription());
					
					Disability d1 = PersonEnums.Disability.parseFromCode(sa[6].equals("") ? 0 : 
						Integer.parseInt(sa[6]));
					if(!d1.equals(Disability.NONE)){
						pa.putAttribute(id.toString(), "disability1", d1.getDescription());
					}
					
					Disability d2 = PersonEnums.Disability.parseFromCode(sa[7].equals("") ? 0 : 
						Integer.parseInt(sa[7]));
					if(!d2.equals(Disability.NONE)){
						pa.putAttribute(id.toString(), "disability2", d2.getDescription());
					}
					
					Disability d3 = PersonEnums.Disability.parseFromCode(sa[8].equals("") ? 0 : 
						Integer.parseInt(sa[8]));
					if(!d3.equals(Disability.NONE)){
						pa.putAttribute(id.toString(), "disability3", d3.getDescription());
					}

					pa.putAttribute(id.toString(), 
							"license_car",
							PersonEnums.LicenseCar.parseFromCode(sa[9].equals("") ? 0 : 
								Integer.parseInt(sa[9])).getDescription());
					pa.putAttribute(id.toString(), 
							"license_motorcycle",
							PersonEnums.LicenseMotorcycle.parseFromCode(sa[10].equals("") ? 0 : 
								Integer.parseInt(sa[10])).getDescription());
					pa.putAttribute(id.toString(), 
							"license_heavyVehicle",
							PersonEnums.LicenseHeavyVehicle.parseFromCode(sa[11].equals("") ? 0 : 
								Integer.parseInt(sa[11])).getDescription());
					
					pa.putAttribute(id.toString(), 
							"employment",
							PersonEnums.Employment.parseFromCode(sa[12].equals("") ? 0 : 
								Integer.parseInt(sa[12])).getDescription());
					pa.putAttribute(id.toString(), 
							"travelToPrimary",
							PersonEnums.TravelToPrimary.parseFromCode(sa[13].equals("") ? 0 : 
								Integer.parseInt(sa[13])).getDescription());
					pa.putAttribute(id.toString(), 
							"workFromHome",
							PersonEnums.WorkFromHome.parseFromCode(sa[14].equals("") ? 0 : 
								Integer.parseInt(sa[14])).getDescription());
					pa.putAttribute(id.toString(), 
							"travelForWork",
							PersonEnums.TravelForWork.parseFromCode(sa[15].equals("") ? 0 : 
								Integer.parseInt(sa[15])).getDescription());
					
					/*TODO We can add more of the variable later, should we wish. */
					
					sc.getPopulation().addPerson(person);
				} else{
					LOG.warn("   person: " + counter + " ==> Line with " + sa.length + " elements.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Total number of persons parsed: " + sc.getPopulation().getPersons().size());
		LOG.info("Done parsing persons.");
	}
	
	
	public void parseHouseholds(String filename){
		LOG.info("Parsing households from " + filename);
		int counter = 0;
		
		ObjectAttributes hha = sc.getHouseholds().getHouseholdAttributes();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				counter++;
				String[] sa = line.split(",");
				if(sa.length >= 33){
					/* Create the household. */
					HouseholdsFactory hf = sc.getHouseholds().getFactory();
					Id<Household> id = Id.create(sa[1], Household.class);
					Household hh = hf.createHousehold(id);
					
					MonthlyIncome mi = HouseholdEnums.MonthlyIncome.parseFromCode(Integer.parseInt(sa[32]));
					Income matsimIncome = mi.getMatsimIncome();
					if(matsimIncome != null){
						hh.setIncome(matsimIncome);
					}
					
					/* Add all the household attributes. */
					hha.putAttribute(id.toString(), "transportZone", sa[7]);
					hha.putAttribute(id.toString(), "enumerationArea", sa[9]);
					hha.putAttribute(id.toString(), "income", mi.getDescription());

					hha.putAttribute(id.toString(), 
							"completedDiary",
							HouseholdEnums.CompletedDiary.parseFromCode(sa[12].equals("") ? 0 : 
								Integer.parseInt(sa[12])).getDescription());
					hha.putAttribute(id.toString(), 
							"completedStatedPreference",
							HouseholdEnums.CompletedStatedPreference.parseFromCode(sa[13].equals("") ? 0 : 
								Integer.parseInt(sa[13])).getDescription());

					hha.putAttribute(id.toString(), 
							"dwellingType",
							HouseholdEnums.DwellingType.parseFromCode(sa[18].equals("") ? 0 : 
								Integer.parseInt(sa[18])).getDescription());

					hha.putAttribute(id.toString(), "householdSize", Integer.parseInt(sa[19]));
					hha.putAttribute(id.toString(), "numberOfEmployedPeople", Integer.parseInt(sa[20]));
					hha.putAttribute(id.toString(), "numberOfHouseholdCarsOwned", Integer.parseInt(sa[21]));
					hha.putAttribute(id.toString(), "numberOfHouseholdCarsAccessTo", Integer.parseInt(sa[22]));
					hha.putAttribute(id.toString(), "numberOfHouseholdMotorcyclesOwned", Integer.parseInt(sa[23]));
					hha.putAttribute(id.toString(), "numberOfHouseholdMotorcyclesAccessTo", Integer.parseInt(sa[24]));
					hha.putAttribute(id.toString(), "numberOfDomesticWorkers", Integer.parseInt(sa[25]));
					hha.putAttribute(id.toString(), "numberOfGardenWorkers", Integer.parseInt(sa[28]));

					
					sc.getHouseholds().getHouseholds().put(id, hh);
				} else{
					LOG.warn("   person: " + counter + " ==> Line with " + sa.length + " elements.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		LOG.info("Total number of households parsed: " + sc.getHouseholds().getHouseholds().size());
		LOG.info("Done parsing households.");
	}
	
	
	public void parseDerivedHouseholdAssets(String filename){
		LOG.info("Parsing derived household asset classes from " + filename);
		int counter = 0;
		
		ObjectAttributes hha = sc.getHouseholds().getHouseholdAttributes();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				counter++;
				String[] sa = line.split(",");
				if(sa.length == 10){
					/* Get the household. */
					Id<Household> id = Id.create(sa[0], Household.class);
					if(!sc.getHouseholds().getHouseholds().containsKey(id)){
						LOG.error("Cannot find household " + id.toString() + " to add asset value.");
					}
					
					hha.putAttribute(id.toString(), "assetClassMethod1", 
							HouseholdEnums.AssetClass1.parseFromCode(Integer.parseInt(sa[7])).getDescription());
					hha.putAttribute(id.toString(), "assetClassMethod2", 
							HouseholdEnums.AssetClass2.parseFromCode(Integer.parseInt(sa[9])).getDescription());
				} else{
					LOG.warn("   household: " + counter + " ==> Line with " + sa.length + " elements.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		LOG.info("Total number of households parsed: " + counter);
		LOG.info("Done parsing derived household asset classes.");
	}
	
	
	
	public void parseDiaryTrips(String filename){
		LOG.info("Parsing travel diary from " + filename);
		int counter = 0;
		
		/* For debugging/cleaning purposes. */
		int tripsSkipped = 0;
		int tripsProcessed = 0;
		int noPersonId = 0;
		List<Id<Household>> listOfMissingHouseholds = new ArrayList<>();
		List<Id<Person>> listOfMissingPersons = new ArrayList<>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				counter++;
				String[] sa = line.split(",");
				if(sa.length == 42){
					/* Get the household. */
					Id<Household> hhId = Id.create(sa[1], Household.class);
					if(!sc.getHouseholds().getHouseholds().containsKey(hhId)){
						if(!listOfMissingHouseholds.contains(hhId)){
							listOfMissingHouseholds.add(hhId);
							LOG.warn("Cannot find household " + hhId.toString() + " to add trip. Household ignored");
						}
						tripsSkipped++;
					} else{
						/* Carry on... */

						/* Get the person. */
						if(sa[12].equals("")){noPersonId++;}; /*TODO Remove after debug. */
						
						Id<Person> pId = Id.createPersonId(hhId.toString() + "_" + sa[12]);
						if(!sc.getPopulation().getPersons().containsKey(pId)){
							if(!listOfMissingPersons.contains(pId)){
								listOfMissingPersons.add(pId);
								LOG.warn("Cannot find person " + pId.toString() + " to add trip. Person ignored.");
							}
							tripsSkipped++;
						} else{
							/* Parse the trip data. Still not sure if the trip 
							 * is going to be useful or not, but just keep the 
							 * data in the meanwhile. */
							
							if(!tripMap.containsKey(pId)){
								tripMap.put(pId, new ArrayList<String[]>());
							}
							List<String[]> list = tripMap.get(pId);
							list.add(sa);
							tripMap.put(pId, list);
							
							tripsProcessed++;
						}
					}
				} else{
					LOG.warn("   trip: " + counter + " ==> Line with " + sa.length + " elements.");
					LOG.error(line);
					throw new RuntimeException("terminating...");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		
		LOG.warn("--------------------------------------------");
		LOG.warn("      Number of skipped trips: " + tripsSkipped);
		LOG.warn(" Number of missing households: " + listOfMissingHouseholds.size());
		LOG.warn("    Number of missing persons: " + listOfMissingPersons.size());
		LOG.warn("    Number of empty person Id: " + noPersonId);
		LOG.warn("    Number of processed trips: " + tripsProcessed);
		LOG.warn("--------------------------------------------");
		
		LOG.info(" Number of persons with trips: " + tripMap.size());
		Map<String, Integer> countMap = new TreeMap<String, Integer>();
		for(Id<Person> id : tripMap.keySet()){
			List<String[]> list = tripMap.get(id);
			Integer size = list.size();
			String sizeKey = String.format("%02d", size);
			if(countMap.containsKey(sizeKey)){
				int oldCount = countMap.get(sizeKey);
				countMap.put(sizeKey, oldCount+1);
			} else{
				countMap.put(sizeKey, 1);
			}
		}
		for(String s : countMap.keySet()){
			LOG.info("    " + s + ": " + countMap.get(s));
		}
		LOG.info("--------------------------------------------");
		
		LOG.info("Total number of trips parsed: " + counter);
		LOG.info("Done parsing travel diary.");
		
		processDiary();
	}
	
	private void processDiary(){
		LOG.info("Processing diary...");
		PopulationFactory pf = this.sc.getPopulation().getFactory();
		
		Map<String, Integer> chainCount = new TreeMap<>();
		
		int noTrips = 0;
		int noFirstTrip = 0;
		int noZoneInfo = 0;
		int zoneInfo = 0;
		
		Counter counter = new Counter("  person # ");
		for(Id<Person> pId : this.tripMap.keySet()){
			List<String[]> list = tripMap.get(pId);
			Map<String,String[]> map = new TreeMap<>();
			for(String[] sa : list){
				String tripNumber = sa[23];
				if(!tripNumber.equals("")){
					String tripId = String.format("%02d", Integer.parseInt(tripNumber));
					map.put(tripId, sa);
				}
			}
			
			if(map.size() == 0){
				noTrips++;
			} else{
				/* Try and do some magic with the diary. */
				Plan plan = pf.createPlan();
				
				/* Check the first activity. */
				String chain = "";
				String[] sa = map.get("01");
				if(sa == null){
					/* There is no first trip numbered '01'. */
					LOG.warn(pId.toString() + ": " + map.keySet().toString());
					noFirstTrip++;
				} else {
					chain += "h";
					String homeZone = sa[7];
					Coord homeCoord = sampleCoord(homeZone, "h");
					
					Activity firstHome = pf.createActivityFromCoord("h", homeCoord);
					double homeEnd = 0.0;
					try{
						homeEnd = Time.parseTime(sa[25]);
					} catch (NumberFormatException e){
						LOG.error(" TIME: ===> " + pId.toString() + ": " + sa[25]);
					}
					firstHome.setEndTime(homeEnd);
					plan.addActivity(firstHome);
				}
				
				/* Parse the chain. */
				Iterator<String> it = map.keySet().iterator();
				
				while(it.hasNext()){
					String trip = it.next();
					String[] tripSa = map.get(trip);
					String tripPurpose = tripSa[33];
					String matsimTrip = DiaryEnums.TripPurpose.parseFromCode(
							tripPurpose.equals("") || tripPurpose.equals(" ") ? 
									0 : Integer.parseInt(tripPurpose)).getMatsimActivityCode();
					chain += "-" + matsimTrip;
					
					/* Add the trip to plan. */
					String recordedMode = tripSa[34];
					if(recordedMode.contains(";")){
						LOG.error("Multiple modes for " + pId.toString() + ": " + recordedMode);
						throw new RuntimeException();
					}
					String matsimMode = DiaryEnums.ModeOfTravel.parseFromCode(
							tripSa[34].equals("") || tripSa[34].equalsIgnoreCase(" ") ? 0 : 
								Integer.parseInt(tripSa[34])).getMatsimMode();
					Leg leg = pf.createLeg(matsimMode);
					double tripStartTime = 0.0;
					double tripEndTime = 0.0;
					try{
						tripStartTime = Time.parseTime(tripSa[25]);
					} catch (NumberFormatException e){
						LOG.error(" TIME: ===> " + pId.toString() + ": " + tripSa[25]);
					}
					try{
						tripEndTime = Time.parseTime(tripSa[27]);
					} catch (NumberFormatException e){
						LOG.error(" TIME: ===> " + pId.toString() + ": " + tripSa[27]);
					}
					leg.setDepartureTime(tripStartTime);
					leg.setTravelTime(tripEndTime - tripStartTime);
					plan.addLeg(leg);
					
					/* Add the destination activity. */
					if(tripSa[32].equals("") || tripSa[32].equals(" ")){
						if(!matsimTrip.equalsIgnoreCase("h")){
							zoneInfo++;
						} else{
							noZoneInfo++;
						}
					} else{
						zoneInfo++;
					}
					Coord actCoord = sampleCoord(tripSa[32], matsimTrip);
					Activity act = pf.createActivityFromCoord(matsimTrip, actCoord);
					act.setStartTime(tripEndTime);
					plan.addActivity(act);
				}
				
				/* Check and add chain. */
				if(!chainCount.containsKey(chain)){
					chainCount.put(chain, 1);
				} else{
					int oldCount = chainCount.get(chain);
					chainCount.put(chain, oldCount+1);
				}
				
				/* Finally, associate the plan with the person. */
				sc.getPopulation().getPersons().get(pId).addPlan(plan);
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("          Number of persons with no trips: " + noTrips);
		LOG.info("    Number of persons with no first trips: " + noFirstTrip);
		LOG.info(" Number of destinations without zone info: " + noZoneInfo);
		LOG.info("    Number of destinations with zone info: " + zoneInfo);
		
		/* Report the activity chain types. */
		SortedSet<Entry<String, Integer>> set = entriesSortedByValues(chainCount);
		Iterator<Entry<String, Integer>> iter = set.iterator();
		while(iter.hasNext()){
			Entry<String, Integer> entry = iter.next();
			LOG.info("  " + entry.getKey() + " (" + entry.getValue() + ")");
		}
		
		LOG.info("Done processing diary.");
	}
	
	
	/**
	 * Randomly samples a coordinate from within a zone.
	 * 
	 * TODO This must still be implemented once we have a shapefile for the
	 * transport zones (TZs) for City of Cape Town.
	 * 
	 * @param zone
	 * @return
	 */
	private Coord sampleCoord(String zone, String activityType){

		MyZone mz = null;
		if(zone.equals("") || zone.equals(" ")){
			/* Do nothing; don't try and get a zone. */
		} else{
			mz = zoneMap.get(zone);
			if(mz == null){
				LOG.error("Cannot find zone " + zone + " in Map.");
			}
		}
		
		/* Update the map indicating how many of each activity occurs in 
		 * each zone. */
		if(mz != null){
			ObjectAttributes oa = mz.getObjectAttributes();
			Object o = oa.getAttribute(mz.getId().toString(), activityType);
			if(o == null){
				oa.putAttribute(mz.getId().toString(), activityType, 1);
			} else{
				if(o instanceof Integer){
					int oldValue = (int)o;
					oa.putAttribute(mz.getId().toString(), activityType, oldValue+1);
				} else{
					LOG.error("The activity count for zone " + mz.getId().toString() 
							+ "'s activity type '" + activityType + " should of type Integer, but is " 
							+ o.getClass().toString());
				}
			}
		}
		
		double x = 0.0;
		double y = 0.0;
		if(mz != null){
			Point p = mz.sampleRandomInteriorPoint();
			x = p.getX();
			y = p.getY();
		} else{
			numberOfUnknownActivityLocations++;
		}
		Coord c = CoordUtils.createCoord(x, y);
		return c;
	}
	
	/**
	 * Code adapted from http://stackoverflow.com/questions/2864840/treemap-sort-by-value
	 * to sort the activity chain map based on number of observations.
	 */
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                    int res = e2.getValue().compareTo(e1.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }	
	
	
	public void writePopulation(String folder, Scenario scenario){
		folder = folder + (folder.endsWith("/") ? "" : "/");
		LOG.info("Writing population to " + folder);
		
		new PopulationWriter(scenario.getPopulation()).write(folder + "population.xml");
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(folder + "populationAttributes.xml");
		
		new HouseholdsWriterV10(scenario.getHouseholds()).writeFile(folder + "households.xml");
		new ObjectAttributesXmlWriter(scenario.getHouseholds().getHouseholdAttributes()).writeFile(folder + "householdAttributes.xml");
		
		LOG.info("Done writing population.");
	}
	
	private Coord sampleActivityLocation(String type){
		Coord c = null;
		if(!this.propabilityMap.containsKey(type)){
			/* First build the cumulative probability map. */
			LOG.info("Building a cumulative probability map for activity type '" + type + "'");
			
			/* a. Get the total number of activity observations. */
			int total = 0;
			for(MyZone mz : this.zoneMap.values()){
				Object o = mz.getObjectAttributes().getAttribute(mz.getId().toString(), type);
				if(o != null && o instanceof Integer){
					total += (int)o;
				}
			}
			
			/* b. Calculate the cumulative probability for each zone. */
			TreeMap<String, Double> map = new TreeMap<>();
			int cumsum = 0;
			for(String zoneId : this.zoneMap.keySet()){
				MyZone zone = this.zoneMap.get(zoneId);
				Object o = zone.getObjectAttributes().getAttribute(zoneId, type);
				if(o != null && o instanceof Integer){
					cumsum += (int)o;
					double cumprob = ((double) cumsum) / ((double) total);
					map.put(zoneId, cumprob);
				}
			}
			this.propabilityMap.put(type, map);
		}
		
		/* Sample from the cumulative probabilities. */
		double r = MatsimRandom.getLocalInstance().nextDouble();
		String sampledZone = null;
		TreeMap<String, Double> theZoneMap = this.propabilityMap.get(type);
		Iterator<String> iterator = theZoneMap.navigableKeySet().iterator();
		while(sampledZone == null && iterator.hasNext()){
			String zoneId = iterator.next();
			double d = theZoneMap.get(zoneId);
			if(r <= d){
				sampledZone = zoneId;
			}
		}
		if(sampledZone == null){
			LOG.error("Should never NOT find a sampled zone.");
		}
		
		Point p = this.zoneMap.get(sampledZone).sampleRandomInteriorPoint();
		c = CoordUtils.createCoord(p.getX(), p.getY());
		
		return c;
	}

	
	public void cleanUpScenario(Scenario sc){
		LOG.info("Cleaning up scenario...");
		/* TODO Still need to figure out what cleaning up must happen. */
		
		
		/* Search for location-less activities, and sample its locations from 
		 * the kernel density estimates. */
		LOG.info("Sampling locations for those without known zones...");
		int locationsFixed = 0;
		int knownLocations = 0;
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			if(plan != null){
				for(PlanElement pe : plan.getPlanElements()){
					if(pe instanceof Activity){
						Activity act = (Activity)pe;
						Coord coord = act.getCoord();
						if(coord.getX() == 0.0 || coord.getY() == 0.0){
							((ActivityImpl)act).setCoord(sampleActivityLocation(act.getType()));
							locationsFixed++;
						} else{
							knownLocations++;
						}
					}
				}
			}
		}
		LOG.info("Number of known locations: " + knownLocations);
		LOG.info("Number of locations fixed: " + locationsFixed);
		LOG.info("Done sampling locations.");
		
		/* Ensure each activity, except the last, has both a start and end time. */
		LOG.info("Ensure each activity has an end time...");
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			if(plan != null){
				List<PlanElement> list = plan.getPlanElements();
				for(int i = 0; i < list.size()-2; i+=2){
					PlanElement pe = list.get(i);
					if(pe instanceof Activity){
						Activity act = (Activity)pe;
						double endTime = act.getEndTime();
						if(endTime < 0.0){
							double legStart = ((Leg)list.get(i+1)).getDepartureTime();
							act.setEndTime(legStart);
						}
					} else{
						LOG.warn("Every second PlanElement should be of type 'Activity'.");
					}
				}
			}
		}
		LOG.info("Done fixing activity end times.");
		
		
		/* Ensure that the home location/coordinate for all members in the 
		 * household are the same for all their "h" activities. */
		LOG.info("Fix home locations for each household member. ");
		int homeLocationsFixed = 0;
		int nonTravellers = 0;
		for(Household hh : sc.getHouseholds().getHouseholds().values()){
			Object o = sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "transportZone");
			if(o instanceof String){
				String zoneId = (String)o;
				Coord homeCoord = null;
				if(zoneId != null && !zoneId.equalsIgnoreCase("")){
					/* There is known home zone. */
					Point p = this.zoneMap.get(zoneId).sampleRandomInteriorPoint();
					homeCoord = CoordUtils.createCoord(p.getX(), p.getY());
				} else{
					/* There is no transport zone. */
					homeCoord = sampleActivityLocation("h");
				}
				
				/* Now assign the home coordinate to ALL home activities for
				 * all members of the household. */
				for(Id<Person> id : hh.getMemberIds()){
					Person person = sc.getPopulation().getPersons().get(id);
					Plan plan = person.getSelectedPlan();
					if(plan != null){
						for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
							if(pe instanceof ActivityImpl){
								ActivityImpl act = (ActivityImpl)pe;
								if(act.getType().equalsIgnoreCase("h")){
									act.setCoord(homeCoord);
									homeLocationsFixed++;
								}
							}
						}
					} else{
						/* The member does not have ANY plan. We 'fix' this by
						 * adding aPlan with a single home-based activity for
						 * which not times are specified. */
						Plan stayHomePlan = sc.getPopulation().getFactory().createPlan();
						Activity act = sc.getPopulation().getFactory().createActivityFromCoord("h", homeCoord);
						stayHomePlan.addActivity(act);
						person.addPlan(stayHomePlan);
						nonTravellers++;
					}
				}
			}
		}
		LOG.info("Total number of home locations fixed: " + homeLocationsFixed);
		LOG.info("      Total number of non-travellers: " + nonTravellers );
		LOG.info("Done fixing home locations.");
		
		/* TODO Check what to do with those household members that are not
		 * travelling. Are they just given a single 'h' activities, and 
		 * left at that? */
		
		/* Look for erroneous activity times that can/should be fixed in
		 * the raw travel diary input data. This will typically include very
		 * long activity times or leg durations. */
		LOG.info("Checking leg durations:");
		int remainingLegOddities = 0;
		double legDurationThreshold = Time.parseTime("03:00:00");
		for(Person p : sc.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			if(plan != null){
				for(PlanElement pe : plan.getPlanElements()){
					if(pe instanceof Leg){
						double duration = ((Leg)pe).getTravelTime();
						if(duration < 0 || duration > legDurationThreshold){
							LOG.warn(" -> Odd leg duration: " + p.getId().toString() + " (" + Time.writeTime(duration) + ")");
							remainingLegOddities++;
						}
					}
				}
			}
		}
		LOG.info("Done checking leg durations. (" + remainingLegOddities + " remain)");
		
		/* Parse the activity duration times in an effort to pick up
		 * erroneous travel time data that results in negative activity 
		 * durations. Then fix in input data. */
		LOG.info("Checking activity durations (from leg times):");
		int remainingActivityOddities = 0;
		double activityDurationThreshold = Time.parseTime("16:00:00");
		for(Person p : sc.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			if(plan != null){
				for(int i = 1; i < plan.getPlanElements().size()-2; i+=2){
					PlanElement pe1 = plan.getPlanElements().get(i);
					PlanElement pe2 = plan.getPlanElements().get(i+2);
					if(pe1 instanceof Leg && pe2 instanceof Leg){
						Leg l1 = (Leg)pe1;
						Leg l2 = (Leg)pe2;
						double act = l2.getDepartureTime() - (l1.getDepartureTime() + l1.getTravelTime());
						if(act < 0 || act > activityDurationThreshold){
							LOG.warn(" -> Odd activity duration: " + p.getId().toString() + " (" + Time.writeTime(act) + ")");
							remainingActivityOddities++;
						}
					} else{
						LOG.error("PlanElements not of type Leg!!");
						LOG.error("   pe1: " + pe1.getClass().toString());
						LOG.error("   pe2: " + pe2.getClass().toString());
					}
				}
			}
		}
		LOG.info("Done checking activity durations. (" + remainingActivityOddities + " remain)");
		
		/* TODO Fix plans for night workers. They typically start with 'h', 
		 * but should actually start with 'w'. */
		
		
		/* TODO Consider what to do with repeating activities, especially
		 * consecutive home activities. */
		
		/* Convert all activity locations to a projected coordinate system. */
		LOG.info("Converting all activity locations to EPSG:3857...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "EPSG:3857");
		for(Person person : sc.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			if(plan != null){
				for(PlanElement pe : plan.getPlanElements()){
					if(pe instanceof Activity){
						Activity act = (Activity)pe;
						((ActivityImpl)act).setCoord(ct.transform(act.getCoord()));
					}
				}
			}
		}
		LOG.info("Done converting activity locations.");
		
		LOG.info("Done cleaning up scenario.");
	}
	
	
	public Scenario filterScenarioToSurveyRespondents(){
		LOG.info("Filtering scenario to households having completed the diary...");
		Scenario newSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int membersAccountedFor = 0;
		int membersUnaccountedFor = 0;
	
		for(Id<Household> hhId : sc.getHouseholds().getHouseholds().keySet()){
			/* Check if household completed the travel diary. */
			Object check = this.sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), "completedDiary");
			if(check instanceof String){
				String sCheck = (String)check;
				if(sCheck.equalsIgnoreCase("Yes")){
					/* Copy the household to the new scenario. */
					newSc.getHouseholds().getHouseholds().put(hhId, this.sc.getHouseholds().getHouseholds().get(hhId));
					
					/* Copy the household attributes to the new households. */
					for(String attr : getHouseholdAttributes()){
						Object o = this.sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), attr);
						newSc.getHouseholds().getHouseholdAttributes().putAttribute(hhId.toString(), attr, o);
					}
					
					/* Get the reported number of household members. */
					Object oMembers = this.sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId.toString(), "householdSize");
					int members = 0;
					if(oMembers instanceof Integer){
						members = (Integer)oMembers;
					}
					
					/* Check for, and copy all the household members. */
					for(int i = 1; i <= members; i++){
						Id<Person> pId = Id.createPersonId(hhId.toString() + "_" + i);
						if(!this.sc.getPopulation().getPersons().containsKey(pId)){
							LOG.warn("Household " + hhId.toString() + " reported " + members + " members; cannot find " + pId.toString());
							membersUnaccountedFor++;
						} else{
							/* Link the person to the household. */
							newSc.getHouseholds().getHouseholds().get(hhId).getMemberIds().add(pId);
							
							/* Copy the person, whether they have a plan or not. */
							newSc.getPopulation().addPerson( this.sc.getPopulation().getPersons().get(pId) );
							
							/* Copy all the person's attributes. */
							for(String attr : getPersonAttributes()){
								Object o = this.sc.getPopulation().getPersonAttributes().getAttribute(pId.toString(), attr);
								newSc.getPopulation().getPersonAttributes().putAttribute(pId.toString(), attr, o);
							}
							
							membersAccountedFor++;
						}
					}
				} else{
					/* Ignore the household. */
				}
			}
		}
		LOG.info("Done filtering scenario.");
		LOG.warn("Number of household members accounted for: " + membersAccountedFor);
		LOG.warn("Number of household members unaccounted for: " + membersUnaccountedFor);
		
		/* Check the number of members with and without plans. */
		int withPlans = 0;
		int withoutPlans = 0;
		for(Person p : newSc.getPopulation().getPersons().values()){
			if(p.getPlans().size() == 0){
				withoutPlans++;
			} else{
				withPlans++;
			}
		}
		LOG.info("   Number of persons with plans: " + withoutPlans);
		LOG.info("Number of persons without plans: " + withPlans);
		
		return newSc;
	}
	
	static List<String> getPersonAttributes(){
		List<String> list = new ArrayList<>();
		list.add("education");
		list.add("employment");
		list.add("gender");
		list.add("license_car");
		list.add("license_heavyVehicle");
		list.add("license_motorcycle");
		list.add("travelForWork");
		list.add("travelToPrimary");
		list.add("workFromHome");
		list.add("yearOfBirth");
		return list;
	}

	static List<String> getHouseholdAttributes(){
		List<String> list = new ArrayList<>();
		list.add("assetClassMethod1");
		list.add("assetClassMethod2");
		list.add("completedDiary");
		list.add("completedStatedPreference");
		list.add("dwellingType");
		list.add("enumerationArea");
		list.add("householdSize");
		list.add("income");
		list.add("numberOfDomesticWorkers");
		list.add("numberOfEmployedPeople");
		list.add("numberOfGardenWorkers");
		list.add("numberOfHouseholdCarsAccessTo");
		list.add("numberOfHouseholdCarsOwned");
		list.add("numberOfHouseholdMotorcyclesAccessTo");
		list.add("numberOfHouseholdMotorcyclesOwned");
		list.add("transportZone");
		return list;
	}
	

	public Scenario getScenario(){
		return this.sc;
	}
	
}
