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

package playground.southafrica.population.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
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
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.utilities.Header;

public class Census2011SampleParser {
	private static final Logger LOG = Logger.getLogger(Census2011SampleParser.class);
	private Map<Id,String> householdMap = new HashMap<Id, String>();
	private Map<Id,String> personMap = new HashMap<Id, String>();
	private Map<Id,String> geographyMap = new HashMap<Id, String>();
	private Scenario sc;
	private HouseholdsImpl households;
	private ObjectAttributes householdAttributes = new ObjectAttributes();
	private ObjectAttributes personAttributes = new ObjectAttributes();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Census2011SampleParser.class.toString(), args);
		
		String householdFilename = args[0];
		String personFilename = args[1];
		String outputFolder = args[2];
		
		Census2011SampleParser cs = new Census2011SampleParser();
		cs.parseHouseholdMap(householdFilename);
		cs.parsePersons(personFilename);

		cs.buildPopulation();
		cs.writePopulationAndAttributes(outputFolder);
		
		Header.printFooter();
	}
	
	
	public Census2011SampleParser() {
		this.sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}

	/**
	 * Populates the household map from the given file. Each line of the file 
	 * contains the record of a household. The following segments are currently 
	 * parsed (The first number indicates the position in the string,
	 * and the second number in brackets the number of characters):
	 * <ul>
	 * 		<li> Serial number - 2(11);
	 * 		<li> Household size - 79(2);
	 * 		<li> Type of living quarters (code) - 13(1); 
	 * 		<li> Type of main dwelling (code) - 14(2); 
	 * 		<li> Majority population group of household (code) - 81(1);
	 * 		<li> Annual household income (code) - 82(2);
	 * </ul> 
	 * 
	 * For the geography of the household, the following segments are 
	 * currently parsed (The first number indicates the position in the string,
	 * and the second number in brackets the number of characters):
	 * <ul>
	 * 		<li> Province (code) - 59 (1);
	 * 		<li> District (code) - 60 (3); and
	 * 		<li> Municipality (code) - 63 (3).
	 * </ul>
	 * 
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
				String serial = line.substring(1, 12);
				String size = line.substring(78, 80);
				String type = line.substring(12, 13);
				String dwelling = line.substring(13, 15);
				String population = line.substring(80, 81);
				String income = line.substring(81, 83);
				
				householdMap.put(new IdImpl(serial), size.replaceAll(" ", "") + "," + this.getHousingType(type) + "," + this.getMainDwellingType(dwelling) + "," + this.getPopulation(population) + "," + this.getIncome(income));
				
				String province = line.substring(58, 59);
				String district = line.substring(59, 62);
				String municipality = line.substring(62, 65);
				geographyMap.put(new IdImpl(serial), province + "," + district + "," + municipality);
				
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
	 * Each line contains the record of a person. The following segments are 
	 * currently parsed (The first number indicates the position in the string,
	 * and the second number in brackets the number of characters):
	 * 
	 * FIXME Must be updated for 2011!!
	 * <ul>
	 * 		<li> Serial number - 2(11);
	 * 		<li> Person number - 13(3);
	 * 		<li> Age - 16(3);
	 * 		<li> Gender (code) - 19(1);
	 * 		<li> Relationship (code) - 28(2);
	 * 		<li> Population group (code) - 33(1);
	 * 		<li> Income (code) - 83(2);
	 * 		<li> Present school attendance (code) - 85(1);
	 * 		<li> Present school type (code) - 86(1);
	 * 		<li> Employment status (code) - 148(1);
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
				String serial = line.substring(1, 12);
				String person = line.substring(12, 15);
				String age = line.substring(15, 18);
				String gender = line.substring(18, 19);
				String relationship = line.substring(27, 29);
				String population = line.substring(32, 33);
				String income = line.substring(82, 84);
				String school = line.substring(84, 85);
				String educationInstitution = line.substring(85, 86);
				String employment = line.substring(147, 148);
				
				String s = Integer.parseInt(age.replaceAll(" ", "")) + "," +
						getGender(gender) + "," +
						getRelationship(relationship) + "," +
						getPopulation(population) + "," + 
						getSchool(school, educationInstitution) + "," + 
						getEmployment(employment) + "," + 
						getIncome(income);
				personMap.put(new IdImpl(serial + "_" + String.valueOf(Integer.parseInt(person.replaceAll(" ", "")))), s);
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
		int noHouseholdCount = 0;
		List<Id> personsToRemove = new ArrayList<Id>();
		for(Id i : personMap.keySet()){
			String s = i.toString().split("_")[0];
			if(!householdMap.containsKey(new IdImpl(s))){
//				LOG.warn("Could not find household " + s);
				noHouseholdCount++;
//				LOG.info("   ---> " + personMap.get(i));
				
				personsToRemove.add(i);
			}
		}
		LOG.warn("Done checking person-household match (" + noHouseholdCount +
				" persons without a match.)");

		/* Currently, Apr 2014 (JWJ), persons without a household
		 * affiliation will be removed. */
		LOG.warn("Persons with no household association will be ignored.");
		for(Id id : personsToRemove){
			personMap.remove(id);
		}
		
		this.households = new HouseholdsImpl();
		HouseholdsFactory hhf = households.getFactory();
		
		Population population = sc.getPopulation();
		PopulationFactoryImpl pf = (PopulationFactoryImpl) population.getFactory();
		for(Id pid : personMap.keySet()){
			String[] sa = personMap.get(pid).split(",");
			int age = Integer.parseInt(sa[0]);
			String gender = sa[1].equalsIgnoreCase("Male") ? "m" : "f";
			String relationship = sa[2];
			String race = sa[3];
			String school = sa[4];
			boolean employment = sa[5].equalsIgnoreCase("Yes") ? true : false;
			String pIncomeString = sa[6];
			Income pIncome = null;
			if(!pIncomeString.equalsIgnoreCase("Unknown")){
				pIncome = hhf.createIncome(Double.parseDouble(sa[6]), IncomePeriod.year);
			}
			
			PersonImpl person = (PersonImpl) pf.createPerson(pid);
			person.setAge(age);
			person.setSex(gender);
			person.setEmployed(employment);
			personAttributes.putAttribute(pid.toString(), "relationship", relationship);
			personAttributes.putAttribute(pid.toString(), "race", race);
			personAttributes.putAttribute(pid.toString(), "school", school);
			personAttributes.putAttribute(pid.toString(), "income", pIncome);
			population.addPerson(person);
			
			/* Add to household */
			Id hid = new IdImpl(pid.toString().split("_")[0]);
			if(!this.households.getHouseholds().containsKey(hid)){
				/* Household data */
				String[] hsa = householdMap.get(hid).split(",");
				int hhSize = Integer.parseInt(hsa[0]);
				String housingType = hsa[1];
				String mainDwellingType = hsa[2];
				String hhPopulation = hsa[3];
				String incomeString = hsa[4];
				Income hhIncome = null;
				if(!incomeString.equalsIgnoreCase("Unknown")){
					hhIncome = hhf.createIncome(Double.parseDouble(hsa[4]), IncomePeriod.year);					
				} 
				Household hh = hhf.createHousehold(hid);
				hh.setIncome(hhIncome);
				householdAttributes.putAttribute(hid.toString(), "housingType", housingType);
				householdAttributes.putAttribute(hid.toString(), "mainDwellingType", mainDwellingType);
				householdAttributes.putAttribute(hid.toString(), "householdSize", hhSize);
				householdAttributes.putAttribute(hid.toString(), "population", hhPopulation);
				
				/* Geography data */
				String[] gsa = geographyMap.get(hid).split(",");
				String provinceCode = gsa[0];
				String districtCode = gsa[1];
				String municipalCode = gsa[2];
				householdAttributes.putAttribute(hid.toString(), "municipalCode", municipalCode);
				householdAttributes.putAttribute(hid.toString(), "districtCode", districtCode);
				householdAttributes.putAttribute(hid.toString(), "provinceCode", provinceCode);

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
			oaw.putAttributeConverter(Income.class, new IncomeConverter());
			
			oaw.setPrettyPrint(true);
			oaw.writeFile(outputfolder + "PersonAttributes.xml");
		}
	}

	private static class IncomeConverter implements AttributeConverter<Income>{
		@Override
		public Income convert(String value) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String convertToString(Object o) {
			if(o instanceof Income){
				Income income = (Income)o;
				String s = String.format("%s_%.2f(%s)", income.getCurrency(), income.getIncome(), income.getIncomePeriod().toString());
				return s;
			}
			return null;
		}
		
	}
	
	
	class SAHouseholdsFactory implements HouseholdsFactory{

		@Override
		public Household createHousehold(Id householdId) {
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
		public Map<Id, Household> getHouseholds() {
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
		if(code.contains(".")){
			return "NotApplicable";
		}
		code = code.replaceAll(" ", "");
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
		case 5:
			return "Other";
		}
		return "Unknown";
	}
	
	/**
	 * Convert income code to <b><i>annual</i></b> income value in South African
	 * Rand (ZAR). The <i>upper</i> upper income level is given.
	 * @param code
	 * @return
	 */
	private String getIncome(String code){
		if(code.contains(".")){
			return "NotApplicable";
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return "0.00";
		case 2:
			return "4800.00";
		case 3:
			return "9600.00";
		case 4:
			return "19200.00";
		case 5:
			return "38400.00";
		case 6:
			return "76800.00";
		case 7:
			return "153600.00";
		case 8:
			return "307200.00";
		case 9:
			return "614400.00";
		case 10:
			return "1228800.00";
		case 11:
			return "2457600.00";
		case 12:
			return "4915200.00"; /* Twice the category 11 value. */
		}
		return "Unknown";
	}

	
	private String getHousingType(String code){
		if(code.contains(".")){
			return "NotApplicable";
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return "House";
		case 2:
			return "Hostel";
		case 3:
			return "Hotel";
		case 4:
			return "OldAgeHome";
		}
		return "Other";
	}
	
	
	private String getMainDwellingType(String code){
		if(code.contains(".")){
			return "NotApplicable";
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		switch (codeInt) {
		case 1:
			return "FormalHouse";
		case 2:
			return "TraditionalDwelling";
		case 3:
			return "Apartment";
		case 4:
			return "Cluster";
		case 5:
			return "Townhouse";
		case 6:
			return "Semi-detachedHouse";
		case 7:
			return "BackyardFormal";
		case 8:
			return "BackyardInformal";
		case 9:
			return "Informal";
		case 10: 
			return "BackyardFormal";
		case 11:
			return "Caravan/Tent";
		case 12:
			return "Other";
		}
		return "Unknown";
	}
	
	private String getGender(String code){
		if(code.contains(".")){
			return "NotApplicable";
		}
		
		code = code.replaceAll(" ", "");
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
		if(code.contains(".")){
			return "NotApplicable";
		}
		
		code = code.replaceAll(" ", "");
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
		case 13:
			return "Other";
		case 14:
			return "Unrelated";
		case 99:
			return "Unspecified";
		}
		return "Unknown";
	}
	
	/**
	 * Take care: only children five years and older were surveyed, so day-care
	 * is not covered, at least by definition.
	 * 
	 * @param code
	 * @param educationInstitution
	 * @return
	 */
	private String getSchool(String code, String educationInstitution){
		if(code.contains(".")){
			return "None";
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		
		/* Deal with explicit "No". */
		switch (codeInt) {
		case 2: // No
			return "None";
		}

		/* Deal with variations. */
		if(educationInstitution.equalsIgnoreCase(".")){
			return "None";
		}
		educationInstitution = educationInstitution.replaceAll(" ", "");
		int eduInt = Integer.parseInt(educationInstitution);
		switch (eduInt){
		case 1:
			return "PreSchool";
		case 2: // Normal grade R-12
		case 3: // Special school
			return "School";
		case 4: // FET
		case 5: // College
		case 6: // University & University of Technology
			return "Tertiary";
		case 7: 
		case 8:
			return "AdultEducation";
		case 9:
			return "HomeSchooling";
		}
		return "Unknown";
	}
	
	private String getEmployment(String code){
		if(code.contains(".")){
			return "No"; /* Not applicable */
		}
		code = code.replaceAll(" ", "");
		int codeInt = Integer.parseInt(code);
		switch (codeInt) {
		case 1:
			return "Yes";
		case 2: // Unemployed
		case 3: // Do not know
		case 9: // Unspecified
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

