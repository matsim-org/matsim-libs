/* *********************************************************************** *
 * project: org.matsim.*
 * Census2001SampleParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.census2001;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

public class Census2001SampleParser {
	private static final Logger LOG = Logger.getLogger(Census2001SampleParser.class);
	private Map<Id<Household>,String> householdMap = new HashMap<>();
	private Map<Id<Person>,String> personMap = new HashMap<>();
	private Map<Id<Household>,String> geographyMap = new HashMap<>();
	private Scenario sc;
	private HouseholdsImpl households;
	private ObjectAttributes householdAttributes = new ObjectAttributes();
	private ObjectAttributes personAttributes = new ObjectAttributes();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Census2001SampleParser.class.toString(), args);
		String householdFilename = args[0];
		String personFilename = args[1];
		String geographyFilename = args[2];
		String outputFolder = args[3];
		
		Census2001SampleParser cs = new Census2001SampleParser();
		cs.parseHouseholdMap(householdFilename);
		cs.parseGeography(geographyFilename);
		cs.parsePersons(personFilename);
		
		cs.buildPopulation();
		cs.writePopulationAndAttributes(outputFolder);
		
		Header.printFooter();
	}
	
	
	public Census2001SampleParser() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	/**
	 * Each line contains the record of a household. The following segments are 
	 * currently parsed:
	 * <ul>
	 * 		<li> Serial number - position 1, 9 characters;
	 * 		<li> Household size - position 10, 3 characters;
	 * 		<li> Type of living quarters (code) - position 13, 2 characters; 
	 * 		<li> Majority population group of household (code) - position 40, 1 character;
	 * 		<li> Annual household income (code) - position 41, 2 characters;
	 * </ul> 
	 * @param filename absolute path of the file containing the HOUSEHOLDS data
	 * 		  set.
	 */
	public void parseHouseholdMap(String filename){
		LOG.info("Parsing households from " + filename);
		Counter counter = new Counter("  household # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				String serial = line.substring(0, 9);
				String size = line.substring(9, 12);
				String type = line.substring(12, 14);
				String population = line.substring(39, 40);
				String income = line.substring(40, 42);
				
				householdMap.put(Id.create(serial, Household.class), size + "," + this.getDwellingType(type) + "," + this.getPopulation(population) + "," + this.getIncome(income));
				counter.incCounter();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader " + filename);
			}
		}
		counter.printCounter();
		LOG.info("Done parsing households (" + householdMap.size() + ").");
	}
	
	/**
	 * Each line contains the record of a household. The following segments are 
	 * currently parsed:
	 * <ul>
	 * 		<li> Serial number - position 1, 9 characters;
	 * 		<li> Municipality (code) - position 10, 3 characters;
	 * 		<li> Municipality name - position 13, 26 characters;
	 * 		<li> Magisterial district (code) - position 39, 3 characters;
	 * 		<li> Magisterial name - position 42, 19 characters;
	 * 		<li> District council (code) - position 61, 3 characters;
	 * 		<li> District council name - position 64, 47 characters;
	 * 		<li> Province (code) - position 111, 1 character;
	 * 		<li> Province name - position 112, 17 characters;
	 * 		<li> EA type (code) - position 129, 1 characters;
	 * </ul> 
	 * @param filename filename absolute path of the file containing the GEOGRAPHY data
	 * 		  set.
	 */
	public void parseGeography(String filename){
		LOG.info("Parsing geography from " + filename);
		Counter counter = new Counter("  geography # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = null;
			while((line = br.readLine()) != null){
				String serial = line.substring(0, 9);
				String municipalCode = line.substring(9, 12);
				String municipalName = line.substring(12, 38);
				String magisterialCode = line.substring(38, 41); 
				String magisterialName = line.substring(41, 60); 
				String districtCode = line.substring(60, 63);
				String districtName = line.substring(63, 110); 
				String provinceCode = line.substring(110, 111);
				String provinceName = line.substring(111, 128);
				String eaType = line.substring(128, 129);
				
				geographyMap.put(Id.create(serial, Household.class), municipalCode + "," + municipalName + "," + 
						magisterialCode + "," + magisterialName + "," + 
						districtCode + "," + districtName + "," + 
						provinceCode + "," + provinceName + "," +
						eaType);
				counter.incCounter();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader "
					+ filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader "
						+ filename);
			}
		}
		counter.printCounter();
		LOG.info("Done parsing geography (" + geographyMap.size() + ").");
	}
	
	
	/**
	 * Each line contains the record of a person. The following segments are 
	 * currently parsed (The first number indicates the position in the string,
	 * and the second number in brackets the number of characters):
	 * <ul>
	 * 		<li> Serial number - 1(9);
	 * 		<li> Type of living quarters comprehensive (code) - 10(2);
	 * 		<li> Person number - 12(4);
	 * 		<li> Age - 24(3);
	 * 		<li> Gender (code) - 27(1);
	 * 		<li> Relationship (code) - 28(2);
	 * 		<li> Population group (code) - 33(1);
	 * 		<li> Present school attendance (code) - 84(1);
	 * 		<li> Employment status (code) - 97(2);
	 * 		<li> Economic sector (code) - 100(3); [Currently not used]
	 * 		<li> Hours worked (code) - 106(2); [Currently not used]
	 * 		<li> Place of work (code) - 108(1); [Currently not used]
	 * 		<li> Main place of work (code) - 110(8);
	 * 		<li> Travel to school / place of work (code) - 144(1);
	 * 		<li> Income (code) - 145(2);
	 * 		<li> Place of enumeration - 147(1); [Currently not used]
	 * </ul> 
	 * @param filename filename absolute path of the file containing the PERSONS 
	 * 		  data set.
	 */
	public void parsePersons(String filename){
		LOG.info("Parsing persons from " + filename);
		Counter counter = new Counter("  persons # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = null;
			while((line = br.readLine()) != null){
				String serial = line.substring(0, 9);
				String quarterType = line.substring(9, 11);
				String person = line.substring(11, 15);
				String age = line.substring(23, 26);
				String gender = line.substring(26, 27);
				String relationship = line.substring(27, 29);
				String population = line.substring(32, 33);
				String school = line.substring(83, 84);
				String employment = line.substring(96, 98);
				String sector = line.substring(99, 102);
				String hours = line.substring(105, 107);
				String workplace = line.substring(107, 108);
				String mainPlaceOfWork = line.substring(109, 117);
				String travel = line.substring(143, 144);
				String income = line.substring(144, 146);
				String enumeration = line.substring(146, 147);
				
				String s = getComprehensiveQuarterType(quarterType) + "," +
						Integer.parseInt(age) + "," +
						getGender(gender) + "," +
						getRelationship(relationship) + "," +
						getPopulation(population) + "," + 
						getSchool(school) + "," + 
						getDerivedEmployment(employment) + "," + 
						getMainPlaceOfWork(mainPlaceOfWork) + "," +
						getMainModeToPrimary(travel) + "," +
						getIncome(income);
				personMap.put(Id.create(serial + "_" + String.valueOf(Integer.parseInt(person)), Person.class), s);
				counter.incCounter();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader "
					+ filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader "
						+ filename);
			}
		}
		counter.printCounter();
		LOG.info("Done parsing persons (" + personMap.size() + ").");
	}
	
	
	public void buildPopulation(){
		if(householdMap.size() == 0 || personMap.size() == 0){
			throw new RuntimeException("Either the househols or persons are of size ZERO!");
		}
		
		LOG.info("Checking that each person has an associated household in the map...");
		for(Id<Person> i : personMap.keySet()){
			String s = i.toString().split("_")[0];
			if(!householdMap.containsKey(Id.create(s, Household.class))){
				LOG.warn("Could not find household " + s);
			}
		}
		LOG.info("Done checking person-household match.");
		
		this.households = new HouseholdsImpl();
		HouseholdsFactory hhf = households.getFactory();
		
		Population population = sc.getPopulation();
		PopulationFactoryImpl pf = (PopulationFactoryImpl) population.getFactory();
		for(Id<Person> pid : personMap.keySet()){
			String[] sa = personMap.get(pid).split(",");
			String quarterType = sa[0];
			int age = Integer.parseInt(sa[1]);
			String gender = sa[2].equalsIgnoreCase("Male") ? "m" : "f";
			String relationship = sa[3];
			String race = sa[4];
			String school = sa[5];
			boolean employment = sa[6].equalsIgnoreCase("Yes") ? true : false;
			String mainPlaceOfWork = sa[7];
			String modeToMain = sa[8];
			Double income = Double.parseDouble(sa[9]);
			
			Person person = pf.createPerson(pid);
			PersonUtils.setAge(person, age);
			PersonUtils.setSex(person, gender);
			PersonUtils.setEmployed(person, employment);
			personAttributes.putAttribute(pid.toString(), "quarterType", quarterType);
			personAttributes.putAttribute(pid.toString(), "relationship", relationship);
			personAttributes.putAttribute(pid.toString(), "race", race);
			personAttributes.putAttribute(pid.toString(), "school", school);
			personAttributes.putAttribute(pid.toString(), "mainPlaceOfWork", mainPlaceOfWork);
			personAttributes.putAttribute(pid.toString(), "modeToMain", modeToMain);
			personAttributes.putAttribute(pid.toString(), "income", income);
			population.addPerson(person);
			
			/* Add to household */
			Id<Household> hid = Id.create(pid.toString().split("_")[0], Household.class);
			if(!this.households.getHouseholds().containsKey(hid)){
				/* Household data */
				String[] hsa = householdMap.get(hid).split(",");
				int hhSize = Integer.parseInt(hsa[0]);
				String dwellingType = hsa[1];
				String hhPopulation = hsa[2];
				String incomeString = hsa[3];
				Income hhIncome = null;
				if(!incomeString.equalsIgnoreCase("Unknown")){
					hhIncome = hhf.createIncome(Double.parseDouble(hsa[3]), IncomePeriod.year);					
				} 
				Household hh = hhf.createHousehold(hid);
				hh.setIncome(hhIncome);
				householdAttributes.putAttribute(hid.toString(), "dwellingType", dwellingType);
				householdAttributes.putAttribute(hid.toString(), "householdSize", hhSize);
				householdAttributes.putAttribute(hid.toString(), "population", hhPopulation);
				
				/* Geography data */
				String[] gsa = geographyMap.get(hid).split(",");
				String municipalCode = gsa[0];
				String municipalName = gsa[1];
				String magisterialCode = gsa[2];
				String magisterialName = gsa[3]; 
				String districtCode = gsa[4];
				String districtName = gsa[5];
				String provinceCode = gsa[6];
				String provinceName = gsa[7];
				String eaType = gsa[8];
				householdAttributes.putAttribute(hid.toString(), "municipalCode", municipalCode);
				householdAttributes.putAttribute(hid.toString(), "municipalName", correctCase(municipalName));
				householdAttributes.putAttribute(hid.toString(), "magisterialCode", magisterialCode);
				householdAttributes.putAttribute(hid.toString(), "magisterialName", correctCase(magisterialName));
				householdAttributes.putAttribute(hid.toString(), "districtCode", districtCode);
				householdAttributes.putAttribute(hid.toString(), "districtName", correctCase(districtName));
				householdAttributes.putAttribute(hid.toString(), "provinceCode", provinceCode);
				householdAttributes.putAttribute(hid.toString(), "provinceName", correctCase(provinceName));
				householdAttributes.putAttribute(hid.toString(), "eaType", eaType);

				this.households.getHouseholds().put(hid, hh);
			}
			this.households.getHouseholds().get(hid).getMemberIds().add(pid);
		}		
		/* Validate: */
		LOG.info("================================================================");
		LOG.info("Validating the population and households");
		LOG.info("----------------------------------------------------------------");
		LOG.info("Person map: " + personMap.size());
		LOG.info("Household map: " + householdMap.size());
		LOG.info("----------------------------------------------------------------");
		LOG.info("Population size: " + sc.getPopulation().getPersons().size());
		LOG.info("Number of households: " + households.getHouseholds().size());
		LOG.info("================================================================");
	}
	
	private String correctCase(String s){
		s.toLowerCase();
		String newS = "";
		String[] sa = s.split(" ");
		for(String ss : sa){
			if(!ss.equalsIgnoreCase("of")){
				ss.substring(0,1).toUpperCase();
				newS += ss + " ";
			}
		}
		String rNew = newS.substring(0, newS.length()-1);
		return rNew;
	}
	
	
	public void writePopulationAndAttributes(String outputFolder){
		writeHouseholds(outputFolder);
		writePopulation(outputFolder);
	}
	
	/**
 	 * Writes the households and their attributes to file.
	 * @param outputfolder
	 */
	public void writeHouseholds(String outputfolder){
		if(this.households == null || this.householdAttributes == null){
			throw new RuntimeException("Either no households or household attributes to write.");
		} else{
			LOG.info("Writing households to file...");
			HouseholdsWriterV10 hw = new HouseholdsWriterV10(this.households);
			hw.setPrettyPrint(true);
			hw.writeFile(outputfolder + "Households.xml");

			LOG.info("Writing household attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(householdAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "HouseholdAttributes.xml");
		}
	}

	
	/**
 	 * Writes the population and their attributes to file.
	 * @param outputfolder
	 */
	public void writePopulation(String outputfolder){
		if(this.sc.getPopulation().getPersons().size() == 0 || this.personAttributes == null){
			throw new RuntimeException("Either no persons or person attributes to write.");
		} else{
			LOG.info("Writing population to file...");
			PopulationWriter pw = new PopulationWriter(this.sc.getPopulation(), this.sc.getNetwork());
			pw.writeV5(outputfolder + "Population.xml");

			LOG.info("Writing person attributes to file...");
			ObjectAttributesXmlWriter oaw = new ObjectAttributesXmlWriter(this.personAttributes);
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}

	
	class SAHouseholdsFactory implements HouseholdsFactory{

		@Override
		public Household createHousehold(Id<Household> householdId) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Income createIncome(double income, IncomePeriod period) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	class SAHouseholds implements Households{

		@Override
		public Map<Id<Household>, Household> getHouseholds() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HouseholdsFactory getFactory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ObjectAttributes getHouseholdAttributes() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	
	private String getPopulation(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "Black"; 
		case 2:
			return "Coloured"; 
		case 3:
			return "Indian-Asian";
		case 4:
			return "White";
		}
		return "Unknown";
	}
	
	private String getDwellingType(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "House";
		case 2:
			return "Hotel";
		case 3:
			return "StudentResidence";
		case 4:
			return "OldAgeHome";
		case 5:
			return "Hostel";
		}
		return "Other";
	}
	
	private String getIncome(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "0.0";
		case 2:
			return "3200.0";
		case 3:
			return "7200.0";
		case 4:
			return "13576.0";
		case 5:
			return "26152.0";
		case 6:
			return "54306.0";
		case 7:
			return "108612.0";
		case 8:
			return "217223.0";
		case 9:
			return "434446.0";
		case 10:
			return "868893.0";
		case 11:
			return "1737786.0";
		case 12:
			return "4915200.0";
		}
		return "Unknown";
	}
	
	private String getComprehensiveQuarterType(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "HousingUnit";
		case 2:
			return "ResidentialHotel";
		case 3:
			return "StudentResidence";
		case 4:
			return "OldAgeHome";
		case 5:
			return "Hostel";
		case 11:
			return "TouristHotel";
		case 12:
			return "Medical";
		case 14:
			return "DisabilityHome";
		case 15:
			return "BoardingSchool";
		case 16:
			return "InitiationSchool";
		case 17:
			return "ReligiousHome";
		case 18:
			return "Defence";
		case 19:
			return "Prison";
		case 20:
			return "Community";
		case 21:
			return "Shelter";
		case 22:
			return "Homeless";
		}
		return "Unknown";
	}
	
	private String getGender(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "Male";
		case 2:
			return "Female";
		}
		return "Unknown";
	}
	
	private String getRelationship(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "Head";
		case 2:
			return "Partner";
		case 3: // Son/daughter
		case 4: // Adopted son/daughter
		case 5: // Stepson/stepdaughter
		case 10: // Son/daughter-in-law
			return "Child";
		case 6: // Brother/sister
		case 11: // Brother/sister-in-law
			return "Sibling";
		case 7: // Parent
		case 8: // Parent-in-law
			return "Parent";
		case 9:
			return "Grandchild";
		case 12:
			return "Other";
		case 13:
			return "Unrelated";
		case 99:
			return "NotApplicable";
		}
		return "Unknown";
	}
	
	private String getSchool(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "None";
		case 2:
			return "PreSchool";
		case 3:
			return "School";
		case 4: // College
		case 5: // Technikon
		case 6: // University
			return "Tertiary";
		case 7: 
			return "AdultEducation";
		case 8:
			return "Other";
		}
		return "Unknown";
	}
	
	private String getDerivedEmployment(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "Yes";
		case 2: // Unemployed
		case 3: // Scholar
		case 4: // Homemaker or housewife
		case 5: // Pensioner/retired - too old to work
		case 6: // Unable to work due to illness or disability
		case 7: // Seasonal worker not working presently
		case 8: // Does not choose to work
		case 9: // Could not find work
		case 0: // Not applicable (younger than 15 and older then 65)
			return "No";
		}
		return "No";
	}
	
	private String getMainPlaceOfWork(String code){
		Integer mp = null;
		try{
			mp = Integer.parseInt(code);
			if(mp > 10000000){
				return code;
			}
		} catch(NumberFormatException e){
		}
		return "Unknown";
	}
	
	private String getMainModeToPrimary(String code){
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 0:
			return "notApplicable";
		case 1:
			return "walk";
		case 2:
			return "bicycle";
		case 3: // Motorcycle
		case 4: // Car
			return "car";
		case 5: // Passenger
			return "passenger";
		case 6:
			return "taxi";
		case 7:
			return "bus";
		case 8:
			return "train";
		case 9:
			return "other";
 	 	}
		return "unknown";
	}
	
	
}

