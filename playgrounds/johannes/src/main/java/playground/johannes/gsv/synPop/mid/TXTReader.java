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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainEpisode;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.PlainSegment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class TXTReader {
	
	private final static String DOT = ".";

	private String separator = "\t";
	
	private List<PersonAttributeHandler> personAttHandlers = new ArrayList<PersonAttributeHandler>();
	
	private List<LegAttributeHandler> legAttHandlers = new ArrayList<LegAttributeHandler>();
	
	private List<LegAttributeHandler> journeyAttHandlers = new ArrayList<>();
	
	private List<PlanAttributeHandler> planAttHandlers = new ArrayList<>();
	
	private Map<String, PlainPerson> persons;
	

	public void addPersonAttributeHandler(PersonAttributeHandler handler) {
		personAttHandlers.add(handler);
	}
	
	public void addLegAttributeHandler(LegAttributeHandler handler) {
		legAttHandlers.add(handler);
	}

	public void addJourneyAttributeHandler(LegAttributeHandler handler) {
		journeyAttHandlers.add(handler);
	}
	
	public void addPlanAttributeHandler(PlanAttributeHandler handler) {
		planAttHandlers.add(handler);
	}
	
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public Map<String, PlainPerson> read(String personFile, String legFile, String journeyFile) throws IOException {
		persons = new LinkedHashMap<String, PlainPerson>(65000);
		/*
		 * read and create persons
		 */
		new PersonRowHandler().read(personFile);
		/*
		 * add an empty plan to each person
		 */
		for(PlainPerson person : persons.values()) {
			Episode plan = new PlainEpisode();
			plan.setAttribute(CommonKeys.DATA_SOURCE, MIDKeys.MID_TRIPS);
			person.setPlan(plan);
		}
		/*
		 * read and create legs
		 */
		new LegRowHandler().read(legFile);
		/*
		 * read and create journeys
		 */
		new JourneyRowHandler().read(journeyFile);
		
		return persons;
	}
	
	private String personIdBuilder(Map<String, String> attributes) {
		StringBuilder builder = new StringBuilder(20);
		builder.append(attributes.get(MIDKeys.HOUSEHOLD_ID));
		builder.append(DOT);
		builder.append(attributes.get(MIDKeys.PERSON_ID));
		
		return builder.toString();
	}
	
	private class LegRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = personIdBuilder(attributes);
			PlainPerson person = persons.get(id);

			PlainSegment leg = new PlainSegment();
			for(LegAttributeHandler handler : legAttHandlers)
				handler.handle(leg, attributes);
			
			person.getPlan().addLeg(leg);
		}
		
	}
	
	private class PersonRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			PlainPerson person = new PlainPerson(personIdBuilder(attributes));
			
			for(PersonAttributeHandler handler : personAttHandlers) {
				handler.handle(person, attributes);
			}
		
			persons.put(person.getId(), person);
		}
		
	}
	
	private class JourneyRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = personIdBuilder(attributes);
			PlainPerson person = persons.get(id);

			Episode plan = new PlainEpisode();
			plan.setAttribute(CommonKeys.DATA_SOURCE, MIDKeys.MID_JOUNREYS);
			for(PlanAttributeHandler handler : planAttHandlers) {
				handler.hanle(plan, attributes);
			}
			
			person.addEpisode(plan);

			PlainSegment leg = new PlainSegment();
			plan.addLeg(leg);
			leg.setAttribute(CommonKeys.LEG_ORIGIN, ActivityType.HOME);
			for(LegAttributeHandler handler : journeyAttHandlers) {
				handler.handle(leg, attributes);
			}
		}
		
	}
	
//	private abstract class RowHandler {
//	
//		protected abstract void handleRow(Map<String, String> attributes);
//		
//		public void read(String file) throws IOException {
//			BufferedReader reader = new BufferedReader(new FileReader(file));
//			
//			String line = reader.readLine();
//			String keys[] = line.split(separator, -1);
//			Map<String, String> attributes = new HashMap<String, String>(keys.length);
//			
//			int lineCount = 1;
//			while((line = reader.readLine()) != null) {
//				String tokens[] = line.split(separator, -1);
//				
//				if(tokens.length - 1 > keys.length) // -1 because rows are numbered
//					throw new RuntimeException(String.format("Line %s has more fields (%s) than available keys (%s).", lineCount, tokens.length, keys.length));
//				
//				for(int i = 1; i < tokens.length; i++) {
//					attributes.put(keys[i - 1], tokens[i]);
//				}
//		
//				handleRow(attributes);
//			}
//			
//			reader.close();
//		}
//	}
}
