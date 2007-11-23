/* *********************************************************************** *
 * project: org.matsim.*
 * CensusPopulationGenerator.java
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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.world.Coord;

/**
 * Generates a Plans object (i.e. a set of persons with some attributes like age, sex etc.)
 * out of the given tab-separated {@code populationFilename} which
 * contains the data of the census of population of Switzerland of the year 2000.
 * A person's home location is added to the person's activity plan as an activity of type <i>"h"</i>.
 * @author lnicolas
 */
public class CensusPopulationGenerator {
	static String populationFilename	= "";
	static int populationFileLineCount = -1;
	
	ArrayList<HouseholdI> households = new ArrayList<HouseholdI>();
	ArrayList<Person> persons = new ArrayList<Person>();
	
	public CensusPopulationGenerator(String inputFolder) {
		populationFilename	= inputFolder + "ETHZ_Pers.tab";
		populationFileLineCount = 
			DatapulsPopulationGenerator.getLineCount(populationFilename);
	}
	
	/**
	 * @return a Plans object (i.e. a set of persons with some attributes like age, sex etc.)
	 * based on the given tab-separated {@code populationFilename}. 
	 */
	public Plans run() {
		
		System.out.println("creating household information...");
		getHouseholdInformation();
		System.out.println("done.");
		
		System.out.println("creating persons...");
		Plans plans = createPlans();
		System.out.println("done.");
		
		return plans;
	}
	
	/**
	 * Each person in the data is associated to a household ID. Out of these IDs, we can 
	 * build HouseholdInformation object which contain the persons within the respective household.
	 * The HouseholdInformation objects in turn can then be used to count the number of 
	 * household members and the number of kids per household and the household income for each person.  
	 */
	private void getHouseholdInformation() {
		TreeMap<String, HouseholdInformation> householdsInformation =
			new TreeMap<String, HouseholdInformation>();
		
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		int invalidHouseholdTypeCount = 0;
		try {
	// how the input file looks like:
	//
	// head:    KANT    ZGDE    GEBAEUDE_ID     HHNR    WOHNUNG_NR      PERSON_ID       ZKRS    GLOC2   GLOC3   GLOC4   WKAT    GEM2    PARTNR  GETG    GEMT    GEJA    ALTJ    AKL5    AGRP    VALTJ  GORT    GORTCH  GORTAUS GESL    ZIVL    ZIVJ    HMAT    CHJA    ZNAT    NATI    NATUNO  AUSW    WO5M    WO5CH   WO5AUS  ELTERN  ZKIND   GJKIND_1   GJKIND_2        GJKIND_3        GJKIND_4        GJKIND_L        GJKIND_J        STHHZ   STHHW   RPHHZ   RPHHW   EPNRZ   EPNRW   HHTPZ   HHTPW   APERZ   APERW   WKATA   SPRA    MSPR    HSPR    SHSD    SHHD    SHPR    SHFR    SHTB    SHIT    SHRR    SHEN    SHAN    BSPR    SBSD    SBHD    SBPR    SBFR    SBTB    SBIT    SBRR    SBEN    SBAN    GEGW    HABG    UHAB    FHAB    HFAB    HBAB    LSAB    MPAB    BLAB    BSAB    OSAB    KAUS    AMS     BGRAD   KAZEIT  ERWS    MAMS    KAMS    VOLL    TZT1    TZT2    ETOA    ARLO    STSU    KSTZ    NENSS   IAUS    RENT    HAFA    FRTA    HVOLL   HTZS    HAZEIT  HIAUS   HHAFA   HFRTA   ERBE    PBER    ISCO    SOPK    BETGR   UNTGR   ANOGA   AREFO   AGDE    AZKRS   AGLOC2  AGLOC3  AGLOC4  AORT    ADIST   APEND   AWMIN   AWOFT   AWTAGE  AVEMI   AWEGM   AVMKE   AVELO   AMOFA   AMRAD   APKWL   APKWM   AWBUS   ABAHN   ATRAM   APOST   AVAND   SGDE    SZKRS   SGLOC2  SGLOC3  SGLOC4  SORT    SDIST   SPEND   SWMIN   SWOFT   SWTAGE  SVEMI   SWEGM   SVMKE   SVELO   SMOFA   SMRAD   SPKWL   SPKWM   SSBUS   SBAHN   STRAM   SPOST   SVAND   SNOGA   SREFO   XACH    YACH
	// example: 12      2701    1030475         1943680 1       5       -7      11              112             0               1       -9      -9      4       12      1974    26      25      25      26     4       3946    -8      2       1       -9      1       -8      -7      8100    1       -9      2       2701    -8      0       -8      -8         -8      -8      -8      -8      -8      111     111     1       1       0       0       1000    1000    1       1       1       110     1111    200     1       0       0       0       0       0       0       0       0       400     1       1       0       0       0       0       0       0       0       -8      31      0       0       0       1       0       0       0       1       1       0       11      11      6       124     10      1       1       0       0       0       0       0       0       0       0       0       1       1       43.0    -8      43.0    -8      6       6       86504   -7      -7      91      10      10      8511A   21      2701    2109    21                                  1       0       1       -7      1       5       390     0       0       1       0       0       0       0       0       0       1       0       0       -8      0                                                       -8      0       -8      -8      -8      -8      -8      0       -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      610100  268000
	// index:   0          1          2          3      4               5               6       7       8       9 ...
			FileReader fileReader = new FileReader(populationFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			// Skip header
			String currentLine = bufferedReader.readLine();
			int lineIndex = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				
				String[] entries = currentLine.split("\t", -1);
				
				// HHNR (the key of the the HouseholdInformation)
				String hhId = entries[3].trim();
				
				HouseholdInformation hInfo = null;
				// balmermi: either "HHTPZ" or "HHTPW" or -1 (no value)
				int hType = getHouseholdType(entries);
				if (hType >= 1000 && hType <= 3222) {
					if (householdsInformation.containsKey(hhId)) {
						hInfo = householdsInformation.get(hhId);
					} else {
						hInfo = new HouseholdInformation(/*hType*/);
						householdsInformation.put(hhId, hInfo);
					}
				} else if (hType == 9121 || hType == 9122 || hType == 9129
						|| (hType > 9150 && hType < 9160)
						|| (hType > 9200 && hType < 9300)
						|| hType == 9804) {
					hInfo = new HouseholdInformation(/*hType*/);
				} else {
//					System.out.println("Invalid household type: " + hType);
					hInfo = null;
					invalidHouseholdTypeCount++;
				}
				if (hInfo != null) {
					String personId = entries[5].trim();
					int age    = Integer.parseInt(entries[16].trim());
					hInfo.addPerson(personId, age);
				}
				households.add(hInfo);
				
				lineIndex++;
				if (lineIndex % (populationFileLineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		System.out.println(invalidHouseholdTypeCount + " persons are linked to households of " +
				"invalid type. Omitting them.");
	}
	
	/**
	 * A helper function that writes out a file with the id, coordinates and number of 
	 * persons of a building. This information was used to enrich the enterprise census data with 
	 * home facilities.
	 * @param filename
	 */
	public static void writeBuildingInformation(String filename) {
		TreeMap<Integer, Integer> personsPerBuilding = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Coord> buildingCoords = new TreeMap<Integer, Coord>();
		
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		try {
	// how the input file looks like:
	//
	// head:    KANT    ZGDE    GEBAEUDE_ID     HHNR    WOHNUNG_NR      PERSON_ID       ZKRS    GLOC2   GLOC3   GLOC4   WKAT    GEM2    PARTNR  GETG    GEMT    GEJA    ALTJ    AKL5    AGRP    VALTJ  GORT    GORTCH  GORTAUS GESL    ZIVL    ZIVJ    HMAT    CHJA    ZNAT    NATI    NATUNO  AUSW    WO5M    WO5CH   WO5AUS  ELTERN  ZKIND   GJKIND_1   GJKIND_2        GJKIND_3        GJKIND_4        GJKIND_L        GJKIND_J        STHHZ   STHHW   RPHHZ   RPHHW   EPNRZ   EPNRW   HHTPZ   HHTPW   APERZ   APERW   WKATA   SPRA    MSPR    HSPR    SHSD    SHHD    SHPR    SHFR    SHTB    SHIT    SHRR    SHEN    SHAN    BSPR    SBSD    SBHD    SBPR    SBFR    SBTB    SBIT    SBRR    SBEN    SBAN    GEGW    HABG    UHAB    FHAB    HFAB    HBAB    LSAB    MPAB    BLAB    BSAB    OSAB    KAUS    AMS     BGRAD   KAZEIT  ERWS    MAMS    KAMS    VOLL    TZT1    TZT2    ETOA    ARLO    STSU    KSTZ    NENSS   IAUS    RENT    HAFA    FRTA    HVOLL   HTZS    HAZEIT  HIAUS   HHAFA   HFRTA   ERBE    PBER    ISCO    SOPK    BETGR   UNTGR   ANOGA   AREFO   AGDE    AZKRS   AGLOC2  AGLOC3  AGLOC4  AORT    ADIST   APEND   AWMIN   AWOFT   AWTAGE  AVEMI   AWEGM   AVMKE   AVELO   AMOFA   AMRAD   APKWL   APKWM   AWBUS   ABAHN   ATRAM   APOST   AVAND   SGDE    SZKRS   SGLOC2  SGLOC3  SGLOC4  SORT    SDIST   SPEND   SWMIN   SWOFT   SWTAGE  SVEMI   SWEGM   SVMKE   SVELO   SMOFA   SMRAD   SPKWL   SPKWM   SSBUS   SBAHN   STRAM   SPOST   SVAND   SNOGA   SREFO   XACH    YACH
	// example: 12      2701    1030475         1943680 1       5       -7      11              112             0               1       -9      -9      4       12      1974    26      25      25      26     4       3946    -8      2       1       -9      1       -8      -7      8100    1       -9      2       2701    -8      0       -8      -8         -8      -8      -8      -8      -8      111     111     1       1       0       0       1000    1000    1       1       1       110     1111    200     1       0       0       0       0       0       0       0       0       400     1       1       0       0       0       0       0       0       0       -8      31      0       0       0       1       0       0       0       1       1       0       11      11      6       124     10      1       1       0       0       0       0       0       0       0       0       0       1       1       43.0    -8      43.0    -8      6       6       86504   -7      -7      91      10      10      8511A   21      2701    2109    21                                  1       0       1       -7      1       5       390     0       0       1       0       0       0       0       0       0       1       0       0       -8      0                                                       -8      0       -8      -8      -8      -8      -8      0       -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      610100  268000
	// index:   0          1          2          3      4               5               6       7       8       9 ...
		
			FileReader fileReader = new FileReader(populationFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			// Skip header
			String currentLine = bufferedReader.readLine();
			int lineIndex = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				
				String[] entries = currentLine.split("\t", -1);
				
//				Integer personId = Integer.parseInt(entries[5].trim());
				Integer buildingId = Integer.parseInt(entries[2].trim());
				int personCount = 0;
				if (personsPerBuilding.containsKey(buildingId)) {
					personCount = personsPerBuilding.get(buildingId);
				} else {
					Double xCoord = Double.parseDouble(entries[170].trim());
					Double yCoord = Double.parseDouble(entries[171].trim());
					buildingCoords.put(buildingId, new Coord(xCoord, yCoord));
				}
				personsPerBuilding.put(buildingId, personCount + 1);
				
				lineIndex++;
				if (lineIndex % (populationFileLineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			
			bufferedReader.close();
			
			System.out.println();
			System.out.println(statusString);
			lineIndex = 0;
			
			BufferedWriter out;
			out = new BufferedWriter(new FileWriter(filename));
			out.write("buildingID\txCoord\tyCoord\tpersonCount\n");
			Coord coord = null;
			for (Entry<Integer, Integer> entry : personsPerBuilding.entrySet()) {
				coord = buildingCoords.get(entry.getKey());
				out.write(entry.getKey() + "\t" + coord.getX() + "\t" + coord.getY()
						+ "\t" + entry.getValue() + "\n");
			}
			
			lineIndex++;
			if (lineIndex % (personsPerBuilding.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
			
			out.close();
			System.out.println("Home facilities written to " + filename);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private int getHouseholdType(String[] entries) {
		int type = Integer.parseInt(entries[49].trim());
		if (type == -9) {
			type = Integer.parseInt(entries[50].trim());
		}
		if (type == -9) {
			type = -1;
		}
		return type;
	}

	/**
	 * Generates Person objects from the {@code populationFilename}.
	 * @return The Plans object containing the Persons generated.
	 */
	private Plans createPlans(/*Double[] kidCountDistr*/) {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		Plans plans = new Plans();
		
		
		try {
	// how the input file looks like:
	//
	// head:    KANT    ZGDE    GEBAEUDE_ID     HHNR    WOHNUNG_NR      PERSON_ID       ZKRS    GLOC2   GLOC3   GLOC4   WKAT    GEM2    PARTNR  GETG    GEMT    GEJA    ALTJ    AKL5    AGRP    VALTJ  GORT    GORTCH  GORTAUS GESL    ZIVL    ZIVJ    HMAT    CHJA    ZNAT    NATI    NATUNO  AUSW    WO5M    WO5CH   WO5AUS  ELTERN  ZKIND   GJKIND_1   GJKIND_2        GJKIND_3        GJKIND_4        GJKIND_L        GJKIND_J        STHHZ   STHHW   RPHHZ   RPHHW   EPNRZ   EPNRW   HHTPZ   HHTPW   APERZ   APERW   WKATA   SPRA    MSPR    HSPR    SHSD    SHHD    SHPR    SHFR    SHTB    SHIT    SHRR    SHEN    SHAN    BSPR    SBSD    SBHD    SBPR    SBFR    SBTB    SBIT    SBRR    SBEN    SBAN    GEGW    HABG    UHAB    FHAB    HFAB    HBAB    LSAB    MPAB    BLAB    BSAB    OSAB    KAUS    AMS     BGRAD   KAZEIT  ERWS    MAMS    KAMS    VOLL    TZT1    TZT2    ETOA    ARLO    STSU    KSTZ    NENSS   IAUS    RENT    HAFA    FRTA    HVOLL   HTZS    HAZEIT  HIAUS   HHAFA   HFRTA   ERBE    PBER    ISCO    SOPK    BETGR   UNTGR   ANOGA   AREFO   AGDE    AZKRS   AGLOC2  AGLOC3  AGLOC4  AORT    ADIST   APEND   AWMIN   AWOFT   AWTAGE  AVEMI   AWEGM   AVMKE   AVELO   AMOFA   AMRAD   APKWL   APKWM   AWBUS   ABAHN   ATRAM   APOST   AVAND   SGDE    SZKRS   SGLOC2  SGLOC3  SGLOC4  SORT    SDIST   SPEND   SWMIN   SWOFT   SWTAGE  SVEMI   SWEGM   SVMKE   SVELO   SMOFA   SMRAD   SPKWL   SPKWM   SSBUS   SBAHN   STRAM   SPOST   SVAND   SNOGA   SREFO   XACH    YACH
	// example: 12      2701    1030475         1943680 1       5       -7      11              112             0               1       -9      -9      4       12      1974    26      25      25      26     4       3946    -8      2       1       -9      1       -8      -7      8100    1       -9      2       2701    -8      0       -8      -8         -8      -8      -8      -8      -8      111     111     1       1       0       0       1000    1000    1       1       1       110     1111    200     1       0       0       0       0       0       0       0       0       400     1       1       0       0       0       0       0       0       0       -8      31      0       0       0       1       0       0       0       1       1       0       11      11      6       124     10      1       1       0       0       0       0       0       0       0       0       0       1       1       43.0    -8      43.0    -8      6       6       86504   -7      -7      91      10      10      8511A   21      2701    2109    21                                  1       0       1       -7      1       5       390     0       0       1       0       0       0       0       0       0       1       0       0       -8      0                                                       -8      0       -8      -8      -8      -8      -8      0       -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      -8      610100  268000
	// index:   0          1          2          3      4               5               6       7       8       9 ...
			FileReader fileReader = new FileReader(populationFilename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			// Skip header
			String currentLine = bufferedReader.readLine();
			int lineIndex = 0;
//			int unknownNationalityCount = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				
				HouseholdInformation hInfo = (HouseholdInformation) households.get(lineIndex);
				if (hInfo != null) {
					int hh_personCount = hInfo.getPersonCount();
					int hh_kidCount = hInfo.getKidCount();
					String[] entries = currentLine.split("\t", -1);
					
					int id   = Integer.parseInt(entries[5].trim());
					
					int age    = Integer.parseInt(entries[16].trim());
	
					int sexIndex    = Integer.parseInt(entries[23].trim());
					String sex = null;
					if (sexIndex == 1) {
						sex = "m";
					} else if (sexIndex == 2) {
						sex = "f";
					}
	
					int nationalityIndex    = Integer.parseInt(entries[26].trim());
					String nationality = null;
					if (nationalityIndex < 1 || nationalityIndex > 2) {
						Gbl.errorMsg("Line $.: wrong format of nationality="
								+ nationalityIndex + "!");
					} else if (nationalityIndex == 1) {
						nationality = "swiss";
					} else if (nationalityIndex == 2) {
						nationality = "other";
					} else {
						nationality = "unknown";
	//					unknownNationalityCount++;
					}
					
					int employedIndex    = Integer.parseInt(entries[88].trim());
					String employed = null;
					if (employedIndex >= 11 && employedIndex <= 14) {
						employed = "yes";
					} else if (employedIndex == 20 ||
							(employedIndex >= 31 && employedIndex <= 35) ||
							employedIndex == 4 || employedIndex == 40) {
						employed = "no";
					} else {
						Gbl.errorMsg("Line $.: wrong format of employed="
								+ entries[36] + "!");
					}
	
					int transport = Integer.parseInt(entries[136].trim());
					String car = "never";
					if (transport == 1) {
						car = "always";
					}
					transport    = Integer.parseInt(entries[137].trim());
					if (transport == 1) {
						car = "always";
					}
					transport    = Integer.parseInt(entries[160].trim());
					if (transport == 1) {
						car = "always";
					}
					transport    = Integer.parseInt(entries[161].trim());
					if (transport == 1) {
						car = "always";
					}
					transport    = Integer.parseInt(entries[138].trim());
					if (transport == 1) {
						car = "sometimes";
					}
					transport    = Integer.parseInt(entries[162].trim());
					if (transport == 1) {
						car = "sometimes";
					}
	
					String license = "no";
					if (car.equals("always") || car.equals("sometimes")) {
						license = "yes";
					}
	
					Person person = new Person(Integer.toString(id), sex,
							Integer.toString(age), license, car, employed);
					person.setNationality(nationality);
//							nationality, null, Integer.toString(hh_personCount),
//							Integer.toString(hh_kidCount));
					
					int actX = Integer.parseInt(entries[170].trim());
					int actY = Integer.parseInt(entries[171].trim());
					
					Act act = new Act(PersonToHomeFacilityMapper.homeActType,
							actX, actY, null, 0, 30, 30, true);
					Plan plan = new Plan(person);
					plan.addAct(act);
					person.addPlan(plan);
					try {
						plans.addPerson(person);
					} catch (Exception e) {
						Gbl.errorMsg(e);
					}
					
					persons.add(person);
					hInfo.addPerson(person);
				}

				lineIndex++;
				if (lineIndex % (populationFileLineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		// remove the invalid household entries
		Iterator<HouseholdI> hIt = households.iterator();
		while (hIt.hasNext()) {
			if (hIt.next() == null) {
				hIt.remove();
			}
		}
		return plans;
	}
	
	/**
	 * A person in {@code persons} at index <i>i</i> belongs to the household in 
	 * {@code households} at the same index <i>i</i>.
	 * @return The persons that were generated.
	 */
	public ArrayList<Person> getPersons() {
		return persons;
	}
	
	/**
	 * A person in {@code persons} at index <i>i</i> belongs to the household in 
	 * {@code households} at the same index <i>i</i>.
	 * @return The households that were generated.
	 */
	public ArrayList<HouseholdI> getHouseholds() {
		return households;
	}
	
	public class HouseholdInformation implements HouseholdI {
		
		ArrayList<String> memberIds;
		ArrayList<Person> members;
		private int kidCount;
		private double income;
		
//		private int typeIndex = -1;
		
		public HouseholdInformation(/*int typeIndex*/) {
			memberIds = new ArrayList<String>();
			members = new ArrayList<Person>(); 
			kidCount = 0;
//			this.typeIndex = typeIndex;
		}
		
		void addPerson(String personId, int age) {
			memberIds.add(personId);
			if (age < 18) {
				addKid();
			}
		}
		
		void addPerson(Person person) {
			members.add(person);
		}

		private void addKid() {
			this.kidCount ++;
		}
		
		public int getPersonCount() {
			return memberIds.size();
		}
		
		public int getKidCount() {
			return kidCount;
		}
		
		public double getIncome() {
			return income;
		}
		
		public void setIncome(double income) {
			this.income = income;
		}
		
		public ArrayList<Person> getPersons() {
			return members;
		}
	}
}
