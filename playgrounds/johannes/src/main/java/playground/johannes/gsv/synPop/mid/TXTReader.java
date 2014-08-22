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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class TXTReader {
	
	private final static String DOT = ".";

	private String separator = "\t";
	
	private List<PersonAttributeHandler> personAttHandlers = new ArrayList<PersonAttributeHandler>();
	
	private List<LegAttributeHandler> legAttHandlers = new ArrayList<LegAttributeHandler>();
	
	private Map<String, ProxyPerson> persons;
	

	public void addPersonAttributeHandler(PersonAttributeHandler handler) {
		personAttHandlers.add(handler);
	}
	
	public void addLegAttributeHandler(LegAttributeHandler handler) {
		legAttHandlers.add(handler);
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public Map<String, ProxyPerson> read(String personFile, String legFile) throws IOException {
		persons = new LinkedHashMap<String, ProxyPerson>(65000);
		/*
		 * read and create persons
		 */
		new PersonRowHandler().read(personFile);
		/*
		 * add an empty plan to each person
		 */
		for(ProxyPerson person : persons.values()) {
			person.setPlan(new ProxyPlan());
		}
		/*
		 * read and create legs
		 */
		new LegRowHandler().read(legFile);
		
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
			ProxyPerson person = persons.get(id);
			
			ProxyObject leg = new ProxyObject();
			for(LegAttributeHandler handler : legAttHandlers)
				handler.handle(leg, attributes);
			
			person.getPlan().addLeg(leg);
		}
		
	}
	
	private class PersonRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			ProxyPerson person = new ProxyPerson(personIdBuilder(attributes));
			
			for(PersonAttributeHandler handler : personAttHandlers) {
				handler.handle(person, attributes);
			}
		
			persons.put(person.getId(), person);
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
