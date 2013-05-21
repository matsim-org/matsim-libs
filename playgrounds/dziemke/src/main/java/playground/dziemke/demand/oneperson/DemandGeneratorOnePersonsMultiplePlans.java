package playground.dziemke.demand.oneperson;

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

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.dziemke.demand.CommuterRelation;
import playground.dziemke.demand.PendlerMatrixReader;

public class DemandGeneratorOnePersonsMultiplePlans implements Runnable{
  
	private double scalingFactor = 0.01;
	// private double carMarketShare = 0.67;
	private double carMarketShare = 1.0;
	// private double fullyEmployedShare = 1.29;
	private double fullyEmployedShare = 1.0;
	
	private String commuterFileIn = "D:/Workspace/container/demand/input/B2009Ge.csv";
	private String commuterFileOut = "D:/Workspace/container/demand/input/B2009Ga.csv";
	private String shapeFileMunicipalities = "D:/Workspace/container/demand/input/shapefiles/gemeindenBerlin.shp";
	private String shapeFileLors = "D:/Workspace/container/demand/input/shapefiles/Bezirksregion_EPSG_25833.shp";
	
	private String outputFilePersons = new String("D:/Workspace/container/demand/input/cemdap_berlin/07/persons.dat");
	
	// new
	private String outputFilePersons2 = new String("D:/Workspace/container/demand/input/cemdap_berlin/08/persons.dat");
	private String outputFilePersons3 = new String("D:/Workspace/container/demand/input/cemdap_berlin/09/persons.dat");
	//
	
	private String outputFileHouseholds = new String("D:/Workspace/container/demand/input/cemdap_berlin/07/households.dat");
	
	private PendlerMatrixReader pendlerMatrixReader = new PendlerMatrixReader(shapeFileMunicipalities, commuterFileIn, 
			commuterFileOut, scalingFactor, carMarketShare, fullyEmployedShare);
	private List <CommuterRelation> commuterRelations = pendlerMatrixReader.getCommuterRelations();
	
	private Map <Integer, String> lors = new HashMap <Integer, String>();
		
	private Map <Integer, Household> households = new HashMap <Integer, Household>();
	// private Map <Integer, Person> persons = new HashMap <Integer, Person>();
	private Map <String, Person> persons = new HashMap <String, Person>();
	
	// new
	// private Map <Integer, Person> persons2 = new HashMap <Integer, Person>();
	private Map <String, Person> persons2 = new HashMap <String, Person>();
	// private Map <Integer, Person> persons3 = new HashMap <Integer, Person>();
	private Map <String, Person> persons3 = new HashMap <String, Person>();
	//
	

	public static void main(String[] args) {
		DemandGeneratorOnePersonsMultiplePlans demandGenerator = new DemandGeneratorOnePersonsMultiplePlans();
		demandGenerator.run();
	}

	
	@Override
	public void run() {
		readShape();
		generatePersonsAndHouseholds();
		writeToPersonsFile();
		
		// new
		writeToPersonsFile2();
		writeToPersonsFile3();
		//		
		
		writeToHouseholdsFile();
	}
	
	
	private void readShape() {
		Collection<SimpleFeature> allLors = ShapeFileReader.getAllFeatures(this.shapeFileLors);
	
		for (SimpleFeature lor : allLors) {
			Integer lorschluessel = Integer.parseInt((String) lor.getAttribute("SCHLUESSEL"));
			String name = (String) lor.getAttribute("LOR");
			this.lors.put(lorschluessel, name);
		}
	}
		
		
	private void generatePersonsAndHouseholds() {
		System.out.println("======================" + "\n"
				   + "Start generating persons and households" + "\n"
				   + "======================" + "\n");
		
		int householdIdCounter = 1;
		
		for (int i = 0; i<this.commuterRelations.size(); i++){
        	int quantity = this.commuterRelations.get(i).getQuantity();
        	
        	int source = this.commuterRelations.get(i).getFrom();
			int sink = this.commuterRelations.get(i).getTo();
        	
			for (int j = 0; j<quantity; j++){
				int householdId = householdIdCounter;
				// int personId = householdId;
				
				int homeTSZLocation;
				int locationOfWork;
				
				// new
				int locationOfWork2;
				int locationOfWork3;
				//
				
				// Gemeindeschluessel "11000000" = Berlin
				if (source == 11000000){
					homeTSZLocation = getRandomLor();
				} else {
					homeTSZLocation = source;
				}
								
				if (sink == 11000000){
					locationOfWork = getRandomLor();
				} else {
					locationOfWork = sink;
				}
				
				// new
				if (sink == 11000000){
					locationOfWork2 = getRandomLor();
				} else {
					locationOfWork2 = sink;
				}
				
				if (sink == 11000000){
					locationOfWork3 = getRandomLor();
				} else {
					locationOfWork3 = sink;
				}
				//
				
				int age = getRandomAge();
				// new 2
				String personId = householdId + "01";
				int employed = 1;
				// end new2
				
				Household household = new Household(householdId, homeTSZLocation);
				Person person = new Person(personId, householdId, employed, locationOfWork, age);
				
				// new
				Person person2 = new Person(personId, householdId, employed, locationOfWork2, age);
				Person person3 = new Person(personId, householdId, employed, locationOfWork3, age);
				//
				
				
				this.households.put(householdId, household);
				this.persons.put(personId, person);
				
				// new
				this.persons2.put(personId, person2);
				this.persons3.put(personId, person3);
				//
				
				householdIdCounter++;
			}
		}
	}
	
	
	public Integer getRandomAge() {
		int rangeMin = 18;
		int rangeMax = 99;
		Random r = new Random();
		int randomAge = (int) (rangeMin + (rangeMax - rangeMin) * r.nextDouble());
		return randomAge;
	}
	
		
	public Integer getRandomLor() {
		List <Integer> keys = new ArrayList<Integer>(this.lors.keySet());
		Random	random = new Random();
		Integer randomLor = keys.get(random.nextInt(keys.size()));
		return randomLor;
	}
	
		
	public void writeToPersonsFile() {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
            File personFile = new File(this.outputFilePersons);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		// for (Integer key : this.persons.keySet()) {
    		for (String key : this.persons.keySet()) {
    			int householdId = this.persons.get(key).getHouseholdId();
    			// int personId = this.persons.get(key).getpersonId();
    			String personId = this.persons.get(key).getpersonId();
    			int employed = this.persons.get(key).getEmployed();
    			int student = this.persons.get(key).getStudent();
    			int driversLicence = this.persons.get(key).getDriversLicence();
    			int locationOfWork = this.persons.get(key).getLocationOfWork();
    			int locationOfSchool = this.persons.get(key).getLocationOfSchool();
    			int female = this.persons.get(key).getFemale();
    			int age = this.persons.get(key).getAge();
    			int parent = this.persons.get(key).getParent();
    			
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
		System.out.println("Persons geschrieben.");
    }
	
	
	// new
	public void writeToPersonsFile2() {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
            File personFile = new File(this.outputFilePersons2);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		// for (Integer key : this.persons2.keySet()) {
    		for (String key : this.persons2.keySet()) {
    			int householdId = this.persons2.get(key).getHouseholdId();
    			// int personId = this.persons2.get(key).getpersonId();
    			String personId = this.persons2.get(key).getpersonId();
    			int employed = this.persons2.get(key).getEmployed();
    			int student = this.persons2.get(key).getStudent();
    			int driversLicence = this.persons2.get(key).getDriversLicence();
    			int locationOfWork = this.persons2.get(key).getLocationOfWork();
    			int locationOfSchool = this.persons2.get(key).getLocationOfSchool();
    			int female = this.persons2.get(key).getFemale();
    			int age = this.persons2.get(key).getAge();
    			int parent = this.persons2.get(key).getParent();
    			
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
		System.out.println("Persons geschrieben.");
    }
	
	
	public void writeToPersonsFile3() {
		BufferedWriter bufferedWriterPersons = null;
		
		try {
            File personFile = new File(this.outputFilePersons3);
    		FileWriter fileWriterPersons = new FileWriter(personFile);
    		bufferedWriterPersons = new BufferedWriter(fileWriterPersons);
    		    		    		
    		// for (Integer key : this.persons3.keySet()) {
    		for (String key : this.persons3.keySet()) {
    			int householdId = this.persons3.get(key).getHouseholdId();
    			// int personId = this.persons3.get(key).getpersonId();
    			String personId = this.persons3.get(key).getpersonId();
    			int employed = this.persons3.get(key).getEmployed();
    			int student = this.persons3.get(key).getStudent();
    			int driversLicence = this.persons3.get(key).getDriversLicence();
    			int locationOfWork = this.persons3.get(key).getLocationOfWork();
    			int locationOfSchool = this.persons3.get(key).getLocationOfSchool();
    			int female = this.persons3.get(key).getFemale();
    			int age = this.persons3.get(key).getAge();
    			int parent = this.persons3.get(key).getParent();
    			
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
		System.out.println("Persons geschrieben.");
    }
	//
	
	
	public void writeToHouseholdsFile() {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(this.outputFileHouseholds);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		int householdIdFromPersonBefore = 0;
    		
    		// use map of persons to write a household for every person
    		// under the condition that the household does not already exist (written from another persons)
    		// this procedure is used to enable the potential use of multiple-person households
    		
    		// for (Integer key : this.persons.keySet()) {
    		for (String key : this.persons.keySet()) {
    			int householdId = this.persons.get(key).getHouseholdId();
    			
    			if (householdId != householdIdFromPersonBefore) {
    				int numberOfAdults = this.households.get(householdId).getNumberOfAdults();
    				int totalNumberOfHouseholdVehicles = this.households.get(householdId).getTotalNumberOfHouseholdVehicles();
    				int homeTSZLocation = this.households.get(householdId).getHomeTSZLocation();
    				int numberOfChildren = this.households.get(householdId).getNumberOfChildren();
    				int householdStructure = this.households.get(householdId).getHouseholdStructure();
    				
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
		System.out.println("Households geschrieben.");
    }

}