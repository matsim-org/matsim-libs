/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.invermo.generator;

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.invermo.LegHandlerAdaptor;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.source.invermo.InvermoKeys;
import playground.johannes.synpop.source.invermo.InvermoValues;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;
import playground.johannes.synpop.source.mid2008.generator.RowHandler;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class FileReader {
	
	private static final Logger logger = Logger.getLogger(FileReader.class);
	
	private Map<String, Household> households;
	
	private Map<String, Person> persons;
	
	private final List<AttributeHandler<PlainElement>> householdAttHandlers = new ArrayList<>();
	
	private final List<PersonAttributeHandler> personAttHandlers = new ArrayList<>();
	
	private final LegHandlerAdaptor legAdaptor = new LegHandlerAdaptor();

	private Factory factory;

	public FileReader(Factory factory) {
		this.factory = factory;
	}

	public void addHousholdAttributeHandler(AttributeHandler<PlainElement> handler) {
		householdAttHandlers.add(handler);
	}

	public void addPersonAttributeHandler(PersonAttributeHandler handler) {
		personAttHandlers.add(handler);
	}

	public void addLegAttributeHandler(LegAttributeHandler handler) {
		legAdaptor.addHandler(handler);
	}

	public Collection<Person> read(String rootDir) throws IOException {
		households = new LinkedHashMap<>(5000);
		persons = new LinkedHashMap<>(65000);
		/*
		 * read and create persons
		 */
		RowHandler hHandler = new HouseholdRowHandler();
		hHandler.setColumnOffset(0);
		hHandler.read(rootDir + "hhw1.txt");
		hHandler.read(rootDir + "hhw2.txt");
		hHandler.read(rootDir + "hhw3.txt");
		hHandler.read(rootDir + "hh4.txt");
		hHandler.read(rootDir + "mobgewichtw1.txt");
		hHandler.read(rootDir + "mobgewichtw2.txt");
		hHandler.read(rootDir + "mobgewichtw3.txt");
		
		RowHandler pHandler = new PersonRowHandler();
		pHandler.setColumnOffset(0);
		pHandler.read(rootDir + "pw1.txt");
		pHandler.read(rootDir + "pw2.txt");
		pHandler.read(rootDir + "pw3.txt");
		pHandler.read(rootDir + "p4.txt");

		RowHandler jHandler = new JourneyRowHandler();
		jHandler.setColumnOffset(0);
		jHandler.read(rootDir + "rw1.txt");
		jHandler.read(rootDir + "rw2.txt");
		jHandler.read(rootDir + "rw3.txt");
		jHandler.read(rootDir + "r4.txt");
		
		return new HashSet<>(persons.values());
	}
	
	private String personIdBuilder(Map<String, String> attributes) {
		StringBuilder builder = new StringBuilder(20);
		builder.append(attributes.get(VariableNames.HOUSEHOLD_ID));
		builder.append(".");
		builder.append(attributes.get(VariableNames.PERSON_ID));
		
		return builder.toString();
	}
	
	private class HouseholdRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = attributes.get(VariableNames.HOUSEHOLD_ID);
			Household household = households.get(id);
			if(household == null) {
				household = new Household();
				households.put(id, household);
			}
			
			for(AttributeHandler<PlainElement> handler : householdAttHandlers) {
				handler.handleAttribute(household, attributes);
			}
			
		}
		
	}

	private class Household extends PlainElement {

	}

	private class PersonRowHandler extends RowHandler {
		
		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = personIdBuilder(attributes);
			Person person = persons.get(id);
			if(person == null) {
				/*
				Create only first household person
				 */
				if(attributes.get(VariableNames.PERSON_ID).equals("1")) {
					person = factory.newPerson(id);
					persons.put(person.getId(), person);
				}
			}
			
			if(person == null)
				return;

			/*
			Copy household attributes to person.
			 */
			Household household = households.get(attributes.get(VariableNames.HOUSEHOLD_ID));
			if(household == null) {
				logger.warn(String.format("Household %s not found.", attributes.get(VariableNames.HOUSEHOLD_ID)));
			} else {
				for(Entry<String, String> entry : household.getAttributes().entrySet()) {
					person.setAttribute(entry.getKey(), entry.getValue());
				}
			}
			
			for(PersonAttributeHandler handler : personAttHandlers) {
				handler.handle(person, attributes);
			}
		
			
		}
	}
	
	private class JourneyRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = personIdBuilder(attributes);
			Person person = persons.get(id);
			
			if (person == null) {
				logger.warn(String.format("Person %s not found.", id));
			} else {
				String planId = attributes.get(VariableNames.TRIP_ID);
				Episode episode = null;
				for(Episode e : person.getEpisodes()) {
					if(e.getAttribute(InvermoKeys.PLAN_ID).equals(planId)) {
						episode = e;
						break;
					}
				}
				/*
				No episode found, create new one.
				 */
				if(episode == null) {
					episode = new PlainEpisode();
					episode.setAttribute(InvermoKeys.PLAN_ID, planId);
					person.addEpisode(episode);
					episode.setAttribute(CommonKeys.DATA_SOURCE, InvermoValues.INVERMO);

				}
				
				legAdaptor.handleAttribute(episode, attributes);

			}

		}
	}
}
