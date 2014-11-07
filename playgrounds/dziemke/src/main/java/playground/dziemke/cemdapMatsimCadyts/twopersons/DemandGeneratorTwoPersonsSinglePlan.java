package playground.dziemke.cemdapMatsimCadyts.twopersons;

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

import playground.dziemke.cemdapMatsimCadyts.CommuterFileReader;
import playground.dziemke.cemdapMatsimCadyts.CommuterRelation;

public class DemandGeneratorTwoPersonsSinglePlan implements Runnable{
  
//	private double scalingFactor = 0.01;
//	private double carMarketShare = 0.67;
//	private double fullyEmployedShare = 1.29;
	
	double scalingFactor = 0.01;
	double carShareBE = 0.67;
	double carShareBB = 0.67;
	double socialSecurityFactor = 1.29;
	double expansionFactor = 2.0;
	
	// Gemeindeschluessel of Berlin is 11000000
	Integer planningAreaId = 11000000;
	
	private String commuterFileIn = "D:/Workspace/container/demand/input/B2009Ge.csv";
	private String commuterFileOut = "D:/Workspace/container/demand/input/B2009Ga.csv";
	private String shapeFileMunicipalities = "D:/Workspace/container/demand/input/shapefiles/gemeindenBerlin.shp";
	private String shapeFileLors = "D:/Workspace/container/demand/input/shapefiles/Bezirksregion_EPSG_25833.shp";
	
	// private String outputFilePersons = new String("D:/Workspace/container/demand/input/cemdap_berlin/06/persons.dat");
	private String outputFilePersons = new String("D:/Workspace/container/demand/input/cemdap_berlin/testingTwo/persons.dat");
	// private String outputFileHouseholds = new String("D:/Workspace/container/demand/input/cemdap_berlin/06/households.dat");
	private String outputFileHouseholds = new String("D:/Workspace/container/demand/input/cemdap_berlin/testingTwo/households.dat");
	
	
	CommuterFileReader commuterFileReader = new CommuterFileReader(shapeFileMunicipalities, commuterFileIn, carShareBB,	commuterFileOut, 
			//carShareBE, scalingFactor * socialSecurityFactor * expansionFactor, planningAreaId.toString());
			carShareBE, scalingFactor * socialSecurityFactor * expansionFactor, planningAreaId);
	List<CommuterRelation> commuterRelations = commuterFileReader.getCommuterRelations();
//	private PendlerMatrixReader pendlerMatrixReader = new PendlerMatrixReader(shapeFileMunicipalities, commuterFileIn, 
//			commuterFileOut, scalingFactor, carMarketShare, fullyEmployedShare);
//	private List <CommuterRelation> commuterRelations = pendlerMatrixReader.getCommuterRelations();
	
	private Map <Integer, String> lors = new HashMap <Integer, String>();
		
	private Map <Integer, HouseholdWithPersons> households = new HashMap <Integer, HouseholdWithPersons>();
	// private Map <Integer, Person> persons = new HashMap <Integer, Person>();
	// private Map <String, Person> persons = new HashMap <String, Person>();
	
	// private List <Integer> householdsAlreadyIncluded = new ArrayList <Integer>();
	

	public static void main(String[] args) {
		DemandGeneratorTwoPersonsSinglePlan demandGenerator = new DemandGeneratorTwoPersonsSinglePlan();
		demandGenerator.run();
	}

	
	@Override
	public void run() {
		readShape();
		generatePersonsAndHouseholds();
		writeToPersonsFile();	
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
		//Random random = new Random();
		
		for (int i = 0; i<this.commuterRelations.size(); i++){
        	int quantity = this.commuterRelations.get(i).getQuantity();
        	
        	int source = this.commuterRelations.get(i).getFrom();
			int sink = this.commuterRelations.get(i).getTo();
        	
			for (int j = 0; j<quantity; j++){
				Map <Integer, PersonInHousehold> persons = new HashMap <Integer, PersonInHousehold>();
				
				int householdId = householdIdCounter;
				int numberOfAdults = 1;
				int householdStructure = 1;
				// int personId = householdId;
				
				int homeTSZLocation;
				
				int locationOfWork1;
				// Gemeindeschluessel "11000000" = Berlin
				if (source == 11000000){
					homeTSZLocation = getRandomLor();
				} else {
					homeTSZLocation = source;
				}
								
				if (sink == 11000000){
					locationOfWork1 = getRandomLor();
				} else {
					locationOfWork1 = sink;
				}
				int age1 = getRandomAge();
				int personId1 = 1;
				int employed1 = 1;
				PersonInHousehold person1 = new PersonInHousehold(personId1, householdId, employed1, locationOfWork1, age1);
				persons.put(personId1, person1);
				
				// generate a second, non-working person to a given household with a chance of 50%				
//				if (random.nextDouble() < 0.5) {
//					int personId2 = 2;
//					int employed2 = 0;
//					int locationOfWork2 = -99;
//					int age2 = getRandomAge();
//					PersonInHousehold person2 = new PersonInHousehold(personId2, householdId, employed2, locationOfWork2, age2);
//					persons.put(personId2, person2);
//					
//					numberOfAdults = 2;
//					householdStructure = 9;
//				}
							
				HouseholdWithPersons household = new HouseholdWithPersons(householdId, homeTSZLocation, numberOfAdults, householdStructure, persons);
				this.households.put(householdId, household);
				
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
    		    		    		
    		for (HouseholdWithPersons household : this.households.values()) {
    			for (PersonInHousehold person : household.getPersons().values()) {
    				int householdId = household.getHouseholdId();
    				int personId = person.getPersonId();
	    			int employed = person.getEmployed();
	    			int student = person.getStudent();
	    			int driversLicence = person.getDriversLicence();
	    			int locationOfWork = person.getLocationOfWork();
	    			int locationOfSchool = person.getLocationOfSchool();
	    			int female = person.getFemale();
	    			int age = person.getAge();
	    			int parent = person.getParent();
	    			
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
	
	
	public void writeToHouseholdsFile() {
		BufferedWriter bufferedWriterHouseholds = null;
		
		try {
            File householdsFile = new File(this.outputFileHouseholds);
    		FileWriter fileWriterHouseholds = new FileWriter(householdsFile);
    		bufferedWriterHouseholds = new BufferedWriter(fileWriterHouseholds);

    		// int householdIdFromPersonBefore = 0;
    		
    		// use map of persons to write a household for every person
    		// under the condition that the household does not already exist (written from another persons)
    		// this procedure is used to enable the potential use of multiple-person households
    		
    		// for (Integer key : this.persons.keySet()) {
    		// for (String key : this.persons.keySet()) {
    		for (HouseholdWithPersons household : this.households.values()) {
    			int householdId = household.getHouseholdId();
    			
    			// if (householdId != householdIdFromPersonBefore) {
    			// if (!this.householdsAlreadyIncluded.contains(householdId)) {
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
	    			
	    			// householdIdFromPersonBefore = householdId;
	    			// this.householdsAlreadyIncluded.add(householdId);
    			// }
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