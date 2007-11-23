/* *********************************************************************** *
 * project: org.matsim.*
 * DatapulsPopulationGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.ktiProject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

/**
 * Generates a Plans object (i.e. a set of persons with some attributes like age, sex etc.)
 * out of the given tab-separated {@code populationFilename} and {@code buildingsFilename} which
 * contains the data of Mappuls.
 * A person's home location is added to the person's activity plan as an activity of type <i>"h"</i>.
 * @author lnicolas
 */
public class DatapulsPopulationGenerator {

	String populationFilename	= "";
	int populationFileLineCount = -1;
	String buildingsFilename	= "";
	int buildingsFileLineCount  = -1;
	String censuspopFilename	= "";
	int censuspopFileLineCount  = -1;
	
	TreeMap<String, BuildingInformation> buildingsInformation;
	TreeMap<String, HouseholdInformation> householdsInformation;
	ArrayList<PersonInformation> personsInformation;

	TreeMap<String, ArrayList<String>> householdsPerBuilding;
	
	TreeMap<Integer, Integer> censusPersonsPerAge;
	private TreeMap<Integer, Integer> censusEmployeesPerAge;
	
	ArrayList<Person> persons;
	ArrayList<HouseholdI> households;
	
	final int censusEmployedPersonCount = 3836312;
	
	public DatapulsPopulationGenerator(String inputFolder) {
		householdsPerBuilding = new TreeMap<String, ArrayList<String>>();
		
		buildingsInformation = new TreeMap<String, BuildingInformation>();
		householdsInformation = new TreeMap<String, HouseholdInformation>();
		personsInformation = new ArrayList<PersonInformation>();
		
		censusPersonsPerAge = new TreeMap<Integer, Integer>();
		censusEmployeesPerAge = new TreeMap<Integer, Integer>();
		
		persons = new ArrayList<Person>();
		households = new ArrayList<HouseholdI>();
		
		populationFilename	= inputFolder + "cdb_kti_2006.txt";
		populationFileLineCount = getLineCount(populationFilename);
		buildingsFilename	= inputFolder + "lig_kti_2006.txt";
		buildingsFileLineCount  = getLineCount(buildingsFilename);
		censuspopFilename	= inputFolder + "ETHZ_Pers.tab";
		censuspopFileLineCount  = getLineCount(censuspopFilename);
	}
	
	public Plans run() {
		System.out.println("reading building information...");
		createBuildings();
		System.out.println("done.");
		
		System.out.println("createPersonsInformation ...");
		createPersonsInformation();
		System.out.println("done.");
		
		System.out.println("reading census population...");
		processCensusData();
		System.out.println("done.");
		
		System.out.println("upSamplePersons...");
		upSamplePersons(censuspopFileLineCount);
		System.out.println("done.");
		
		System.out.println("creating persons...");
		Plans plans = createPlans();
		System.out.println("done.");
		
		return plans;
	}
	
	public Plans runWithoutUpsampling() {
		System.out.println("reading building information...");
		createBuildings();
		System.out.println("done.");
		
		System.out.println("createPersonsInformation ...");
		createPersonsInformation();
		System.out.println("done.");
		
		int noEmployeesCnt = 0;
		int noPersonsCnt = 0;
		for (BuildingInformation bInfo : buildingsInformation.values()) {
			if (bInfo.getEmployedPersonCount() <= 0) {
				noEmployeesCnt++;
			}
			if (bInfo.getPersonCount() <= 0) {
				noPersonsCnt++;
			}
		}
		System.out.println(noEmployeesCnt + " out of " + buildingsInformation.size() +
				" buildings contain no employed persons, " + noPersonsCnt
				+ " buildings contain no persons at all.");
		
		System.out.println("creating persons...");
		Plans plans = createPlans();
		System.out.println("done.");
		
		return plans;
	}
	
	public static int getLineCount(String filename) {
		int fileLineCount = -1;
		try {
			RandomAccessFile randFile = new RandomAccessFile(filename, "r");
			long lastRec = randFile.length();
			randFile.close();
			FileReader fileRead = new FileReader(filename);
			LineNumberReader lineRead = new LineNumberReader(fileRead);
			lineRead.skip(lastRec);
			fileLineCount = lineRead.getLineNumber() - 1;
			fileRead.close();
			lineRead.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return fileLineCount;
	}

	private void createPersonsInformation() {
		TreeMap<String, Integer> currEmplPersPerBuil = new TreeMap<String, Integer>();
		TreeMap<String, Integer> currForeignersPerBuilding = new TreeMap<String, Integer>();
		
		int employedCount = 0;
		int invalidSexCount = 0;
		int invalidAgeCount = 0;
		int invalidNationalityCount = 0;
		int i = 0;
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		try {
			FileReader fileReader = new FileReader(populationFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				String buildingID = entries[19].trim();
				if (buildingsInformation.containsKey(buildingID)) {
					int age = getAge(entries[9].trim());
					
					if (age >= 0) {
						boolean employed = getEmployed(buildingID,
							currEmplPersPerBuil, buildingsInformation, age);
					
						// get sex
						String sex = getSex(entries[12].trim());
						int sexIndex = getSexIndex(entries[12].trim());
						if (sexIndex == 0) {
							invalidSexCount++;
						}
						
						// Get nationality
						String nationality = getNationality(buildingID, buildingsInformation,
								currForeignersPerBuilding);
						if (nationality == null) {
							invalidNationalityCount++;
						}
						
						// process employed flag
						if (employed == true) {
							employedCount++;
						}
	
						String householdID = entries[13].trim();
					
						HouseholdInformation hInfo = householdsInformation.get(
								householdID + buildingID);
						if (hInfo == null) {
							hInfo = new HouseholdInformation();
							householdsInformation.put(householdID + buildingID, hInfo);
						}
						hInfo.addPerson();
						if (age < 18) {
							hInfo.addKid();
						}
						personsInformation.add(new PersonInformation(age, sex,
								employed, nationality, buildingsInformation.get(buildingID), hInfo));
					} else if (age < 0) {
						invalidAgeCount++;
					}
					
					i++;
					if (i % (populationFileLineCount / statusString.length()) == 0) {
						System.out.print(".");
						System.out.flush();
					}
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		System.out.println(personsInformation.size() + " persons");
		System.out.println(invalidSexCount + " persons are of unknown gender");
		System.out.println(invalidAgeCount + " persons are of unknown age (< 0)");
		System.out.println(invalidNationalityCount + " persons are of unknown nationality");
	}

	private Plans createPlans() {
		Plans plans = new Plans();

		System.out.println("Creating persons...");
		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		int personID = 0;
		for (PersonInformation pInfo : personsInformation) {
			BuildingInformation bInfo = pInfo.getBuildingInformation();
			HouseholdInformation hInfo = pInfo.getHouseholdInformation();

			String employed = "yes";
			if (pInfo.getIsEmployed() == false) {
				employed = "no";
			}
			Person person = new Person(Integer.toString(personID), pInfo.getSex(),
					Integer.toString(pInfo.getAge()), null, null, employed);
			person.setNationality(pInfo.getNationality());
//					pInfo.getNationality(), Double.toString(bInfo.getBuyingValuePerHH()),
//					Integer.toString(hInfo.getPersonCount()), Integer.toString(hInfo.getKidCount()));
			personID++;

			Act act = new Act("h", bInfo.getXCoord(), bInfo.getYCoord(), null, 0, 30, 30, true);
			Plan plan = new Plan(person);
			plan.addAct(act);
			person.addPlan(plan);
			try {
				plans.addPerson(person);
			} catch (Exception e) {
				Gbl.errorMsg(e);
			}
			persons.add(person);
			households.add(hInfo);

			if (personID % (personsInformation.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		
		System.out.println("done.");

		return plans;
	}

	private void upSamplePersons(int popSize) {
		double additionalPersonsFraction = (double)popSize / personsInformation.size();
		
		TreeMap<Integer, Integer> currentPersonCount = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> currentEmployeeCount = new TreeMap<Integer, Integer>();
		// init currentAgeCount
		int foreignerCnt = 0;
		int emplPersCnt = 0;
		for (PersonInformation pInfo : personsInformation) {
			int age = pInfo.getAge();
			int count = 0;
			if (currentPersonCount.containsKey(age)) {
				count = currentPersonCount.get(age);
			}
			currentPersonCount.put(age, count + 1);
			boolean employed = pInfo.getIsEmployed();
			if (employed) {
				count = 0;
				if (currentEmployeeCount.containsKey(age)) {
					count = currentEmployeeCount.get(age);
				}
				currentEmployeeCount.put(age, count + 1);
				emplPersCnt++;
			}
			if (pInfo.getIsForeigner()) {
				foreignerCnt++;
			}
		}
		ArrayList<Integer> missingAges = new ArrayList<Integer>(currentPersonCount.keySet());
		int newForeignerCnt = (int)(foreignerCnt * additionalPersonsFraction);
		
		System.out.println("Before upsamling: " + emplPersCnt + " of " + popSize + " persons work ("
				+ (double)emplPersCnt/popSize + "%)");
		
		ArrayList<Double> householdSizeDistribution = 
			getHouseholdSizeDistribution(householdsInformation);
		TreeMap<String, HouseholdInformation> newHouseholdsPerBuilding =
			new TreeMap<String, HouseholdInformation>();
		TreeMap<String, Integer> newHouseholdSizes = new TreeMap<String, Integer>();
		
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		ArrayList<String> buildingArray = new ArrayList<String>(buildingsInformation.keySet());
		int additionalPersonCount = popSize - personsInformation.size(); 
		double avgGenAge = 0;
		int maxGenAge = 0;
		for (int i = personsInformation.size(); i < popSize; i++) {
			// select a random building and add a person to it
			String buildingID = buildingArray.get(Gbl.random.nextInt(buildingArray.size()));
			BuildingInformation bInfo = buildingsInformation.get(buildingID);
			bInfo.addPerson();
			
			String nationality = "swiss";
			// add a foreigner to the building, if needed
			if (foreignerCnt < newForeignerCnt) {
				nationality = "other";
				foreignerCnt++;
			}
			String householdID = null;
			// select the new household (or create a new household) in this building 
			// and add a person to it
			HouseholdInformation hInfo = newHouseholdsPerBuilding.get(buildingID);
			if (hInfo == null || newHouseholdSizes.get(buildingID) == hInfo.getPersonCount()) {
				// Create a new household and add it to the building
				boolean hhIDExists = true;
				int tmpCnt = 0;
				ArrayList<String> hhArray = householdsPerBuilding.get(buildingID);
				// Create a Household-ID
				while (hhIDExists == true) {
					householdID = Integer.toString(tmpCnt);
					tmpCnt++;
					hhIDExists = false;
					for (String tmpHHID : hhArray) {
						if (tmpHHID.equals(householdID + buildingID) == true) {
							hhIDExists = true;
						}
					}
				}
				hInfo = new HouseholdInformation();
				householdsInformation.put(householdID + buildingID, hInfo);
				bInfo.addHousehold();
				
				hhArray.add(householdID + buildingID);
				
				newHouseholdSizes.put(buildingID, generateHouseholdSize(householdSizeDistribution));
				newHouseholdsPerBuilding.put(buildingID, hInfo);
			}
			hInfo.addPerson();
			
			// create personInformation
			int age = generateAge(currentPersonCount, missingAges);
			if (age < 18) {
				hInfo.addKid();
			}
			avgGenAge = (avgGenAge*(i - personsInformation.size()) + age) /
				(i - personsInformation.size() + 1);
			if (age > maxGenAge) {
				maxGenAge = age;
			}
			boolean employed = generateEmployed(currentEmployeeCount, age);
			if (employed) {
				emplPersCnt++;
			}
			
			String sex = generateSex();
			personsInformation.add(new PersonInformation(age, sex, employed, nationality, 
					bInfo, hInfo));
			
			if (i % (additionalPersonCount / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}

		System.out.println();
		System.out.println("maxGenAge: " + maxGenAge + "avgGenAge: " + avgGenAge);
		System.out.println("additionalPersonsFraction =  " + additionalPersonsFraction);
		System.out.println(emplPersCnt + " of " + popSize + " persons work ("
				+ (double)emplPersCnt/popSize + "%)");
		System.out.println("Currently " + foreignerCnt + " of " + 
				popSize + " persons are foreigners");
		
		upSampleRetirees(emplPersCnt, currentEmployeeCount);
	}

	private void upSampleRetirees(int emplPersCnt, TreeMap<Integer, Integer> currentEmployeeCount) {
		System.out.print("Upsampling retirees..."); System.out.flush();
		// Get nof employed people over 64
		int emplRetirCount = 0;
		for (PersonInformation pInfo : personsInformation) {
			if (pInfo.getAge() >= 65 && pInfo.getIsEmployed()) {
				emplRetirCount++;
			}
		}
		// Get nof employed people over 64 in census data
		int censusEmplRetirCount = 0;
		for (int age = 65; age < 125; age++) {
			if (censusEmployeesPerAge.containsKey(age)) {
				censusEmplRetirCount += censusEmployeesPerAge.get(age);
			}
		}
		// Scale down nof employed persons over 64 until we reach the same total nof employed persons
		// as in the census data
		Iterator<PersonInformation> pIter = personsInformation.iterator();
		int newRetCnt = 0;
		while (emplPersCnt > censusEmployedPersonCount
				&& emplRetirCount > censusEmplRetirCount
				&& pIter.hasNext()) {
			PersonInformation pInfo = pIter.next();
			if (pInfo.getAge() >= 65 && pInfo.getIsEmployed()) {
				pInfo.setIsEmployed(false);
				emplRetirCount--;
				emplPersCnt--;
				newRetCnt++;
			}
		}
		System.out.println("done (" + newRetCnt + " new retirees (" + emplRetirCount +
				" work, total " + emplPersCnt + ")");
	}

	private int generateHouseholdSize(ArrayList<Double> householdSizeDistribution) {
		double r = Gbl.random.nextDouble() * 100;
		int hhSize = 0;
		while (r >= householdSizeDistribution.get(hhSize)) {
			hhSize++;
		}
		if (hhSize == 0) {
			hhSize++;
		}
		return hhSize;
	}

	private ArrayList<Double> getHouseholdSizeDistribution(
			TreeMap<String, HouseholdInformation> hInfos) {
		TreeMap<Integer, Integer> householdsPerSize = new TreeMap<Integer, Integer>();
		int householdCount = 0;
		int maxHouseholdSize = 0;

		Iterator<Entry<String, HouseholdInformation>> it = hInfos.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, HouseholdInformation> entry = it.next();

			int hSize = entry.getValue().getPersonCount();
			int hSizeCount = 0;
			if (householdsPerSize.containsKey(hSize)) {
				hSizeCount = householdsPerSize.get(hSize);
			}
			householdsPerSize.put(hSize, hSizeCount + 1);
			if (hSize > maxHouseholdSize) {
				maxHouseholdSize = hSize;
			}
			householdCount++;
		}
		
		ArrayList<Double> hSizeDistr = new ArrayList<Double>();
		// create household size distribution
		double householdFrac = 0;
		for (int i = 0; i <= maxHouseholdSize; i++) {
			if (householdsPerSize.containsKey(i)) {
				householdFrac += (100 * householdsPerSize.get(i)) / (double)householdCount;
			}
			hSizeDistr.add(householdFrac);
		}
		
		return hSizeDistr;
	}

	private String generateSex() {
		String sex = "f";
		// generate sex
		if (Gbl.random.nextDouble() < 0.5) {
			sex = "m";
		}
		return sex;
	}

	private String getNationality(String buildingID,
			TreeMap<String, BuildingInformation> buildingsInformation,
			TreeMap<String, Integer> currForeignersPerBuilding) {
		String nationality = null;
		BuildingInformation bInfo = buildingsInformation.get(buildingID);
		int curForeignerCount = 0;
		if (currForeignersPerBuilding.containsKey(buildingID)) {
			curForeignerCount = currForeignersPerBuilding.get(buildingID);
		}
		if (bInfo != null) {
			if (curForeignerCount < bInfo.getForeignerCount()) {
				currForeignersPerBuilding.put(buildingID, curForeignerCount + 1);
				nationality = "other";
			} else {
				nationality = "swiss";
			}
		}
		
		return nationality;
	}

	private String getSex(String string) {
		int sex = getSexIndex(string);

		if (sex == 1) {
			string = "m";
		} else if (sex == 2) {
			string = "f";
		} else if (sex == 0) {
			if (Gbl.random.nextDouble() < 0.5) {
				string = "m";
			} else {
				string = "f";
			}
		}
		return string;
	}

	private int getSexIndex(String string) {
		int sex;
		if (string.equals("")) {
			sex = 0;
		} else {
			sex = Integer.parseInt(string);
		}
		return sex;
	}

	private int generateAge(TreeMap<Integer, Integer> currentAgeCount,
			ArrayList<Integer> missingAges) {
		
		int ageIndex = Gbl.random.nextInt(missingAges.size());
		int age = missingAges.get(ageIndex);
		int curAgeCnt = 0;
		if (currentAgeCount.containsKey(age)) {
			curAgeCnt = currentAgeCount.get(age);
		}
		curAgeCnt++;
		currentAgeCount.put(age, curAgeCnt);
		int maxAgeCnt = 0;
		if (censusPersonsPerAge.containsKey(age)) {
			maxAgeCnt = censusPersonsPerAge.get(age);
		}
		if (curAgeCnt >= maxAgeCnt) {
			missingAges.remove(ageIndex);
		}
		return age;
	}

	private boolean generateEmployed(TreeMap<Integer, Integer> currentEmployeeCountPerAge,
			int age) {
		int emplCount = 0;
		if (censusEmployeesPerAge.containsKey(age)) {
			emplCount = censusEmployeesPerAge.get(age);
		}
		int curEmplCnt = 0;
		if (currentEmployeeCountPerAge.containsKey(age)) {
			curEmplCnt = currentEmployeeCountPerAge.get(age);
		}
		if (curEmplCnt < emplCount) {
			curEmplCnt++;
			currentEmployeeCountPerAge.put(age, curEmplCnt);
			return true;
		}
		
		return false;
	}
	
	private Boolean getEmployed(String buildingID,
			TreeMap<String, Integer> currEmplPersPerBuil,
			TreeMap<String, BuildingInformation> buildingsInformation, int age) {
		int curEmplPersCount = 0;
		if (currEmplPersPerBuil.containsKey(buildingID)) {
			curEmplPersCount = currEmplPersPerBuil.get(buildingID);
		}
		boolean employed = false;
		if (buildingsInformation.containsKey(buildingID) == false) {
			return null;
		} else if ((age >= 16 || age == -1) 
			&& curEmplPersCount < buildingsInformation.get(buildingID).getEmployedPersonCount()) {
			currEmplPersPerBuil.put(buildingID, curEmplPersCount + 1);
			employed = true;
		}
		return employed;
	}

	/**
	 * @param string
	 * @return The age represented by string or -1, if string contains no valid age.
	 */
	private int getAge(String string) {
		// set age to undefined
		int age = -1;
		if (string.equals("") == false) {
			age = Integer.parseInt(string);
		}
		
		age = 2006 - age;
		if (age < 0 || age > 125) {
			age = -1;
		}
		
		return age;
	}

	/**
	 * reads in the number of persons per building and the household sizes 
	 * based on the populationfilename file
	 */
	void getNofPersonsPerBuilding() {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		try {
			FileReader fileReader = new FileReader(populationFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String currentLine;
			int lineCount = populationFileLineCount;
			int i = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				
				String buildingID = entries[19].trim();
				if (buildingsInformation.containsKey(buildingID)) {
					BuildingInformation bInfo = buildingsInformation.get(buildingID);
					bInfo.addPerson();
					String householdID = entries[13].trim();
					ArrayList<String> householdIDs = householdsPerBuilding.get(buildingID);
					if (householdIDs == null) {
						householdIDs = new ArrayList<String>();
						householdsPerBuilding.put(buildingID, householdIDs);
					}
					householdIDs.add(householdID);
				}
				
				i++;
				if (i % (lineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	void processCensusData() {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		try {
			FileReader fileReader = new FileReader(censuspopFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String currentLine;
			int lineIndex = 0;
			int lineCount = censuspopFileLineCount;
			// Skip the first line
			currentLine = bufferedReader.readLine();
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				
				int age    = Integer.parseInt(entries[16].trim());
				if (age < 0 || age > 120) {
					Gbl.errorMsg("Line " + lineIndex + ": wrong format of age=" + age + "!");
				}
				int count = 0;
				if (censusPersonsPerAge.containsKey(age)) {
					count = censusPersonsPerAge.get(age);
				}
				censusPersonsPerAge.put(age, count + 1);
				
				int employedIndex    = Integer.parseInt(entries[88].trim());
				boolean employed = false;
				if (employedIndex >= 11 && employedIndex <= 14) {
					employed = true;
				} else if (employedIndex == 20 ||
						(employedIndex >= 31 && employedIndex <= 35) ||
						employedIndex == 4 || employedIndex == 40) {
					employed = false;
				} else {
					Gbl.errorMsg("Line $.: wrong format of employed="
							+ entries[36] + "!");
				}
				if (employed) {
					count = 0;
					if (censusEmployeesPerAge.containsKey(age)) {
						count = censusEmployeesPerAge.get(age);
					}
					censusEmployeesPerAge.put(age, count + 1);
				}
				
				lineIndex++;
				if (lineIndex % (lineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
//		TreeMap<Integer, Integer> employedPersonsPerAge = new TreeMap<Integer, Integer>();
//		int employedPersonsCount = 0;
//		TreeMap<Integer, Integer> nonEmployedPersonsPerAge = new TreeMap<Integer, Integer>();
//		int nonEmployedPersonsCount = 0;
//		
//		String statusString = "|----------+-----------|";
//		System.out.println(statusString);
//		
//		try {
//			FileReader fileReader = new FileReader(censuspopFilename);
//			BufferedReader bufferedReader = new BufferedReader(fileReader);
//			String currentLine;
//			int lineIndex = 0;
//			int lineCount = censuspopFileLineCount;
//			// Skip the first line
//			currentLine = bufferedReader.readLine();
//			while ((currentLine = bufferedReader.readLine()) != null) {
//				String[] entries = currentLine.split("\t", -1);
//				lineIndex++;
//				
//				int age    = Integer.parseInt(entries[16].trim());
//				if (age < 0 || age > 120) {
//					Gbl.errorMsg("Line " + lineIndex + ": wrong format of age=" + age + "!");
//				}
//				
//				int employed = Integer.parseInt(entries[88].trim());
//
//				int currentPersonsCount = 1; 
//				if (employed >= 11 && employed <= 14) {
//					if (employedPersonsPerAge.containsKey(age)) {
//						currentPersonsCount = employedPersonsPerAge.get(age) + 1;
//					}
//					employedPersonsPerAge.put(age, currentPersonsCount);
//					employedPersonsCount++;
//				} else if (employed == 20 || (employed >= 31 && employed <= 35)
//						|| employed == 4 || employed == 40) {
//					if (nonEmployedPersonsPerAge.containsKey(age)) {
//							currentPersonsCount = nonEmployedPersonsPerAge.get(age) + 1;
//					}
//					nonEmployedPersonsPerAge.put(age, currentPersonsCount);
//					nonEmployedPersonsCount++;
//				} else {
//					Gbl.errorMsg("Line " + lineIndex + ": wrong format of employed=" + employed + "!");
//				}
//				
//				lineIndex++;
//				if (lineIndex % (lineCount / statusString.length()) == 0) {
//					System.out.print(".");
//					System.out.flush();
//				}
//			}
//			
//			bufferedReader.close();
//		} catch (IOException e) {
//			Gbl.errorMsg(e);
//		}
//		
//		// create age distribution, both for employed and non-employed persons
//		double procEmplPersFrac = 0;
//		double procNonEmplPersFrac = 0;
//		for (int i = 0; i < 125; i++) {
//			if (employedPersonsPerAge.containsKey(i)) {
//				procEmplPersFrac +=
//					(100 * employedPersonsPerAge.get(i)) /
//					(double)employedPersonsCount;
//			}
//			censusEmplAgeDistr.add(procEmplPersFrac);
//			if (nonEmployedPersonsPerAge.containsKey(i)) {
//				procNonEmplPersFrac += (100 * nonEmployedPersonsPerAge.get(i))
//				/ (double)nonEmployedPersonsCount;
//			}
//			censusNonEmplAgeDistr.add(procNonEmplPersFrac);
//		}
	
	/**
	 * reads in the number of employed persons per building based on the buildingsfilename file
	 */
	void createBuildings() {

		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		try {
			FileReader fileReader = new FileReader(buildingsFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			int lineCount = buildingsFileLineCount;
			String currentLine;
			int totalPersonCount = 0;
			int invalidBuyingPowerCount = 0;
			int noBuyingPowerCount = 0;
			int noHouseholdCount = 0;
			int buildingCount = 0;
			int noEmployeesInBuildingCount = 0;
			int lineIndex = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				
				String[] entries = currentLine.split("\t", -1);
				
				String buildingID = entries[39].trim();
				
				// Get the buying power per House hold and building
				// process it only if there is a valid buying power 
				// associated to the building
				int buyingPower = Integer.parseInt(entries[10].trim());
				int householdCount = Integer.parseInt(entries[5].trim());
				
				if (buyingPower < 0) {
					buyingPower = Integer.MIN_VALUE;
					invalidBuyingPowerCount++;
				} else if (householdCount <= 0) {
					noHouseholdCount++;
				} else {
					if (buyingPower == 0) {
						noBuyingPowerCount++;
					}
				
					buildingCount++;
					
					int personCount = Integer.parseInt(entries[4].trim());
					double employedFraction   = Double.parseDouble(entries[21].trim());
					double alienFraction   = Double.parseDouble(entries[8].trim());
					int xCoord   = Integer.parseInt(entries[2].trim());
					int yCoord   = Integer.parseInt(entries[3].trim());
					
					if (employedFraction <= 0) {
						noEmployeesInBuildingCount++;
					}
					
					// 1.7 ist der Faktor um den Nettolohn II in den Bruttolohn umzurechnen (Pi*Daumen)
					buildingsInformation.put(buildingID, new BuildingInformation(householdCount,
							buyingPower * 1.7 * 1000, employedFraction, alienFraction, xCoord, yCoord));
	
					totalPersonCount += personCount;
	
				}

				lineIndex++;
				if (lineIndex % (lineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			bufferedReader.close();
		
			System.out.println(buildingCount + " buildings");
			System.out.println(noBuyingPowerCount + " have no buying power," +
				invalidBuyingPowerCount + " have invalid buying power," +
				noHouseholdCount + " have no households, in " +
				noEmployeesInBuildingCount + " buildings there are no employees.");
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		getNofPersonsPerBuilding();

	}
	
	class PersonInformation {
		
		private int age;
		private String sex;
		private boolean employed;
		private String nationality;
		private BuildingInformation bInfo;
		private HouseholdInformation hInfo;
		
		/**
		 * @param id
		 * @param age
		 * @param sex
		 * @param employed
		 * @param nationality
		 * @param buildingID
		 * @param householdID
		 */
		public PersonInformation(int age, String sex, boolean employed,
				String nationality, BuildingInformation bInfo, HouseholdInformation hInfo) {
			super();
			this.age = age;
			this.sex = sex;
			this.employed = employed;
			this.nationality = nationality;
			this.bInfo = bInfo;
			this.hInfo = hInfo;
		}
		
		public void setIsEmployed(boolean employed) {
			this.employed = employed;
		}

		public String getNationality() {
			return nationality;
		}
		
		public String getSex() {
			return sex;
		}
		
		public int getAge() {
			return age;
		}
		
		public HouseholdInformation getHouseholdInformation() {
			return hInfo;
		}
		
		public BuildingInformation getBuildingInformation() {
			return bInfo;
		}
		
		public boolean getIsForeigner() {
			return (nationality.equals("swiss") == false);
		}
		
		public boolean getIsEmployed() {
			return employed;
		}
	}
	
	class HouseholdInformation implements HouseholdI {
		private int personCount;
		private int kidCount;
		private double income;
		
		/**
		 * @param personIDs
		 * @param id
		 * @param buildingID
		 */
		public HouseholdInformation() {
			super();
			personCount = 0;
			kidCount = 0;
//			this.buildingID = buildingID;
		}
		
		public int getPersonCount() {
			return personCount;
		}
		
		public int getKidCount() {
			return kidCount;
		}

		public void addPerson() {
			personCount++;
		}
		
		public void addKid() {
			kidCount++;
		}
		
		public double getIncome() {
			return income;
		}
		
		public void setIncome(double income) {
			this.income = income;
		}
	}
	
	class BuildingInformation {
		private int personCount;
		private int householdCount;
		private double buyingPower;
		private double employedFraction;
		private double foreignerFraction;
		private int xCoord;
		private int yCoord;
		
		public BuildingInformation(int householdCount, double buyingPower,
				double employedFraction, double alienFraction, int xCoord, int yCoord) {
			super();
			this.personCount = 0;
			this.householdCount = householdCount;
			this.buyingPower = buyingPower;
			this.employedFraction = employedFraction;
			this.foreignerFraction = alienFraction;
			this.xCoord = xCoord;
			this.yCoord = yCoord;
		}
		
		public double getBuyingValuePerHH() {
			return buyingPower / householdCount;
		}

		public int getForeignerCount() {
			return (int)Math.round(personCount * foreignerFraction);
		}

		public void addPerson() {
			this.personCount++;
		}
		/**
		 * @return the alienFraction
		 */
		public double getForeignerFraction() {
			return foreignerFraction;
		}
		/**
		 * @return the buyingPower
		 */
		public double getBuyingPower() {
			return buyingPower;
		}
		/**
		 * @return the number of employed persons in this building
		 */
		public int getEmployedPersonCount() {
			int emplCnt = (int)Math.round(personCount * employedFraction);
//			if (emplCnt <= 0 && employedFraction > 0) {
//				emplCnt = 1;
//			}
			return emplCnt;
		}
		/**
		 * @return the householdCount
		 */
		public int getHouseholdCount() {
			return householdCount;
		}
		/**
		 * @return the householdCount
		 */
		public void addHousehold() {
			// Increment the buyingPower, such that the buyingPower per household remains the same
			buyingPower += (double)buyingPower / householdCount;
			householdCount++;
		}

		/**
		 * @return the personCount
		 */
		public int getPersonCount() {
			return personCount;
		}
		/**
		 * @return the xCoord
		 */
		public int getXCoord() {
			return xCoord;
		}
		/**
		 * @return the yCoord
		 */
		public int getYCoord() {
			return yCoord;
		}
	}

	public ArrayList<Person> getPersons() {
		return persons;
	}

	public ArrayList<HouseholdI> getHouseholds() {
		return households;
	}
}
