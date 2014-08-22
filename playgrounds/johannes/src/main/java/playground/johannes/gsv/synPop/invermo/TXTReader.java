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

package playground.johannes.gsv.synPop.invermo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.FixActivityTimesTask;
import playground.johannes.gsv.synPop.InsertActivitiesTask;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTaskComposite;
import playground.johannes.gsv.synPop.RoundTripTask;
import playground.johannes.gsv.synPop.SetActivityTimeTask;
import playground.johannes.gsv.synPop.SetActivityTypeTask;
import playground.johannes.gsv.synPop.SetFirstActivityTypeTask;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.PersonAttributeHandler;
import playground.johannes.gsv.synPop.mid.RowHandler;
import playground.johannes.gsv.synPop.mid.run.ProxyTaskRunner;

/**
 * @author johannes
 *
 */
public class TXTReader {
	
	private static final Logger logger = Logger.getLogger(TXTReader.class);
	
	private Map<String, ProxyObject> households;
	
	private Map<String, ProxyPerson> persons;
	
	private List<AttributeHandler<ProxyObject>> householdAttHandlers = new ArrayList<AttributeHandler<ProxyObject>>();
	
	private List<PersonAttributeHandler> personAttHandlers = new ArrayList<PersonAttributeHandler>();
	
//	private List<AttributeHandler<ProxyPlan>> planAttHandlers = new ArrayList<AttributeHandler<ProxyPlan>>();
	private LegHandlerAdaptor legAdaptor = new LegHandlerAdaptor();

	public Collection<ProxyPerson> read(String householdFile, String personFile, String journeyFile) throws IOException {
		households = new LinkedHashMap<String, ProxyObject>(5000);
		persons = new LinkedHashMap<String, ProxyPerson>(65000);
		/*
		 * read and create persons
		 */
		RowHandler hHandler = new HouseholdRowHandler();
		hHandler.setColumnOffset(0);
		hHandler.read(householdFile);
		
		RowHandler pHandler = new PersonRowHandler();
//		pHandler.setSeparator(",");
		pHandler.setColumnOffset(0);
		pHandler.read(personFile);
//		/*
//		 * add an empty plan to each person
//		 */
//		for(ProxyPerson person : persons.values()) {
//			person.setPlan(new ProxyPlan());
//		}
//		/*
//		 * read and create legs
//		 */
//		new LegRowHandler().read(legFile);
		
		RowHandler jHandler = new JourneyRowHandler();
//		jHandler.setSeparator(",");
		jHandler.setColumnOffset(0);
		jHandler.read(journeyFile);
		
		return persons.values();
	}
	
	private String personIdBuilder(Map<String, String> attributes) {
		StringBuilder builder = new StringBuilder(20);
		builder.append(attributes.get(ColumnKeys.HOUSEHOLD_ID));
		builder.append(".");
		builder.append(attributes.get(ColumnKeys.PERSON_ID));
		
		return builder.toString();
	}
	
	private class HouseholdRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			ProxyObject household = new ProxyObject();
			String id = attributes.get(ColumnKeys.HOUSEHOLD_ID);
			households.put(id, household);
			
			for(AttributeHandler<ProxyObject> handler : householdAttHandlers) {
				handler.handleAttribute(household, attributes);
			}
			
		}
		
	}
	private class PersonRowHandler extends RowHandler {
		
		@Override
		protected void handleRow(Map<String, String> attributes) {
			ProxyPerson person = new ProxyPerson(personIdBuilder(attributes));
			ProxyObject household = households.get(attributes.get(ColumnKeys.HOUSEHOLD_ID));
			if(household == null) {
				logger.warn(String.format("Household %s not found.", attributes.get(ColumnKeys.HOUSEHOLD_ID)));
			} else {
				for(Entry<String, String> entry : household.getAttributes().entrySet()) {
					person.setAttribute(entry.getKey(), entry.getValue());
				}
			}
			
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
			ProxyPerson person = persons.get(id);
			
			if (person == null) {
				logger.warn(String.format("Person %s not found.", id));
			} else {
				ProxyPlan plan = new ProxyPlan();
				legAdaptor.handleAttribute(plan, attributes);

				person.addPlan(plan);
			}
			
		}
		
	}
	
	public static void main(String args[]) throws IOException {
		TXTReader reader = new TXTReader();
//		reader.planAttHandlers.add(new LegHandlerAdaptor());
		reader.householdAttHandlers.add(new HouseholdLocationHandler());
		reader.legAdaptor.addHandler(new LegStartLocHandler());
		Collection persons = reader.read("/home/johannes/gsv/invermo/txt/hhw1.csv", "/home/johannes/gsv/invermo/txt/pw1.csv", "/home/johannes/gsv/invermo/txt/rw1.csv");
		
		ProxyPlanTaskComposite composite = new ProxyPlanTaskComposite();
		composite.addComponent(new InsertActivitiesTask());
//		composite.addComponent(new SetActivityTypeTask());
//		composite.addComponent(new SetFirstActivityTypeTask());
//		composite.addComponent(new RoundTripTask());
//		composite.addComponent(new SetActivityTimeTask());
//		composite.addComponent(new FixActivityTimesTask());
		
		logger.info("Applying person tasks...");
		ProxyTaskRunner.run(composite, persons);
		logger.info("Done.");
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/invermo/pop.xml", persons);
	}
}
