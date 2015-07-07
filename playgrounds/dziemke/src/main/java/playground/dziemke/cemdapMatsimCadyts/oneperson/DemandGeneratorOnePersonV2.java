package playground.dziemke.cemdapMatsimCadyts.oneperson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import playground.dziemke.cemdapMatsimCadyts.CommuterFileReader;
import playground.dziemke.cemdapMatsimCadyts.CommuterRelation;
import playground.dziemke.utils.LogToOutputSaver;
import playground.dziemke.utils.TwoAttributeShapeReader;

public class DemandGeneratorOnePersonV2 {
	private static final Logger log = Logger.getLogger(DemandGeneratorOnePersonV2.class);

	public static void main(String[] args) {
		// main parameters
		//double scalingFactor = 0.01;
		double scalingFactor = 0.1;
		double carShareBE = 0.37;
		double carShareBB = 0.55;
		double allWorkersToSociallySecuredWorkersRatio = 1.52;
		double adultsToWorkersRatio = 1.9;
		double expansionFactor = 1.;
		//int numberOfPlansPerPerson = 10;
		int numberOfPlansPerPerson = 3;
		// Gemeindeschluessel of Berlin is 11000000
		Integer planningAreaId = 11000000;
		
		
		// input and output files
		String commuterFileIn = "D:/VSP/CemdapMatsimCadyts/Data/BA-Pendlerstatistik/Berlin2009/B2009Ge.txt";
		String commuterFileOut = "D:/VSP/CemdapMatsimCadyts/Data/BA-Pendlerstatistik/Berlin2009/B2009Ga.txt";
		
		String shapeFileMunicipalities = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/gemeindenBerlin.shp";
		String shapeFileLors = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/Bezirksregion_EPSG_25833.shp";
		
		String outputBase = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/21/";
		
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		
		// create a PendlerMatrixReader and store its output to a list
		CommuterFileReader commuterFileReader = new CommuterFileReader(shapeFileMunicipalities, commuterFileIn, carShareBB,	commuterFileOut, 
				//carShareBE, scalingFactor * socialSecurityFactor * adultsWorkersFactor * expansionFactor, planningAreaId.toString());
				carShareBE, scalingFactor * allWorkersToSociallySecuredWorkersRatio * adultsToWorkersRatio * expansionFactor, planningAreaId);
		List<CommuterRelation> commuterRelationList = commuterFileReader.getCommuterRelations();
		
		
		// create storage objects
		Map<Integer, String> lors = new HashMap<Integer, String>();
		Map<Integer, Household> householdMap = new HashMap<Integer, Household>();
		Map<Integer, Map<String, SimplePerson>> mapOfPersonMaps = new HashMap<Integer, Map<String, SimplePerson>>();
		for (int i=1; i<=numberOfPlansPerPerson; i++) {
			Map<String, SimplePerson> persons = new HashMap<String, SimplePerson>();
			mapOfPersonMaps.put(i, persons);
		}
		
		
		// read in LORs	
		TwoAttributeShapeReader.readShape(shapeFileLors, lors, "SCHLUESSEL", "LOR");
		
		
		// create households and persons
		int householdIdCounter = 1;
		
		
		for (int i = 0; i<commuterRelationList.size(); i++){
			int quantity = commuterRelationList.get(i).getQuantity();
	        	
	       	int source = commuterRelationList.get(i).getFrom();
			int sink = commuterRelationList.get(i).getTo();
	        	
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
				householdMap.put(householdId, household);
				
				
				// create persons
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
				// we make the assumption that stundets are not employed at the same time and that students are
				// aged less than 30 years
				if (employed == 0 && age < 30) {
					student = getStudent();
				} else {
					student = 0;
				}
				
				for (int k=1; k<=numberOfPlansPerPerson; k++) {
					int locationOfWork;
					//if (sink == 11000000){
					if (sink == planningAreaId){
						locationOfWork = getRandomLor(lors);
					} else {
						locationOfWork = sink;
					}
					
					if (employed == 0) {
						locationOfWork = -99;
					}
					
					int locationOfSchool;
					//if (sink == 11000000){
					if (sink == planningAreaId){
						locationOfSchool = getRandomLor(lors);
					} else {
						locationOfSchool = sink;
					}
					
					if (student == 0) {
						locationOfSchool = -99;
					}

					SimplePerson person = new SimplePerson(personId, householdId, employed, student, locationOfWork, locationOfSchool, sex, age);
					mapOfPersonMaps.get(k).put(personId, person);
				}	
				householdIdCounter++;
				}
			}
		
			writeToHouseholdsFile(mapOfPersonMaps.get(1), householdMap, outputBase + "households.dat");
			for (int i=1; i<=numberOfPlansPerPerson; i++) {
				writeToPersonsFile(mapOfPersonMaps.get(i), outputBase + "persons" + i + ".dat");
			}
		}
		
	
	private static int getSex() {
		Random r = new Random();
		double randomNumber = r.nextDouble();
		// assume that both sexes are equally frequent for every age, work status etc.
		// TODO this can clearly be improved, IPF etc.
		if (randomNumber < 0.5) {
			return 1;
		} else {
			return 0;
		}
	}
	
	
	private static int getAge() {
		int ageRange = getAgeRange();
		
        // Es ist wichtig darauf zu achten, dass nach Ausführung einer Anweisung der Schleifendurchlauf mit "break"
        // unterbrochen wird, da die folgenden Sprungmarken sonst ebenfalls geprüft und ggf. ausgeführt werden.
        // Trifft keine Übereinstimmung zu, kann optional mit der Sprungmarke default eine Standardanweisung ausgeführt werden. 
        switch (ageRange) {
            case 1:	return getAgeInRange(18, 19);
            case 2:	return getAgeInRange(20, 24);
            case 3:	return getAgeInRange(25, 29);
            case 4:	return getAgeInRange(30, 34);
            case 5:	return getAgeInRange(35, 39);
            case 6:	return getAgeInRange(40, 44);
            case 7:	return getAgeInRange(45, 59);
            case 8:	return getAgeInRange(60, 64);
            case 9:	return getAgeInRange(65, 90);
            default: 
            	throw new RuntimeException("No age range met.");
            	//return -1;
        }
	}
	
	
	private static int getAgeRange() {
		Random r = new Random();
		// cf. p. 11 of statistic of 2012
		int populationInWorkingAge = 2932167;
		double randomNumber = r.nextDouble() * populationInWorkingAge;
		if (randomNumber < 54469) {return 1;}
		if (randomNumber < 54469+222434) {return 2;}
		if (randomNumber < 54469+222434+284440) {return 3;}
		if (randomNumber < 54469+222434+284440+277166) {return 4;}
		if (randomNumber < 54469+222434+284440+277166+228143) {return 5;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192) {return 6;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482) {return 7;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482+198908) {return 8;}
		if (randomNumber < 54469+222434+284440+277166+228143+256192+755482+198908+654933) {return 9;}
		else {
			throw new RuntimeException("No age selected.");
			//return -1;
		}
	}
	
	
	private static int getAgeInRange(int rangeMin, int rangeMax) {
		Random r = new Random();
		int randomAge = (int) (rangeMin + (rangeMax - rangeMin) * r.nextDouble());
		return randomAge;
	}
	
	
	private static int getEmployedWorkingAge() {
		Random r = new Random();
		double randomNumber = r.nextDouble();
		// Population aged 18 through 65 (2277234) is 1.48 times as high as number of Erwerbstaetige (1543825)
		// 100 out of 148 are determined to be employed
		if (randomNumber < 1/1.48) {
			return 1;
		} else {
			return 0;
		}
	}
	
	
	private static int getStudent() {
		Random r = new Random();
		double randomNumber = r.nextDouble();
		// OLD: Number of  students in Berlin (150000) divided by non-employed population aged 18-29 (266000)
		// Number of  students in Berlin (150000) divided by non-employed population aged 18-29 (181000)
		//TODO Correctly it has to be 150/181.
		if (randomNumber < 150/266.) {
			return 1;
		} else {
			return 0;
		}
	}
	
	
	private static Integer getRandomLor(Map<Integer, String> lors) {
		List <Integer> keys = new ArrayList<Integer>(lors.keySet());
		Random	random = new Random();
		Integer randomLor = keys.get(random.nextInt(keys.size()));
		return randomLor;
	}
	
		
	public static void writeToPersonsFile(Map <String, SimplePerson> persons, String fileName) {
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
    			// TODO check if column position is correct, especially for "age" and "parent"
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
	

	public static void writeToHouseholdsFile(Map <String, SimplePerson> persons, Map<Integer, Household> households, String fileName) {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(fileName);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		int householdIdFromPersonBefore = 0;
    		
    		// use map of persons to write a household for every person
    		// under the condition that the household does not already exist (written from another persons)
    		// this procedure is used to enable the potential use of multiple-person households
    		// TODO use proper household sizes
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