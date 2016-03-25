/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStopsParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.analysis.srv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 * This class parses the SrV2008 person file and creates (Person) ObjectAttributes
 * contain the relevant information
 */
public class SrV2008PersonParser {
	private final static Logger log = Logger.getLogger(SrV2008PersonParser.class);
	private ObjectAttributes personAttributes = new ObjectAttributes();
	
	private static final String HOUSEHOLD_ID = "HHNR";
	private static final String PERSON_ID = "PNR";
	
	private static final String AGE = "V_ALTER";
	private static final String SEX = "V_GESCHLECHT";
	private static final String EMPLOYED = "V_ERW";
	
	private static final String DRIVERS_LICENCE = "V_FUEHR_PKW";
	// other possible variables: locationOfWork, locationOfSchool, parent
	
	
	public SrV2008PersonParser() {
	}
	
	
	public final void parse(String srvPersonFile) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		
		int lineCount = 0;
		
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(srvPersonFile);

			// header
			String currentLine = bufferedReader.readLine();
			lineCount++;
			String[] heads = currentLine.split(";", -1);
			Map<String,Integer> columnNumbers = new LinkedHashMap<String,Integer>(heads.length);
			for (int i = 0; i < heads.length; i++) {
				columnNumbers.put(heads[i],i);
			}
			
			
			// read data
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split(";", -1);
				lineCount++;
					
				if (lineCount % 100000 == 0) {
					log.info(lineCount+ " lines read in so far.");
					Gbl.printMemoryUsage();
				}
				
				Id<Household> householdId = Id.create(entries[columnNumbers.get(HOUSEHOLD_ID)], Household.class);
				Id<Person> personId = Id.create(entries[columnNumbers.get(PERSON_ID)], Person.class);
				
				Integer age = new Integer(entries[columnNumbers.get(AGE)]);
				Integer sex = new Integer(entries[columnNumbers.get(SEX)]);
				Integer employed = new Integer(entries[columnNumbers.get(EMPLOYED)]);
				Integer driversLicence = new Integer(entries[columnNumbers.get(DRIVERS_LICENCE)]);
	
								
				personId = Id.create(householdId + "_" + personId, Person.class);
				Person person = population.getFactory().createPerson(Id.create(personId, Person.class));
				
				if (age >= 0) {
					personAttributes.putAttribute(person.getId().toString(), "age", age);
				} else {
					personAttributes.putAttribute(person.getId().toString(), "age", -1);
					log.warn("Age is not a positive number.");
				}
				
				if (sex == 1 || sex == 2) {
					personAttributes.putAttribute(person.getId().toString(), "sex", sex);
				} else {
					personAttributes.putAttribute(person.getId().toString(), "sex", -1);
					log.warn("Sex is neither male nor female.");
				}
				
				if (employed == 8 || employed == 9 || employed == 10 || employed == 11 ) {
					personAttributes.putAttribute(person.getId().toString(), "employed", 1);
				} else if (employed == 1 || employed == 2 || employed == 3 || employed == 4 || 
						employed == 5 || employed == 6 || employed == 7 || employed == 12) {
					personAttributes.putAttribute(person.getId().toString(), "employed", 0);
				} else {
					personAttributes.putAttribute(person.getId().toString(), "employed", -1);
					log.warn("No information on employment.");
				}
				
				if (employed == 7) {
					personAttributes.putAttribute(person.getId().toString(), "student", 1);
				} else if (employed == -9 || employed == -10) {
					personAttributes.putAttribute(person.getId().toString(), "student", -1);
					log.warn("No information on being student.");
				} else {
					personAttributes.putAttribute(person.getId().toString(), "student", 0);
				}
				
				if (driversLicence == 1) {
					personAttributes.putAttribute(person.getId().toString(), "driversLicence", 1);
				} else if (driversLicence == 2) {
					personAttributes.putAttribute(person.getId().toString(), "driversLicence", 0);
				} else {
					personAttributes.putAttribute(person.getId().toString(), "driversLicence", 0);
					log.warn("No information on driver's licence.");
				}
			}
		} catch (IOException e) {
			log.error(new Exception(e));
		}
	}
	
	
	public ObjectAttributes getPersonAttributes() {
		return this.personAttributes;
	}
}