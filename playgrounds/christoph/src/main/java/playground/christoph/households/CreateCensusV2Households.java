///* *********************************************************************** *
// * project: org.matsim.*
// * CreateCensusV2Households.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.christoph.households;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.InputStreamReader;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Random;
//import java.util.TreeMap;
//import java.util.zip.GZIPInputStream;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.facilities.ActivityOptionImpl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.population.ActivityImpl;
//import org.matsim.core.population.PopulationWriter;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.CoordUtils;
//import org.matsim.core.utils.misc.Counter;
//import org.matsim.households.Household;
//import org.matsim.households.Households;
//import org.matsim.households.HouseholdsWriterV10;
//import org.matsim.utils.objectattributes.ObjectAttributes;
//import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
//
//public class CreateCensusV2Households {
//
//	private static final Logger log = Logger.getLogger(CreateCensusV2Households.class);
//
//	private final Scenario scenario;
//	private final String separator = "\t";
//	private final Charset charset = Charset.forName("UTF-8");
//
//	private Map<Id<Person>, CensusData> censusDataMap;	// PersonId, Census Entry
//	private Map<Id<Household>, Integer> householdHHTPMap;	// HouseholdId, HHTP Code
//	private Map<Id, List<Id<Household>>> buildingHouseholdMap;	// BuildingId, List of households assigned to that building
//	private Map<Id, List<Id<Household>>> municipalityHouseholdMap;	// MuncipalityId, List of households assigned to that municipality
//
//	private final ObjectAttributes householdAttributes;
//
//	public static void main(String[] args) throws Exception {
//		if (args.length != 7) return;
//
//		new CreateCensusV2Households(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
//	}
//
//	/**
//	 * Creates the household data structure for a population based on the Swiss Census 2000 (Version 2).
//	 * Persons from collective households (HHTP Code 980X) are re-assigned to existing households.
//	 *
//	 * If the building of a Person is known but not its exact household, it is tried to find a household
//	 * in the same building where the person can be assigned to (HHTP Codes 2XXX to 3YYY).
//	 *
//	 * If the person could not be assigned to a household, the reassignment is done on municipality level.
//	 * Additionally, the persons home activities as well as his/her knowledge are adapted.
//	 *
//	 * For each household it is checked, whether all members have the same home facility.
//	 * The scores of all plans are removed.
//	 *
//	 * @param populationFile ... the input population file
//	 * @param facilitiesFile ... the input facilities file
//	 * @param networkFile ... the input network file
//	 * @param censusFile ... the input Swiss census file
//	 * @param outHouseholdsFile ... the output households file
//	 * @param outObjectAttributesFile ... the output household object attributes file
//	 * @param outPopulationFile ... the output population file
//	 * @throws Exception ... if an error occurred when writing the object attributes file
//	 */
//	public CreateCensusV2Households(String populationFile, String facilitiesFile, String networkFile, String censusFile,
//			String outHouseholdsFile, String outObjectAttributesFile, String outPopulationFile) throws Exception {
//
//		Config config = ConfigUtils.createConfig();
//		config.scenario().setUseKnowledge(true);
//		config.scenario().setUseHouseholds(true);
//
//		config.plans().setInputFile(populationFile);
//		config.facilities().setInputFile(facilitiesFile);
//		config.network().setInputFile(networkFile);
//		this.scenario = ScenarioUtils.loadScenario(config);
//
//		this.householdAttributes = new ObjectAttributes();
//
//		readCensusFile(censusFile);
//
//		createHouseHolds();
//
//		reassignCollectiveHouseholds();
//
//		writeHouseHolds(outHouseholdsFile);
//
//		writeHouseHoldObjectAttributes(outObjectAttributesFile);
//
//		writePopulation(outPopulationFile);
//
//		printStatistics();
//	}
//
//	public Scenario getScenario() {
//		return this.scenario;
//	}
//
//	private void readCensusFile(String censusFile) throws Exception {
//
//		boolean isGZ = censusFile.toLowerCase().endsWith(".gz");
//		boolean isZip = censusFile.toLowerCase().endsWith(".zip");
//
//		this.censusDataMap = new HashMap<Id<Person>, CensusData>();
//		this.householdHHTPMap = new HashMap<Id<Household>, Integer>();
//		this.buildingHouseholdMap = new HashMap<Id, List<Id<Household>>>();
//		this.municipalityHouseholdMap = new HashMap<Id, List<Id<Household>>>();
//
//		FileInputStream fis = null;
//		GZIPInputStream gzis = null;
//		ZipFile zipFile = null;
//		InputStreamReader isr = null;
//	    BufferedReader br = null;
//
//	    Counter lineCounter = new Counter("Parsed lines from the population file ");
//
//	    log.info("start parsing...");
//	    fis = new FileInputStream(censusFile);
//	    if (isGZ) {
//	    	gzis = new GZIPInputStream(fis);
//	    	isr = new InputStreamReader(gzis, this.charset);
//	    } else if (isZip) {
//	    	zipFile = new ZipFile(censusFile);
//	    	ZipEntry zipEntry = zipFile.entries().nextElement();	// we assume that there is only one entry
//	    	isr = new InputStreamReader(zipFile.getInputStream(zipEntry), this.charset);
//	    }
//	    else {
//	    	isr = new InputStreamReader(fis, this.charset);
//	    }
//	    br = new BufferedReader(isr);
//
//	    // skip first line
//	    br.readLine();
//
//	    String line;
//	    while((line = br.readLine()) != null) {
//	    	String[] cols = line.split(this.separator);
//
//	    	int WKAT = parseInteger(cols[10]);
//	    	int GEM2 = parseInteger(cols[11]);
//	    	int PARTNR = parseInteger(cols[12]);
//
//	    	CensusData censusData = new CensusData();
//	    	censusData.ZGDE = parseInteger(cols[1]);
//	    	censusData.GEBAEUDE_ID = parseInteger(cols[2]);
//	    	censusData.HHNR = parseInteger(cols[3]);
//	    	censusData.WOHNUNG_NR = parseInteger(cols[4]);
//	    	censusData.PERSON_ID = parseInteger(cols[5]);
//	    	censusData.HHTPZ = parseInteger(cols[49]);
//	    	censusData.HHTPW = parseInteger(cols[50]);
//
//	    	/*
//	    	 * One person can be represented by multiple lines in the Census file.
//	    	 *
//	    	 * allowed combinations:
//	    	 * wkat  gem2  partnr  occurrence meaning
//	    	 * 1     -9    -9      1/person   person does have only one household (z and w)
//	    	 * 3     -7    -7      1/person   person is ONLY part of the 'wirtschaftliche wohnbevoelkerung' (w)
//	    	 * 4     -7    -7      1/person   person is ONLY part of the 'zivilrechtliche wohnbevoelkerung' (z)
//	    	 * 3     id    id      2/person   person is part of w and z. current line reflects w
//	    	 * 4     id    id      2/person   person is part of w and z. current line reflects z
//	    	 *
//	    	 *
//	    	 * If a person has a "wirtschaftlicher" household, then we use its Id. Otherwise we use the
//	    	 * "zivilrechtliche".
//	    	 */
//	    	if (WKAT == 1 && GEM2 == -9 && PARTNR == -9) {
//	    		this.censusDataMap.put(this.scenario.createId(String.valueOf(censusData.PERSON_ID)), censusData);
//	    	} else if (WKAT == 3 && GEM2 == -7 && PARTNR == -7) {
////	    		log.info("only w: " + censusData.PERSON_ID);
//	    		this.censusDataMap.put(this.scenario.createId(String.valueOf(censusData.PERSON_ID)), censusData);
//	    	} else if (WKAT == 4 && GEM2 == -7 && PARTNR == -7) {
////	    		log.info("only z: " + censusData.PERSON_ID");
//	    		this.censusDataMap.put(this.scenario.createId(String.valueOf(censusData.PERSON_ID)), censusData);
//	    	} else if (WKAT == 3 && GEM2 > 0 && PARTNR > 0) {
//
//	    		CensusData cd = this.censusDataMap.get(this.scenario.createId(String.valueOf(PARTNR)));
//	    		if (cd == null) {
//	    			this.censusDataMap.put(this.scenario.createId(String.valueOf(censusData.PERSON_ID)), censusData);
//	    		} else {
//		    		/*
//		    		 * There is already a partner-entry in the map: reuse its Id but overwrite
//		    		 * all other data.
//		    		 */
//	    			this.censusDataMap.remove(this.scenario.createId(String.valueOf(PARTNR)));
//	    			this.censusDataMap.put(this.scenario.createId(String.valueOf(PARTNR)), censusData);
//	    		}
//	    	} else if (WKAT == 4 && GEM2 > 0 && PARTNR > 0) {
//	    		CensusData cd = this.censusDataMap.get(this.scenario.createId(String.valueOf(PARTNR)));
//	    		if (cd == null) {
//	    			this.censusDataMap.put(this.scenario.createId(String.valueOf(censusData.PERSON_ID)), censusData);
//	    		} else {
//	    			// there is already a partner-entry which will be used
//	    			continue;
//	    		}
//	    	} else log.warn("Unknown combination!");
//
//	    	Id householdId = this.scenario.createId(String.valueOf(censusData.HHNR));
//	    	Integer HHTP = this.householdHHTPMap.get(householdId);
//	    	if (HHTP == null) {
//	    		this.householdHHTPMap.put(householdId, censusData.HHTPW);
//	    	}
//	    	else if (HHTP != censusData.HHTPW) log.warn("Non-matching HHTP entry found for household " + censusData.HHNR);
//
//	    	// Add the household to the list of households located in the building
//	    	Id buildingId = this.scenario.createId(String.valueOf(censusData.GEBAEUDE_ID));
//	    	List<Id<Household>> list = this.buildingHouseholdMap.get(buildingId);
//	    	if (list == null) {
//	    		list = new ArrayList<Id<Household>>();
//	    		this.buildingHouseholdMap.put(buildingId, list);
//	    	}
//	    	list.add(householdId);
//
//	    	// Add the household to the list of households located in the municipality
//	    	Id muncipalityId = this.scenario.createId(String.valueOf(censusData.ZGDE));
//	    	list = this.municipalityHouseholdMap.get(muncipalityId);
//	    	if (list == null) {
//	    		list = new ArrayList<Id<Household>>();
//	    		this.municipalityHouseholdMap.put(muncipalityId, list);
//	    	}
//	    	list.add(householdId);
//
//	    	lineCounter.incCounter();
//	    }
//	    lineCounter.printCounter();
//
//	    br.close();
//	    isr.close();
//	    if (isGZ) gzis.close();
//	    fis.close();
//	    log.info("done.");
//	}
//
//	private void createHouseHolds() {
//
//		log.info("Creating households...");
//		Households households = ((ScenarioImpl) this.scenario).getHouseholds();
//        Knowledges result2;
//        throw new RuntimeException("Knowledges are no more.");
//
//        Knowledges knowledges = result2;
//		ActivityFacilities facilities = ((ScenarioImpl) this.scenario).getActivityFacilities();
//
//		Map<Id,Id> householdFacilityMap = new HashMap<Id, Id>();
//
//		for(Person person : this.scenario.getPopulation().getPersons().values()) {
//			/*
//			 * Get the id of the person's household. It is stored in the knowledge
//			 * description of the person.
//			 * Examples: (hh_w:2492821)(hh_z:2492821); (hh_z:2492821)(hh_w:2492821)
//			 * hh_w... wirtschaftlicher Haushalt (z.B. Pendler unter der Woche)
//			 * hh_z... ziviler Haushalt (z.B. Pendler am Wochenende)
//			 */
//			Object knowledge = knowledges.getKnowledgesByPersonId().get(person.getId());
//            String result1;
//            throw new RuntimeException("Knowledges are no more.");
//
//            String description = result1;
//			if (description.contains("hh_w:")) {
//				description = description.substring(description.indexOf("hh_w:"));
//				description = description.substring(5, description.indexOf(")"));
//			} else if (description.contains("hh_z:")) {
//				description = description.substring(description.indexOf("hh_z:"));
//				description = description.substring(5, description.indexOf(")"));
//			} else {
//				log.warn("No household Id found in person's knowledge - skipping person!");
//				continue;
//			}
////			String idString = description.substring(6, description.indexOf(")"));
////			Id houseHoldId = scenario.createId(idString);
//			Id householdId = this.scenario.createId(description);
//
//            ArrayList<ActivityOptionImpl> result;
//            throw new RuntimeException("Knowledges are no more.");
//
//            ActivityFacility homeFacility = result.get(0).getFacility();
//			Coord homeFacilityCoord = homeFacility.getCoord();
//
//			Household household = households.getHouseholds().get(householdId);
//
//			if (person.getId().equals(this.scenario.createId("802686"))) {
//				log.info("found!");
//				log.info("housholdid " + householdId);
//				log.info("description " + description);
//			}
//
//			if (household == null) {
//				household = households.getFactory().createHousehold(householdId);
//				households.getHouseholds().put(householdId, household);
//
//				householdFacilityMap.put(householdId, homeFacility.getId());
//
//				this.householdAttributes.putAttribute(householdId.toString(), "homeFacilityId", homeFacility.getId().toString());
//				this.householdAttributes.putAttribute(householdId.toString(), "x", homeFacilityCoord.getX());
//				this.householdAttributes.putAttribute(householdId.toString(), "y", homeFacilityCoord.getY());
//			} else {
//				Id householdFacilityId = householdFacilityMap.get(householdId);
//				if (!homeFacility.getId().equals(householdFacilityId)) {
//
//					log.warn("Home facility has changed - knowledge and plans have to be adapted!");
//
//					Coord householdFacilityCoord = facilities.getFacilities().get(householdFacilityId).getCoord();
//					double distance = CoordUtils.calcDistance(householdFacilityCoord, homeFacilityCoord);
//
//					// max distance between two points in two neighbor hectars is 223.61m
//					if (distance > 225.0) {
//						log.warn("Distance between facilities is > 225.0 (is: " + distance + ")!");
//					}
//
//					// re-assigning person to household
//					reassignHousehold(person.getId(), householdFacilityId);
//				}
//			}
//
//			household.getMemberIds().add(person.getId());
//		}
//		log.info("done.");
//	}
//
//	private void writeHouseHolds(String householdsFile) {
//		log.info("Writing households...");
//		new HouseholdsWriterV10(((ScenarioImpl) this.scenario).getHouseholds()).writeFile(householdsFile);
//		log.info("done.");
//	}
//
//	private void writeHouseHoldObjectAttributes(String objectAttributesFile) {
//		// add an entry for the municipality where the household is located
//		log.info("Adding municipality information to household object attributes...");
//		for (Entry<Id, List<Id<Household>>> entry : this.municipalityHouseholdMap.entrySet()) {
//			List<Id<Household>> householdIds = entry.getValue();
//			for (Id id : householdIds) {
//				this.householdAttributes.putAttribute(id.toString(), "municipality", Integer.valueOf(entry.getKey().toString()));
//			}
//		}
//		log.info("done.");
//
//		// and an entry for the HHTP code of the household
//		log.info("Adding HTTP codes to households...");
//		for (Entry<Id<Household>, Integer> entry : this.householdHHTPMap.entrySet()) {
//			this.householdAttributes.putAttribute(entry.getKey().toString(), "HHTP", entry.getValue());
//		}
//		log.info("done.");
//
//		log.info("Writing household object attributes...");
//		new ObjectAttributesXmlWriter(this.householdAttributes).writeFile(objectAttributesFile);
//		log.info("done.");
//	}
//
//	private void writePopulation(String populationFile) {
//		// remove scores from all plans
//		for (Person person : this.scenario.getPopulation().getPersons().values()) {
//			for (Plan plan : person.getPlans()) {
//				plan.setScore(null);
//			}
//		}
//
//		new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork()).write(populationFile);
//	}
//
//	private void printStatistics() {
//		int s1 = 0;
//		int s2 = 0;
//		int s3 = 0;
//		int s4 = 0;
//		int s5 = 0;
//		int s6 = 0;
//		int s7 = 0;
//		int s8 = 0;
//		int s9 = 0;
//		int s10 = 0;
//		int s11 = 0;
//		int s12 = 0;
//		int s13 = 0;
//		int s14 = 0;
//		int s15 = 0;
//		int s16 = 0;
//		int s17 = 0;
//		int s18 = 0;
//		int s19 = 0;
//		int s20plus = 0;
//
//		Households houseHolds = ((ScenarioImpl) this.scenario).getHouseholds();
//		Map<Integer, Integer> mapPersonCount = new TreeMap<Integer, Integer>();
//		for (Household household : houseHolds.getHouseholds().values()) {
//			int members = household.getMemberIds().size();
//
//			if (members == 1) s1++;
//			else if (members == 2) s2++;
//			else if (members == 3) s3++;
//			else if (members == 4) s4++;
//			else if (members == 5) s5++;
//			else if (members == 6) s6++;
//			else if (members == 7) s7++;
//			else if (members == 8) s8++;
//			else if (members == 9) s9++;
//			else if (members == 10) s10++;
//			else if (members == 11) s11++;
//			else if (members == 12) s12++;
//			else if (members == 13) s13++;
//			else if (members == 14) s14++;
//			else if (members == 15) s15++;
//			else if (members == 16) s16++;
//			else if (members == 17) s17++;
//			else if (members == 18) s18++;
//			else if (members == 19) s19++;
//			else {
//				s20plus++;
////				log.info("Members: " + members + ", HouseholdId: " + household.getId() + ", HHTP: " + householdHHTPMap.get(household.getId()));
//			}
//
//			// count number of persons per HHTP type
//			int HHTP = this.householdHHTPMap.get(household.getId());
//			Integer count = mapPersonCount.get(HHTP);
//			if (count == null) mapPersonCount.put(HHTP, members);
//			else mapPersonCount.put(HHTP, count + members);
//		}
//
//		log.info("Households with one member:        " + s1);
//		log.info("Households with two members:       " + s2);
//		log.info("Households with three members:     " + s3);
//		log.info("Households with four members:      " + s4);
//		log.info("Households with five members:      " + s5);
//		log.info("Households with six members:       " + s6);
//		log.info("Households with seven members:     " + s7);
//		log.info("Households with eight members:     " + s8);
//		log.info("Households with nine members:      " + s9);
//		log.info("Households with ten members:       " + s10);
//		log.info("Households with eleven members:    " + s11);
//		log.info("Households with twelve members:    " + s12);
//		log.info("Households with thirteen members:  " + s13);
//		log.info("Households with fourteen members:  " + s14);
//		log.info("Households with fifteen members:   " + s14);
//		log.info("Households with sixteen members:   " + s14);
//		log.info("Households with seventeen members: " + s14);
//		log.info("Households with eighteen members:  " + s14);
//		log.info("Households with ninteen members:   " + s14);
//		log.info("Households with twenty or more members: " + s20plus);
//
//		Map<Integer, Integer> mapHHTPCount = new TreeMap<Integer, Integer>();
//		for (int i : this.householdHHTPMap.values()) {
//			Integer count = mapHHTPCount.get(i);
//
//			if (count == null) mapHHTPCount.put(i, 1);
//			else mapHHTPCount.put(i, count + 1);
//		}
//		for (Entry<Integer, Integer> entry : mapHHTPCount.entrySet()) {
//			log.info("HHTP Code: " + entry.getKey() + ", number of households: " + entry.getValue() + ", number of persons: " + mapPersonCount.get(entry.getKey()));
//		}
//	}
//
//	/*
//	 * Assign persons from collective households to "real household".
//	 */
//	private void reassignCollectiveHouseholds() {
//		log.info("Re-assigning collective households...");
//
//		Counter reassignmentCounter = new Counter("Re-assigned collective households ");
//		Counter notReassignableInBuilding = new Counter("Not Re-assignable in building ");
//		Counter notReassignableInMunicipality = new Counter("Not Re-assignable in municipality ");
//
//		Counter removedHouseholds = new Counter("Removed households ");
//
//		Random random = MatsimRandom.getLocalInstance();
//
//		int reassigned9802 = 0;
//		int reassigned9803 = 0;
//		int reassigned9804 = 0;
//
//		Households households = ((ScenarioImpl) this.scenario).getHouseholds();
//		Iterator<Household> householdIter = households.getHouseholds().values().iterator();
//		while (householdIter.hasNext()) {
//			Household household = householdIter.next();
//
//			// get HHTP Code
//			int HHTP = this.householdHHTPMap.get(household.getId());
//
//			if (HHTP == 9802) {
//				Iterator<Id<Person>> iter = household.getMemberIds().iterator();
//				while (iter.hasNext()) {
//					Id personId = iter.next();
//
////					boolean reassigned = reassignInBuilding(personId, houseHolds, random);
//					boolean reassigned = reassignInMunicipality(personId, households, random);
//					if (!reassigned) {
//						notReassignableInMunicipality.incCounter();
//						log.info("Not reassignable HHPT 9802: " + personId.toString());
//					} else {
//						/*
//						 * Remove the person from its original household and increase the reassigned counter.
//						 */
//						iter.remove();
//						reassigned9802++;
//					}
//				}
//
//			} else if (HHTP == 9803) {
//				Iterator<Id<Person>> iter = household.getMemberIds().iterator();
//				while (iter.hasNext()) {
//					Id personId = iter.next();
//
////					boolean reassigned = reassignInBuilding(personId, houseHolds, random);
//					boolean reassigned = reassignInMunicipality(personId, households, random);
//					if (!reassigned) notReassignableInMunicipality.incCounter();
//					if (!reassigned) log.info("Not reassignable HHPT 9803: " + personId.toString());
//
//					if (reassigned) {
//						/*
//						 * Remove the person from its original household and increase the reassigned counter.
//						 */
//						iter.remove();
//						reassigned9803++;
//					}
//				}
//			} else if (HHTP == 9804) {
//				Iterator<Id<Person>> iter = household.getMemberIds().iterator();
//				while (iter.hasNext()) {
//					Id personId = iter.next();
//
//					boolean reassigned = reassignInBuilding(personId, households, random);
//					if(!reassigned) {
//						notReassignableInBuilding.incCounter();
//						reassigned = reassignInMunicipality(personId, households, random);
//						if (!reassigned) notReassignableInMunicipality.incCounter();
//						if (!reassigned) log.info("Not reassignable HHPT 9804: " + personId.toString());
//					}
//
//					if (reassigned) {
//						/*
//						 * Remove the person from its original household and increase the reassigned counter.
//						 */
//						iter.remove();
//						reassigned9804++;
//					}
//				}
//			} else {
//				// no household that has to be re-assigned
//				continue;
//			}
//			reassignmentCounter.incCounter();
//
//			if (household.getMemberIds().size() == 0) {
//				householdIter.remove();
//				removedHouseholds.incCounter();
//			}
//		}
//		removedHouseholds.printCounter();
//		reassignmentCounter.printCounter();
//
//		log.info("Reassigned persons from households with HHTP Code 9082: " + reassigned9802);
//		log.info("Reassigned persons from households with HHTP Code 9083: " + reassigned9803);
//		log.info("Reassigned persons from households with HHTP Code 9084: " + reassigned9804);
//	}
//
//	private boolean reassignInMunicipality(Id personId, Households houseHolds, Random random) {
//		CensusData censusData = this.censusDataMap.get(personId);
//		Id municipalityId = this.scenario.createId(String.valueOf(censusData.ZGDE));
//
//		/*
//		 * Identify those households in the same building where additional persons
//		 * can be assigned to: households with more than one member (> 2000) and non
//		 * collective households (< 9000).
//		 */
//		List<Id<Household>> householdsInMuncipality = this.municipalityHouseholdMap.get(municipalityId);
//		List<Id<Household>> useableHouseholds = new ArrayList<Id<Household>>();
//		for (Id<Household> id : householdsInMuncipality) {
//			/*
//			 * If there is no corresponding entry in the households data.
//			 * This will occur, if a sample population is used.
//			 */
//			if (!houseHolds.getHouseholds().containsKey(id)) continue;
//
//			int HHTP = this.householdHHTPMap.get(id);
//			if (HHTP > 2000 && HHTP < 9000) useableHouseholds.add(id);
//		}
//		if (useableHouseholds.size() == 0) {
//			log.warn("Could not reassign person within municipality - no valid households found! Municipality Id: " + municipalityId);
//			return false;
//		}
//
//		/*
//		 * Choose randomly one of the usable households and add the person.
//		 */
//		int r = random.nextInt(useableHouseholds.size());
//		int oldHousehold = this.censusDataMap.get(personId).HHNR;
//		Id newHousehold = useableHouseholds.get(r);
//		houseHolds.getHouseholds().get(newHousehold).getMemberIds().add(personId);
//		reassignHousehold(personId, newHousehold);
//		log.info("\t\t reassigned within municipality: person " + personId + " from household " + oldHousehold + " to " + newHousehold);
//
//		return true;
//	}
//
//	private boolean reassignInBuilding(Id personId, Households houseHolds, Random random) {
//		CensusData censusData = this.censusDataMap.get(personId);
//		Id buildingId = this.scenario.createId(String.valueOf(censusData.GEBAEUDE_ID));
//
//		/*
//		 * Identify those households in the same building where additional persons
//		 * can be assigned to: households with more than one member (> 2000) and non
//		 * collective households (< 9000).
//		 */
//		List<Id<Household>> householdsInBuilding = this.buildingHouseholdMap.get(buildingId);
//		List<Id<Household>> useableHouseholds = new ArrayList<Id<Household>>();
//		for (Id<Household> id : householdsInBuilding) {
//			/*
//			 * If there is no corresponding entry in the households data.
//			 * This will occur, if a sample population is used.
//			 */
//			if (!houseHolds.getHouseholds().containsKey(id)) continue;
//
//			int HHTP2 = this.householdHHTPMap.get(id);
//			if (HHTP2 > 2000 && HHTP2 < 9000) useableHouseholds.add(id);
//		}
//		if (useableHouseholds.size() == 0) {
////			log.warn("Could not reassign person within building - trying municipality!");
//			return false;
//		}
//
//		/*
//		 * Choose randomly one of the usable households and add the person.
//		 */
//		int r = random.nextInt(useableHouseholds.size());
//		int oldHousehold = this.censusDataMap.get(personId).HHNR;
//		Id newHousehold = useableHouseholds.get(r);
//		houseHolds.getHouseholds().get(newHousehold).getMemberIds().add(personId);
//		reassignHousehold(personId, newHousehold);
//		log.info("\t\t reassigned within building: person " + personId + " from household " + oldHousehold + " to " + newHousehold);
//
//		return true;
//	}
//
//	/*
//	 * If a person is assigned to a different household or the members of a household
//	 * have different home facilities, this has to be corrected. This methods replaces
//	 * all points within the plans of a person to the home facility with the home facility
//	 * of a given household.
//	 */
//	private void reassignHousehold(Id personId, Id newHouseholdId) {
//        Knowledges result4;
//        throw new RuntimeException("Knowledges are no more.");
//
//        Knowledges knowledges = result4;
//		ActivityFacilities facilities = ((ScenarioImpl) this.scenario).getActivityFacilities();
//
//		Id homeFacilityId = this.scenario.createId((String) this.householdAttributes.getAttribute(newHouseholdId.toString(), "homeFacilityId"));
//		double x = (Double) this.householdAttributes.getAttribute(newHouseholdId.toString(), "x");
//		double y = (Double) this.householdAttributes.getAttribute(newHouseholdId.toString(), "y");
//
//		Coord homeFacilityCoord = this.scenario.createCoord(x, y);
//		ActivityFacility homeFacility = facilities.getFacilities().get(homeFacilityId);
//
//		Person person = this.scenario.getPopulation().getPersons().get(personId);
//
//		// adapt the knowledge - ActivityOptions cannot be edited, therefore replace them
//		Object knowledge = knowledges.getKnowledgesByPersonId().get(personId);
//        ArrayList<ActivityOptionImpl> result2;
//        throw new RuntimeException("Knowledges are no more.");
//
//        List<ActivityOptionImpl> list = result2;
//		for (ActivityOptionImpl activityOption : list) {
//			ActivityOptionImpl newActivityOption = new ActivityOptionImpl("home");
//			newActivityOption.setFacility(homeFacility);
//            boolean result3;
//            throw new RuntimeException("Knowledges are no more.");
//
//            boolean isPrimary = result3;
//            boolean result1;
//            throw new RuntimeException("Knowledges are no more.");
//
//            boolean result;
//            throw new RuntimeException("Knowledges are no more.");
//
//        }
//
//		// adapt the plan
//		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
//			if (planElement instanceof Activity) {
//				Activity activity = (Activity) planElement;
//				if (activity.getType().equals("home")) {
//					((ActivityImpl) activity).setFacilityId(homeFacilityId);
//					((ActivityImpl) activity).setCoord(homeFacilityCoord);
//				}
//			}
//		}
//	}
//
//	private int parseInteger(String string) {
//		if (string == null) return 0;
//		else if (string.trim().equals("")) return 0;
//		else return Integer.valueOf(string);
//	}
//
//	private class CensusData {
//		int HHNR;
//		int ZGDE;
//		int HHTPZ;
//		int HHTPW;
//		int GEBAEUDE_ID;
//		int PERSON_ID;
//		int WOHNUNG_NR;
//	}
//}