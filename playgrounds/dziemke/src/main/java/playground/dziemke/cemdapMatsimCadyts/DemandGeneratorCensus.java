/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.cemdapMatsimCadyts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.cemdapMatsimCadyts.oneperson.SimpleHousehold;
import playground.dziemke.cemdapMatsimCadyts.oneperson.SimplePerson;
import playground.dziemke.utils.LogToOutputSaver;

/**
 * This class is derived from "playground.dziemke.cemdapMatsimCadyts.oneperson.DemandGeneratorOnePersonV2.java"
 * In contrast to its predecessors, it creates a full population (not just car users).
 * Its main inputs are the Census and the Pendlerstatistik
 * 
 * @author dziemke
 */
public class DemandGeneratorCensus {
	private static final Logger log = Logger.getLogger(DemandGeneratorCensus.class);
	
	
	
	/*
	 * there will be mismatches between number of employees from zensus and commuter from commuter file
	 * - because of socially secured workers (commuter file) vs. all workers (zensus)
	 * - because not exactly the same year
	 * Handle by scaling?
	 */

	public static void main(String[] args) {
		// Parameters
//		double scalingFactor = 1.;
		int numberOfPlansPerPerson = 3;
		// Gemeindeschluessel of Berlin is 11000000 (Gemeindeebene) and 11000 (Landkreisebene)
		Integer planningAreaId = 11000000;
		
		// Input and output files
//		String commuterFileOutgoing1 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Berlin_2009/B2009Ga.txt";
//		String commuterFileOutgoing2 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
//		String commuterFileOutgoing3 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil2BR2009Ga.txt";
//		String commuterFileOutgoing4 = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil3BR2009Ga.txt";
		String commuterFileOutgoingTest = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga_Test.txt";
		
//		String censusFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung.csv";
		String censusFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
		
//		String shapeFileMunicipalities = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/gemeindenBerlin.shp";
		String shapeFileLors = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Bezirksregion_EPSG_25833.shp";
		
		String outputBase = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/census-based_test2/"; // TODO ...

		
		// Infrastructure
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Map<Id<Household>, Household> households = new HashMap<>();
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		// Create a CensusReader
		CensusReader censusReader = new CensusReader(censusFile, ";");
		ObjectAttributes municipalities = censusReader.getMunicipalities();
//		List<String> municipalitiesList = censusReader.getMunicipalitiesList();
		Map<Integer, Map<Integer, CommuterRelationV2>> relationsMap = new HashMap<>();
		
		// Read in commuter relations
//		{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing1, "\t");
//			Map<Integer, Map<Integer, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing2, "\t");
//			Map<Integer, Map<Integer, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing3, "\t");
//			Map<Integer, Map<Integer, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}{
//			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing4, "\t");
//			Map<Integer, Map<Integer, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
//			relationsMap.putAll(currentRelationMap);
//		}
		{
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoingTest, "\t");
			Map<Integer, Map<Integer, CommuterRelationV2>> currentRelationMap = commuterFileReader.getRelationsMap();
			relationsMap.putAll(currentRelationMap);
		}
		
		// Create storage objects
//		Map<Integer, SimpleHousehold> householdMap = new HashMap<Integer, SimpleHousehold>();
		Map<Integer, Map<String, SimplePerson>> mapOfPersonMaps = new HashMap<Integer, Map<String, SimplePerson>>();
		for (int i=1; i<=numberOfPlansPerPerson; i++) {
			Map<String, SimplePerson> persons = new HashMap<String, SimplePerson>();
			mapOfPersonMaps.put(i, persons);
		}
		
		// Read in LORs	
//		Map<Integer, String> lors = new HashMap<Integer, String>();
		List<Integer> lors = readShape(shapeFileLors, "SCHLUESSEL", "LOR");
		
		// Create households and persons
//		int householdCounter = 1;
//		int personCounter = 1;
		
		
		// New loop over inhabitant of municipalities
//		for (String municipalityId : municipalitiesList) { // wrong this goes over all German municipalities from Zensus
		for (Integer municipalityIdInt : relationsMap.keySet()) {
			Map<Integer, CommuterRelationV2> relationsFromMunicipality = relationsMap.get(municipalityIdInt);
			
			// Employees from Zensus seems to be all employees, not only socially-secured employees
			String municipalityId = municipalityIdInt.toString();
			int employeesMale = (int) municipalities.getAttribute(municipalityId, "employedMale");
			int employeesFemale = (int) municipalities.getAttribute(municipalityId, "employedFemale");
						
			scaleRelations(relationsFromMunicipality, employeesMale, employeesFemale);
			List<Integer> commuterRelationListMale = createRelationList(relationsFromMunicipality, "male");
			List<Integer> commuterRelationListFemale = createRelationList(relationsFromMunicipality, "female");
			
//			int population = (int) municipalities.getAttribute(municipalityId, "population");
			
			int pop18_24Male = (int) municipalities.getAttribute(municipalityId, "pop18_24Male");
			int pop25_29Male = (int) municipalities.getAttribute(municipalityId, "pop25_29Male");
			int pop30_39Male = (int) municipalities.getAttribute(municipalityId, "pop30_39Male");
			int pop40_49Male = (int) municipalities.getAttribute(municipalityId, "pop40_49Male");
			int pop50_64Male = (int) municipalities.getAttribute(municipalityId, "pop50_64Male");
			int pop65_74Male = (int) municipalities.getAttribute(municipalityId, "pop65_74Male");
			int pop75PlusMale = (int) municipalities.getAttribute(municipalityId, "pop75PlusMale");
			
			int pop18_24Female = (int) municipalities.getAttribute(municipalityId, "pop18_24Female");
			int pop25_29Female = (int) municipalities.getAttribute(municipalityId, "pop25_29Female");
			int pop30_39Female = (int) municipalities.getAttribute(municipalityId, "pop30_39Female");
			int pop40_49Female = (int) municipalities.getAttribute(municipalityId, "pop40_49Female");
			int pop50_64Female = (int) municipalities.getAttribute(municipalityId, "pop50_64Female");
			int pop65_74Female = (int) municipalities.getAttribute(municipalityId, "pop65_74Female");
			int pop75PlusFemale = (int) municipalities.getAttribute(municipalityId, "pop75PlusFemale");
			
			int adultsMale = pop18_24Male + pop25_29Male + pop30_39Male + pop40_49Male + pop50_64Male;
			int adultsFemale = pop18_24Female + pop25_29Female + pop30_39Female + pop40_49Female + pop50_64Female;
			
			
			
			double adultsToEmployeesMaleRatio = 0.;
			double adultsToEmployeesFemaleRatio = 0.;
			if (employeesMale != 0) { // Avoid dividing by zero
				adultsToEmployeesMaleRatio = adultsMale / employeesMale;
			} else {
				adultsToEmployeesMaleRatio = 2.; // TODO This is an assumption; maybe improve later!
			}
			if (employeesFemale != 0) { // Avoid dividing by zero
				adultsToEmployeesFemaleRatio = adultsFemale / employeesFemale;
			} else {
				adultsToEmployeesFemaleRatio = 2.; // TODO This is an assumption; maybe improve later!
			}
				
			
			int seniorsMale = pop65_74Male + pop75PlusMale;
			int seniorsFemale = pop65_74Female + pop75PlusFemale;
			
			int counter = 1;
			
			{
				int gender = 0;
				int lowerAgeBound = 18;
				int upperAgeBound = 24;
				createHouseholdsAndPersons(population, households, counter, municipalityIdInt, planningAreaId, lors, pop18_24Male, 
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesMaleRatio, commuterRelationListMale);
			}
			counter += pop18_24Male;
			{
				int gender = 1;
				int lowerAgeBound = 18;
				int upperAgeBound = 24;
				createHouseholdsAndPersons(population, households, counter, municipalityIdInt, planningAreaId, lors, pop18_24Female, 
						gender, lowerAgeBound, upperAgeBound, adultsToEmployeesFemaleRatio, commuterRelationListFemale);
			}
			counter += pop18_24Female;
			
			
			

//			if (commuterRelationList.size() > 100) {
//				throw new RuntimeException("More than 100 commuter relations from minucipality with ID " + municipalityId + " remain unassigned.");
//			}
			
			
			
		}
		// TODO householdsFile
		writePersonsFile(population, outputBase + "persons.dat");
	}


	private static void createHouseholdsAndPersons(Population population, Map<Id<Household>, Household> households, int counter,
			Integer municipalityId, Integer planningAreaId, List<Integer> lors, int numberOfPersons, int gender, int lowerAgeBound,
			int upperAgeBound, double adultsToEmployeesRatio, List<Integer> commuterRelationList) {
		
		for (int i = 0; i < numberOfPersons; i++) {
			Id<Household> householdId = Id.create(municipalityId + "_" + (counter + i), Household.class);
			HouseholdImpl household = new HouseholdImpl(householdId); // TODO Or use factory?
			household.getAttributes().putAttribute("numberOfAdults", 1); // always 1; no household structure
			household.getAttributes().putAttribute("totalNumberOfHouseholdVehicles", 1);
			household.getAttributes().putAttribute("homeTSZLocation", getHomeLocation(municipalityId, planningAreaId, lors));
			household.getAttributes().putAttribute("numberOfChildren", 0); // none, ignore them in this version
			household.getAttributes().putAttribute("householdStructure", 1); // 1 = single, no children
			
			Id<Person> personId = Id.create(householdId + "_1", Person.class);
			Person person = population.getFactory().createPerson(personId);
			// attribute names inspired by "PersonUtils.java": "sex", "hasLicense", "carAvail", "employed", "age", "travelcards"
			person.getAttributes().putAttribute("householdId", householdId);
			boolean employed = getEmployed(adultsToEmployeesRatio);
			person.getAttributes().putAttribute("employed", employed);
			person.getAttributes().putAttribute("student", false); // TODO certain share of young adults?
			person.getAttributes().putAttribute("hasLicense", true); // for CEMDAP's "driversLicence" variable
			
			if (employed == true) {
				person.getAttributes().putAttribute("locationOfWork", getRandomWorkLocation(commuterRelationList));
			} else {
				person.getAttributes().putAttribute("locationOfWork", -99);
			}
			
			person.getAttributes().putAttribute("locationOfSchool", -99); // TODO ?
			person.getAttributes().putAttribute("gender", gender); // for CEMDAP's "female" variable
			person.getAttributes().putAttribute("age", getAgeInBounds(lowerAgeBound, upperAgeBound));
			person.getAttributes().putAttribute("parent", false);
			
			population.addPerson(person);
			
			List<Id<Person>> personIds = new ArrayList<>(); // does in current implementation (only 1 p/hh) not make much sense
			personIds.add(personId);
			household.setMemberIds(personIds);
			households.put(householdId, household);
		}
	}	
	
			
	private static void scaleRelations(Map<Integer, CommuterRelationV2> relationsFromMunicipality, int employeesMale, int employeesFemale) {
		// Count all commuters starting in the given municipality
		int commutersMale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsMale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == 0 || relation.getTrips() == null) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsMale((int) (relation.getTrips() / 2));
				}
			}
			commutersMale += relation.getTripsMale();
		}
		int commutersFemale = 0;
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			if (relation.getTripsFemale() == null) { // This is the case when there are very few people traveling on that relation
				if (relation.getTrips() == 0 || relation.getTrips() == null) {
					throw new RuntimeException("No travellers at all on this relation! This should not happen.");
				} else {
					relation.setTripsFemale((int) (relation.getTrips() / 2));
				}
			}
			commutersFemale += relation.getTripsFemale();
		}
		
		// Compute ratios
		double employeesToCommutersMaleRatio = 0.;
		double employeesToCommutersFemaleRatio = 0.;
		if (employeesMale != 0) { // Avoid dividing by zero
			employeesToCommutersMaleRatio = employeesMale / commutersMale;
		} else {
			employeesToCommutersMaleRatio = 2.; // TODO This is an assumption; maybe improve later!
		}
		if (employeesFemale != 0) { // Avoid dividing by zero
			employeesToCommutersFemaleRatio = employeesFemale / commutersFemale;
		} else {
			employeesToCommutersFemaleRatio = 2.; // TODO This is an assumption; maybe improve later!
		}
		
		// Scale
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsMale((int) (relation.getTripsMale() * employeesToCommutersMaleRatio)); 
		}
		for (CommuterRelationV2 relation : relationsFromMunicipality.values()) {
			relation.setTripsFemale((int) (relation.getTripsFemale() * employeesToCommutersFemaleRatio));
		}
	}


	private static List<Integer> createRelationList(Map<Integer, CommuterRelationV2> relationsFromMunicipality, String gender) {
		List<Integer> commuterRealtionsList = new ArrayList<>();
		for (Integer destination : relationsFromMunicipality.keySet()) {
			int trips = 0;
			if (gender.equals("male")) {
				trips = relationsFromMunicipality.get(destination).getTripsMale();
			} else if (gender.equals("female")) {
				trips = relationsFromMunicipality.get(destination).getTripsFemale();
			} else {
				throw new IllegalArgumentException("Must either be male or female.");
			}
			for (int i = 0; i < trips ; i++) {
				commuterRealtionsList.add(destination);
			}
		}
		return commuterRealtionsList;
	}


	private static Integer getRandomWorkLocation(List<Integer> commuterRelationList) {
		Random random = new Random();
		int position = random.nextInt(commuterRelationList.size());
		Integer workLocation = commuterRelationList.get(position);
		commuterRelationList.remove(position);
		return workLocation;
	}


	private static Integer getHomeLocation(Integer municipalityId, Integer planningAreaId, List<Integer> lors) {
		Integer locationId;
		if (municipalityId == planningAreaId){
			locationId = getRandomLor(lors);
		} else {
			locationId = municipalityId;
		}
		return locationId;
	}


	private static boolean getEmployed(double adultsToEmployeesRatio) {
		if (Math.random() * adultsToEmployeesRatio < 1) {
			return true;
		} else {
			return false;
		}
	}
	
	
	private static int getAgeInBounds(int lowerBound, int upperBound) {
		return (int) (lowerBound + Math.random() * (upperBound - lowerBound + 1));
	}
			
	
	private static Integer getRandomLor(List<Integer> lors) {
		Random random = new Random();
		Integer randomLor = lors.get(random.nextInt(lors.size()));
		return randomLor;
	}


	private static List<Integer> readShape(String shapeFile, String attributeKey, String attributeName) {
		List<Integer> lors = new ArrayList<>();
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

		for (SimpleFeature feature : features) {
			Integer key = Integer.parseInt((String) feature.getAttribute(attributeKey));
			lors.add(key);
		}
		return lors;
	}
	
	

		/*
		// TODO adapt to new loop
		for (int i = 0; i<commuterRelationList.size(); i++){
			int quantity = commuterRelationList.get(i).getTripsMale();
	        	
	       	int source = commuterRelationList.get(i).getFrom();
			int sink = commuterRelationList.get(i).getTo();
	        	
			for (int j = 0; j<quantity; j++){
				// Create households
				int householdId = householdCounter;
				int homeTSZLocation;
					
				if (source == planningAreaId){
					homeTSZLocation = getRandomLor(lors);
				} else {
					homeTSZLocation = source;
				}
				Household household = new Household(householdId, homeTSZLocation);
				householdMap.put(householdId, household);
				
				// Create persons
				int sex = getSex();			
				int age = getAge();
				String personId = householdId + "01";

				int employed;
				if (age > 65) {
					employed = 0;
				} else {
					employed = getEmployedWorkingAge();
				}
				
				int student;
				// We make the assumption that students are not employed at the same time and that students are
				// aged less than 30 years
				if (employed == 0 && age < 30) {
					student = getStudent();
				} else {
					student = 0;
				}
				
				for (int k=1; k<=numberOfPlansPerPerson; k++) {
					int locationOfWork;
					if (sink == planningAreaId){
						locationOfWork = getRandomLor(lors);
					} else {
						locationOfWork = sink;
					}
					
					if (employed == 0) {
						locationOfWork = -99;
					}
					
					int locationOfSchool;
					if (sink == planningAreaId){
						locationOfSchool = getRandomLor(lors);
					} else {
						locationOfSchool = sink;
					}
					
					if (student == 0) {
						locationOfSchool = -99;
					}

					SimplePerson person = new SimplePerson(personId, householdId, employed, student, locationOfWork,
							locationOfSchool, sex, age);
					mapOfPersonMaps.get(k).put(personId, person);
				}	
				householdCounter++;
				}
			}
		
			writeHouseholdsFile(mapOfPersonMaps.get(1), householdMap, outputBase + "households.dat");
			for (int i=1; i<=numberOfPlansPerPerson; i++) {
				writePersonsFile(mapOfPersonMaps.get(i), outputBase + "persons" + i + ".dat");
			}
		}
*/
	
	
	public static void writePersonsFile(Population population, String fileName) {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
			File personFile = new File(fileName);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		for (Person person : population.getPersons().values()) {
    			
    			
    			Id<Household> householdId = (Id<Household>) person.getAttributes().getAttribute("householdId");
    			Id<Person> personId = person.getId();
    			
    			int employed;
    			if ((boolean) person.getAttributes().getAttribute("employed") == true) {
    				employed = 1;
    			} else {
    				employed = 0;
    			}
    			
    			int student;
    			if ((boolean) person.getAttributes().getAttribute("student") == true) {
    				student = 1;
    			} else {
    				student = 0;
    			}
    			
    			int driversLicence;
    			if ((boolean) person.getAttributes().getAttribute("hasLicense") == true) {
    				driversLicence = 1;
    			} else {
    				driversLicence = 0;
    			}
    			
    			int locationOfWork = (Integer) person.getAttributes().getAttribute("locationOfWork");
    			int locationOfSchool = (Integer) person.getAttributes().getAttribute("locationOfSchool");
    			
    			int female = (Integer) person.getAttributes().getAttribute("gender"); // assumes that female = 1
    			int age = (Integer) person.getAttributes().getAttribute("age");
    			
    			int parent;
    			if ((boolean) person.getAttributes().getAttribute("parent") == true) {
    				parent = 1;
    			} else {
    				parent = 0;
    			}
    			
    			// Altogether this creates 59 columns = number in query file
    			bufferedWriterPersons.write(householdId + "\t" + personId + "\t" + employed  + "\t" + student
    					+ "\t" + driversLicence + "\t" + locationOfWork + "\t" + locationOfSchool
    					+ "\t" + female + "\t" + age + "\t" + parent + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 
    					+ "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0  + "\t" + 0 );
    			bufferedWriterPersons.newLine();
    		}
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterPersons != null) {
                    bufferedWriterPersons.flush();
                    bufferedWriterPersons.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		log.info("Persons file " + fileName + " written.");
    }
	

	public static void writeHouseholdsFile(Map <String, SimplePerson> persons, Map<Integer, SimpleHousehold> households,
			String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(fileName);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		int householdIdFromPersonBefore = 0;
    		
    		// Use map of persons to write a household for every person under the condition that the household does not
    		// already exist (written from another persons); used to enable the potential use of multiple-person households.
    		// TODO use proper household sizes
    		for (String key : persons.keySet()) {
    			int householdId = persons.get(key).getHouseholdId();
    			
    			if (householdId != householdIdFromPersonBefore) {
    				int numberOfAdults = households.get(householdId).getNumberOfAdults();
    				int totalNumberOfHouseholdVehicles = households.get(householdId).getTotalNumberOfHouseholdVehicles();
    				int homeTSZLocation = households.get(householdId).getHomeTSZLocation();
    				int numberOfChildren = households.get(householdId).getNumberOfChildren();
    				int householdStructure = households.get(householdId).getHouseholdStructure();
    				
    				// Altogether this creates 32 columns = number in query file
    				bufferedWriterHouseholds.write(householdId + "\t" + numberOfAdults + "\t" + totalNumberOfHouseholdVehicles
    						 + "\t" + homeTSZLocation + "\t" + numberOfChildren + "\t" + householdStructure + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    						 + "\t" + 0);
	    			bufferedWriterHouseholds.newLine();
	    			householdIdFromPersonBefore = householdId;
    			}
    		}
    	} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriterHouseholds != null) {
                    bufferedWriterHouseholds.flush();
                    bufferedWriterHouseholds.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		log.info("Households file " + fileName + " written.");
    }
}