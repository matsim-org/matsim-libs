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

package playground.southafrica.population.census2011;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.population.census2011.containers.Employment2011;
import playground.southafrica.population.census2011.containers.Gender2011;
import playground.southafrica.population.census2011.containers.HousingType2011;
import playground.southafrica.population.census2011.containers.Income2011;
import playground.southafrica.population.census2011.containers.MainDwellingType2011;
import playground.southafrica.population.census2011.containers.PopulationGroup2011;
import playground.southafrica.population.census2011.containers.Relationship2011;
import playground.southafrica.population.census2011.containers.School2011;
import playground.southafrica.population.utilities.SAIncomeConverter;
import playground.southafrica.utilities.Header;

public class Census2011SampleParser {
	static final Logger LOG = Logger.getLogger(Census2011SampleParser.class);
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
				
				householdMap.put(Id.create(serial, Household.class), size.replaceAll(" ", "") + "," 
						+ HousingType2011.parseTypeFromCensusCode(type) + "," 
						+ MainDwellingType2011.parseTypeFromCensusCode(dwelling) + "," 
						+ PopulationGroup2011.parseTypeFromCensusCode(population) + "," 
						+ Income2011.parseIncome2011FromCensusCode(income));
				
				String province = line.substring(58, 59);
				String district = line.substring(59, 62);
				String municipality = line.substring(62, 65);
				geographyMap.put(Id.create(serial, Household.class), province + "," + district + "," + municipality);
				
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
						Gender2011.parseGenderFromCensusCode(gender) + "," +
						Relationship2011.parseRelationshipFromCensusCode(relationship) + "," +
						PopulationGroup2011.parseTypeFromCensusCode(population) + "," + 
						School2011.parseEducationFromCensusCode(school, educationInstitution) + "," + 
						Employment2011.parseEmploymentFromCensusCode(employment) + "," + 
						Income2011.parseIncome2011FromCensusCode(income);
				personMap.put(Id.create(serial + "_" + String.valueOf(Integer.parseInt(person.replaceAll(" ", ""))), Person.class), s);
				
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
		List<Id<Person>> personsToRemove = new ArrayList<>();
		for(Id<Person> i : personMap.keySet()){
			String s = i.toString().split("_")[0];
			if(!householdMap.containsKey(Id.create(s, Household.class))){
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
		for(Id<Person> id : personsToRemove){
			personMap.remove(id);
		}
		
		this.households = new HouseholdsImpl();
		HouseholdsFactory hhf = households.getFactory();
		
		Population population = sc.getPopulation();
		PopulationFactoryImpl pf = (PopulationFactoryImpl) population.getFactory();
		for(Id<Person> personId : personMap.keySet()){
			String[] sa = personMap.get(personId).split(",");
			int age = Integer.parseInt(sa[0]);
			
			Gender2011 gender = Gender2011.parseGenderFromString(sa[1]);
			
			Relationship2011 relationship = Relationship2011.parseRelationshipFromString(sa[2]);
			PopulationGroup2011 populationGroup = PopulationGroup2011.parseTypeFromString(sa[3]);
			School2011 school = School2011.parseEducationFromString(sa[4]);
			
			Employment2011 employment = Employment2011.parseEmploymentFromString(sa[5]);
			boolean isEmployed = false;
			switch (employment) {
			case Employed:
				isEmployed = true;
			case Discouraged:
				break;
			case Inactive:
				break;
			case NotApplicable:
				break;
			case Unemployed:
				break;
			default:
				break;
			}			

			Income2011 pIncome = Income2011.parseIncome2011FromString(sa[6]);
			
			Person person = pf.createPerson(personId);
			PersonUtils.setAge(person, age);
			PersonUtils.setSex(person, Gender2011.getMatsimGender(gender));
			PersonUtils.setEmployed(person, isEmployed);
			personAttributes.putAttribute(personId.toString(), "relationship", relationship.toString());
			personAttributes.putAttribute(personId.toString(), "race", populationGroup.toString());
			personAttributes.putAttribute(personId.toString(), "school", school.toString());
			if(pIncome != null){
				personAttributes.putAttribute(personId.toString(), "income", pIncome.toString());
			}
			population.addPerson(person);
			
			/* Add to household */
			Id<Household> hid = Id.create(personId.toString().split("_")[0], Household.class);
			if(!this.households.getHouseholds().containsKey(hid)){
				/* Household data */
				String[] hsa = householdMap.get(hid).split(",");
				int hhSize = Integer.parseInt(hsa[0]);
				
				HousingType2011 housingType = HousingType2011.parseTypeFromString(hsa[1]);
				MainDwellingType2011 mainDwellingType = MainDwellingType2011.parseTypeFromString(hsa[2]);
				PopulationGroup2011 hhPopulation = PopulationGroup2011.parseTypeFromString(hsa[3]);
				
				Income2011 hhIncome = Income2011.parseIncome2011FromString(hsa[4]);

				Household hh = hhf.createHousehold(hid);
				hh.setIncome(Income2011.getIncome(hhIncome));
				
				householdAttributes.putAttribute(hid.toString(), "housingType", housingType.toString());
				householdAttributes.putAttribute(hid.toString(), "mainDwellingType", mainDwellingType.toString());
				householdAttributes.putAttribute(hid.toString(), "householdSize", hhSize);
				householdAttributes.putAttribute(hid.toString(), "population", hhPopulation.toString());
				
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
			this.households.getHouseholds().get(hid).getMemberIds().add(personId);
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
			oaw.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
			
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

}

