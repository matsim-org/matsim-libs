/* *********************************************************************** *
 * project: org.matsim.*
 * SurveyParser.java
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
package playground.jjoubert.projects.capeTownDiary;

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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.jjoubert.projects.capeTownDiary.HouseholdEnums.MonthlyIncome;
import playground.jjoubert.projects.capeTownDiary.PersonEnums.Disability;
import playground.southafrica.utilities.Header;

/**
 * Class to parse the different elements/files from the travel survey for the
 * City of Cape Town of 2012.
 * 
 * @author jwjoubert
 */
public class SurveyParser {
	final private static Logger LOG = Logger.getLogger(SurveyParser.class);
	private Scenario sc;
	private Map<Id<Person>, List<String[]>> tripMap;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SurveyParser.class.toString(), args);
		
		SurveyParser sp = new SurveyParser();
		sp.parsePersons(args[0]);
		sp.parseHouseholds(args[1]);
		sp.parseDerivedHouseholdAssets(args[2]);
		sp.parseDiaryTrips(args[3]);
		
		sp.writePopulation(args[4]);
		
		Header.printFooter();
	}

	public SurveyParser() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.tripMap = new HashMap<>();
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
					Coord homeCoord = sampleCoord(homeZone);
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
					String recordedMode = sa[34];
					if(recordedMode.contains(";")){
						LOG.error("Multiple modes for " + pId.toString() + ": " + recordedMode);
						throw new RuntimeException();
					}
					String matsimMode = DiaryEnums.ModeOfTravel.parseFromCode(
							sa[34].equals("") ? 0 : Integer.parseInt(sa[34])).getMatsimMode();
					Leg leg = pf.createLeg(matsimMode);
					double tripStartTime = 0.0;
					double tripEndTime = 0.0;
					try{
						tripStartTime = Time.parseTime(sa[25]);
					} catch (NumberFormatException e){
						LOG.error(" TIME: ===> " + pId.toString() + ": " + sa[25]);
					}
					try{
						tripEndTime = Time.parseTime(sa[27]);
					} catch (NumberFormatException e){
						LOG.error(" TIME: ===> " + pId.toString() + ": " + sa[27]);
					}
					leg.setDepartureTime(tripStartTime);
					leg.setTravelTime(tripEndTime - tripStartTime);
					plan.addLeg(leg);
					
					/* Add the destination activity. */
					if(sa[32].equals("") || sa[32].equals(" ")){
						if(!matsimTrip.equalsIgnoreCase("h")){
							zoneInfo++;
						} else{
							noZoneInfo++;
						}
					} else{
						zoneInfo++;
					}
					Coord actCoord = sampleCoord(sa[32]);
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
	private Coord sampleCoord(String zone){
		double x = 0.0;
		double y = 0.0;
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
	
	
	public void writePopulation(String folder){
		folder = folder + (folder.endsWith("/") ? "" : "/");
		LOG.info("Writing population to " + folder);
		
		new PopulationWriter(sc.getPopulation()).write(folder + "population.xml");
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(folder + "populationAttributes.xml");
		
		new HouseholdsWriterV10(sc.getHouseholds()).writeFile(folder + "households.xml");
		new ObjectAttributesXmlWriter(sc.getHouseholds().getHouseholdAttributes()).writeFile(folder + "householdAttributes.xml");
		
		
		LOG.info("Done writing population.");
	}

}
