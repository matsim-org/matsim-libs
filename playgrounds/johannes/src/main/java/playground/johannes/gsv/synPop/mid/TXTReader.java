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
import java.util.List;
import java.util.Map;

import playground.johannes.gsv.synPop.ProxyLeg;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;

/**
 * @author johannes
 *
 */
public class TXTReader {

	private String separator = "\t";
	
	private List<PersonAttributeHandler> personHandlers = new ArrayList<PersonAttributeHandler>();
	
	private List<LegAttributeHandler> legHandlers = new ArrayList<LegAttributeHandler>();
	
	private Map<String, ProxyPerson> persons;
	
	private ProxyBuilder builder = new ProxyBuilder();
	
	public Map<String, ProxyPerson> read(String personFile, String legFile) throws IOException {
		ProxyBuilder builder = new ProxyBuilder();
		
		PersonRowHandler handler = new PersonRowHandler(builder);
		handler.read(personFile);
		
		for(ProxyPerson person : handler.getPersons().values()) {
			person.setPlan(new ProxyPlan());
		}
		

		legHandlers.add(new LegMainPurposeHandler());
		legHandlers.add(new LegOriginHandler());
		legHandlers.add(new LegRoundTrip());
		legHandlers.add(new LegStartTimeHandler());
		legHandlers.add(new LegEndTimeHandler());
		legHandlers.add(new LegDistanceHandler());
		
		LegRowHandler legHandler = new LegRowHandler();
		legHandler.read(legFile);
		
		
		return handler.getPersons();
	}
	
	private class LegRowHandler extends RowHandler {

		/* (non-Javadoc)
		 * @see playground.johannes.gsv.synPop.mid.TXTReader.RowHandler#handleRow(java.util.Map)
		 */
		@Override
		protected void handleRow(Map<String, String> attributes) {
			ProxyLeg leg = builder.addLeg(attributes, persons);
			for(LegAttributeHandler handler : legHandlers)
				handler.handle(leg, attributes);
		}
		
	}
	
	private class PersonRowHandler extends RowHandler {

		
		
		
		public PersonRowHandler(ProxyBuilder builder) {
//			this.builder = builder;
			persons = new HashMap<String, ProxyPerson>(40000);
		}
		
		public Map<String, ProxyPerson> getPersons() {
			return persons;
		}
		
		/* (non-Javadoc)
		 * @see playground.johannes.gsv.synPop.mid.TXTReader.RowHandler#handleRow(java.util.Map)
		 */
		@Override
		protected void handleRow(Map<String, String> attributes) {
			ProxyPerson person = builder.buildPerson(attributes);
			
			for(PersonAttributeHandler handler : personHandlers) {
				handler.handle(person, attributes);
			}
		
			persons.put(person.getId(), person);
		}
		
	}
	
	private abstract class RowHandler {
		
//		private String separator = "\t";
		
		protected abstract void handleRow(Map<String, String> attributes);
		
		public void read(String file) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String line = reader.readLine();
			String keys[] = line.split(separator, -1);
			Map<String, String> attributes = new HashMap<String, String>(keys.length);
			
			int lineCount = 1;
			while((line = reader.readLine()) != null) {
				String tokens[] = line.split(separator, -1);
				
				if(tokens.length - 1 > keys.length) // -1 because rows are numbered
					throw new RuntimeException(String.format("Line %s has more fields (%s) than available keys (%s).", lineCount, tokens.length, keys.length));
				
				for(int i = 1; i < tokens.length; i++) {
					attributes.put(keys[i - 1], tokens[i]);
				}
		
				handleRow(attributes);
			}
		}
	}
}
