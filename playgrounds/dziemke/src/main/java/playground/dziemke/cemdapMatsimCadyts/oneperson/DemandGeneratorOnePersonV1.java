package playground.dziemke.cemdapMatsimCadyts.oneperson;

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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.cemdapMatsimCadyts.CommuterFileReader;
import playground.dziemke.cemdapMatsimCadyts.CommuterRelation;

public class DemandGeneratorOnePersonV1 {
	private static final Logger log = Logger.getLogger(DemandGeneratorOnePersonV1.class);
  
	public static void main(String[] args) {
	
	// main parameters
	// carFactor of 0.67 for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
	// socialSecurityFactor of 1.29, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
	// reaffirmed through own calculations
//	double scalingFactor = 2.0 * 0.01;
//	double carFactor = 0.67;
//	double socialSecurityFactor = 1.29;
	
	double scalingFactor = 0.01;
	double carShareBE = 0.67;
	double carShareBB = 0.67;
	double socialSecurityFactor = 1.29;
	double expansionFactor = 2.0;
	
	//int numberOfPlansPerPerson = 10;
	int numberOfPlansPerPerson = 1;
	// Gemeindeschluessel of Berlin is 11000000
	Integer planningAreaId = 11000000;
	
	// input and output files
	String commuterFileIn = "D:/VSP/Masterarbeit/Data/BA-Pendlerstatistik/Berlin2009/B2009Ge.txt.";
	String commuterFileOut = "D:/VSP/Masterarbeit/Data/BA-Pendlerstatistik/Berlin2009/B2009Ga.txt.";
	
	String shapeFileMunicipalities = "D:/Workspace/container/demand/input/shapefiles/gemeindenBerlin.shp";
	String shapeFileLors = "D:/Workspace/container/demand/input/shapefiles/Bezirksregion_EPSG_25833.shp";
	
	//String outputBase = "D:/Workspace/container/demand/input/cemdap_berlin/17/";
	String outputBase = "D:/Workspace/container/demand/input/cemdap_berlin/test/03/";
	
	// create a CommuterFileReader and store its output to a list
	CommuterFileReader commuterFileReader = new CommuterFileReader(shapeFileMunicipalities, commuterFileIn, carShareBB,	commuterFileOut, 
			carShareBE, scalingFactor * socialSecurityFactor * expansionFactor, planningAreaId.toString());
	List<CommuterRelation> commuterRelations = commuterFileReader.getCommuterRelations();
//	CommuterFileReader commuterFileReader = new CommuterFileReader(shapeFileMunicipalities, commuterFileIn, 
//			commuterFileOut, scalingFactor * carFactor * socialSecurityFactor, planningAreaId.toString());
//	List<CommuterRelation> commuterRelations = commuterFileReader.getCommuterRelations();
	
	// create storage objects
	Map<Integer, String> lors = new HashMap<Integer, String>();
	Map<Integer, Household> households = new HashMap<Integer, Household>();
	Map<Integer, Map<String, Person>> mapOfPersonsMaps = new HashMap<Integer, Map<String, Person>>();
	for (int i=1; i<=numberOfPlansPerPerson; i++) {
		Map<String, Person> persons = new HashMap<String, Person>();
		mapOfPersonsMaps.put(i, persons);
	}
	
	// read in LORs	
	readShape(shapeFileLors, lors);
	
	// create households and persons
	int householdIdCounter = 1;
	
	//#########################################################################################################################
	int counter = 0;
	for (int i = 0; i<commuterRelations.size(); i++){
		int quantity = commuterRelations.get(i).getQuantity();
        
		for (int j = 0; j<quantity; j++){
			counter++;
		}	
	}
	System.out.println("commuter realtions: " + counter);
	//###############################################################################################################################
		
	for (int i = 0; i<commuterRelations.size(); i++){
		
		// print sth in console
		//CommuterRelation currentRelation = commuterRelations.get(i);
		//log.info("from: " + currentRelation.getFromName() + " - " + currentRelation.getFrom() + " - to: " + currentRelation.getToName()
		//		+ " - " + currentRelation.getTo() + " - quantity: " + currentRelation.getQuantity());
				
       	int quantity = commuterRelations.get(i).getQuantity();
        	
       	int source = commuterRelations.get(i).getFrom();
		int sink = commuterRelations.get(i).getTo();
        	
		for (int j = 0; j<quantity; j++){
			// create households
			int householdId = householdIdCounter;
			int homeTSZLocation;
				
			if (source == planningAreaId){
				homeTSZLocation = getRandomLor(lors);
			} else {
				homeTSZLocation = source;
			}
			Household household = new Household(householdId, homeTSZLocation);
			households.put(householdId, household);
			
			// create persons
			int sex = 1;
			int age = getRandomAge();
			String personId = householdId + "01";
			int employed = 1;
							
			for (int k=1; k<=numberOfPlansPerPerson; k++) {
				int locationOfWork;
				if (sink == 11000000){
					locationOfWork = getRandomLor(lors);
				} else {
					locationOfWork = sink;
				}
					
				Person person = new Person(personId, householdId, employed, 0, locationOfWork, -99, sex, age);
				mapOfPersonsMaps.get(k).put(personId, person);
			}	
			householdIdCounter++;
			}
		}
	
		writeToHouseholdsFile(mapOfPersonsMaps.get(1), households, outputBase + "households.dat");
		for (int i=1; i<=numberOfPlansPerPerson; i++) {
			writeToPersonsFile(mapOfPersonsMaps.get(i), outputBase + "persons" + i + ".dat");
		}
	}
		

	private static void readShape(String shapeFileLors, Map<Integer, String> lors) {
		Collection<SimpleFeature> allLors = ShapeFileReader.getAllFeatures(shapeFileLors);
	
		for (SimpleFeature lor : allLors) {
			Integer lorschluessel = Integer.parseInt((String) lor.getAttribute("SCHLUESSEL"));
			String name = (String) lor.getAttribute("LOR");
			lors.put(lorschluessel, name);
		}
	}
	
	
	private static Integer getRandomAge() {
		int rangeMin = 18;
		int rangeMax = 99;
		Random r = new Random();
		int randomAge = (int) (rangeMin + (rangeMax - rangeMin) * r.nextDouble());
		return randomAge;
	}
	
		
	private static Integer getRandomLor(Map<Integer, String> lors) {
		List <Integer> keys = new ArrayList<Integer>(lors.keySet());
		Random	random = new Random();
		Integer randomLor = keys.get(random.nextInt(keys.size()));
		return randomLor;
	}
	
		
	public static void writeToPersonsFile(Map <String, Person> persons, String fileName) {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
			File personFile = new File(fileName);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		for (String key : persons.keySet()) {
    			int householdId = persons.get(key).getHouseholdId();
    			String personId = persons.get(key).getpersonId();
    			int employed = persons.get(key).getEmployed();
    			int student = persons.get(key).getStudent();
    			int driversLicence = persons.get(key).getDriversLicence();
    			int locationOfWork = persons.get(key).getLocationOfWork();
    			int locationOfSchool = persons.get(key).getLocationOfSchool();
    			int female = persons.get(key).getSex();
    			int age = persons.get(key).getAge();
    			int parent = persons.get(key).getParent();
    			
    			// altogether this creates 59 columns = number in query file
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
	

	public static void writeToHouseholdsFile(Map <String, Person> persons, Map<Integer, Household> households, String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(fileName);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		int householdIdFromPersonBefore = 0;
    		
    		// use map of persons to write a household for every person
    		// under the condition that the household does not already exist (written from another persons)
    		// this procedure is used to enable the potential use of multiple-person households
    		for (String key : persons.keySet()) {
    			int householdId = persons.get(key).getHouseholdId();
    			
    			if (householdId != householdIdFromPersonBefore) {
    				int numberOfAdults = households.get(householdId).getNumberOfAdults();
    				int totalNumberOfHouseholdVehicles = households.get(householdId).getTotalNumberOfHouseholdVehicles();
    				int homeTSZLocation = households.get(householdId).getHomeTSZLocation();
    				int numberOfChildren = households.get(householdId).getNumberOfChildren();
    				int householdStructure = households.get(householdId).getHouseholdStructure();
    				
    				// altogether this creates 32 columns = number in query file
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