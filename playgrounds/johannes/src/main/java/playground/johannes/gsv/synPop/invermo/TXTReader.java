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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.*;
import playground.johannes.gsv.synPop.analysis.DeleteShortLongTrips;
import playground.johannes.gsv.synPop.invermo.sim.InitializeTargetDistance;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.source.mid2008.processing.TaskRunner;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.source.mid2008.generator.InsertActivitiesTask;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;
import playground.johannes.synpop.source.mid2008.generator.RowHandler;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class TXTReader {
	
	private static final Logger logger = Logger.getLogger(TXTReader.class);
	
	private Map<String, PlainElement> households;
	
	private Map<String, PlainPerson> persons;
	
	private List<AttributeHandler<PlainElement>> householdAttHandlers = new ArrayList<AttributeHandler<PlainElement>>();
	
	private List<PersonAttributeHandler> personAttHandlers = new ArrayList<PersonAttributeHandler>();
	
//	private List<AttributeHandler<PlainEpisode>> planAttHandlers = new ArrayList<AttributeHandler<PlainEpisode>>();
	private LegHandlerAdaptor legAdaptor = new LegHandlerAdaptor();

	public Collection<PlainPerson> read(String rootDir) throws IOException {
		households = new LinkedHashMap<String, PlainElement>(5000);
		persons = new LinkedHashMap<String, PlainPerson>(65000);
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
//		pHandler.setSeparator(",");
		pHandler.setColumnOffset(0);
		pHandler.read(rootDir + "pw1.txt");
		pHandler.read(rootDir + "pw2.txt");
		pHandler.read(rootDir + "pw3.txt");
		pHandler.read(rootDir + "p4.txt");
//		/*
//		 * add an empty plan to each person
//		 */
//		for(PlainPerson person : persons.values()) {
//			person.setPlan(new PlainEpisode());
//		}
//		/*
//		 * read and create legs
//		 */
//		new LegRowHandler().read(legFile);
		
		RowHandler jHandler = new JourneyRowHandler();
//		jHandler.setSeparator(",");
		jHandler.setColumnOffset(0);
		jHandler.read(rootDir + "rw1.txt");
		jHandler.read(rootDir + "rw2.txt");
//		jHandler.read(rootDir + "rw3.txt");
//		jHandler.read(rootDir + "r4.txt");
		
		return new HashSet<PlainPerson>(persons.values());
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
			String id = attributes.get(ColumnKeys.HOUSEHOLD_ID);
			PlainElement household = households.get(id);
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
			PlainPerson person = persons.get(id);
			if(person == null) {
				if(attributes.get(ColumnKeys.PERSON_ID).equals("1")) { 
					person = new PlainPerson(id);
					persons.put(person.getId(), person);
				}
			}
			
			if(person == null)
				return;
//			PlainPerson person = new PlainPerson(id);
//			persons.put(person.getId(), person);
			
			PlainElement household = households.get(attributes.get(ColumnKeys.HOUSEHOLD_ID));
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
		
			
		}
	}
	
	private class JourneyRowHandler extends RowHandler {

		@Override
		protected void handleRow(Map<String, String> attributes) {
			String id = personIdBuilder(attributes);
			PlainPerson person = persons.get(id);
			
			if (person == null) {
				logger.warn(String.format("Person %s not found.", id));
			} else {
				String planId = attributes.get("Reisenr");
				Episode thePlan = null;
//				for(PlainEpisode plan : person.getEpisodes()) {
//					if(plan.getAttribute("id").equals(planId)) {
//						thePlan = plan;
//						break;
//					}
//				}
				if(thePlan == null) {
					thePlan = new PlainEpisode();
					thePlan.setAttribute("id", planId);
					person.addEpisode(thePlan);
					thePlan.setAttribute("datasource", "invermo");
				}
				
				legAdaptor.handleAttribute(thePlan, attributes);

				
			}
			
		}
		
	}
	
	public static void main(String args[]) throws IOException {
		TXTReader reader = new TXTReader();
//		reader.planAttHandlers.add(new LegHandlerAdaptor());
		reader.householdAttHandlers.add(new HouseholdLocationHandler());
		reader.householdAttHandlers.add(new HouseholdWeigthHandler());
		reader.personAttHandlers.add(new WorkLocationHandler());
		reader.legAdaptor.addHandler(new LegStartLocHandler());
		reader.legAdaptor.addHandler(new LegDestinationLocHandler());
		reader.legAdaptor.addHandler(new LegEndTimeHandler());
		reader.legAdaptor.addHandler(new LegStartTimeHandler());
		reader.legAdaptor.addHandler(new LegModeHandler());
		reader.legAdaptor.addHandler(new LegPurposeHandler());
		Collection<PlainPerson> persons = reader.read("/home/johannes/gsv/invermo/txt-utf8/");
		
		logger.info(String.format("Parsed %s persons.", persons.size()));
		
		EpisodeTaskComposite composite = new EpisodeTaskComposite();
		composite.addComponent(new InsertActivitiesTask(new PlainFactory()));
		composite.addComponent(new ValidateDatesTask());
		composite.addComponent(new ComposeTimeTask());
		composite.addComponent(new SetActivityLocations());
		composite.addComponent(new CleanLegLocations());
		composite.addComponent(new SetActivityTypes());
		composite.addComponent(new CleanLegPurposes());
		composite.addComponent(new InfereVacationsType());

//		composite.addComponent(new SetFirstActivityTypeTask());
//		composite.addComponent(new RoundTripTask());
		
		
		logger.info("Applying person tasks...");
		TaskRunner.run(composite, persons);
		
		PersonTaskComposite personTasks = new PersonTaskComposite();
		
		GeocodeLocationsTask geoTask = new GeocodeLocationsTask("localhost", 3128);
		geoTask.setCacheFile("/home/johannes/gsv/invermo/txt-utf8/geocache.txt");
		
		Plans2PersonsTask plans2Persons = new Plans2PersonsTask();
		
		
		personTasks.addComponent(new SplitPlanTask());
		personTasks.addComponent(new InsertHomePlanTask());
		personTasks.addComponent(new ReplaceLocationAliasTask());
		personTasks.addComponent(geoTask);
//		personTasks.addComponent(plans2Persons);
		
		
		TaskRunner.run(personTasks, persons);
		
		geoTask.writeCache();
		
		new DeleteNoWeight().apply(persons);
		
		TaskRunner.run(plans2Persons, persons);
		for(PlainPerson person : plans2Persons.getNewPersons()) {
			persons.add(person);
		}
		
		
//		new ApplySampleProbas(82000000).apply(persons);
//		
//		double wsum = 0;
//		for(PlainPerson person : persons) {
//			wsum += Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
//		}
//		logger.info(String.format("Sum of weigths = %s.", wsum));
		
		composite = new EpisodeTaskComposite();
		composite.addComponent(new Date2TimeTask());
		composite.addComponent(new SetActivityTimeTask());
		composite.addComponent(new FixMissingActTimesTask());
		composite.addComponent(new ValidateActTimesTask());
		composite.addComponent(new SetLegTimes());
		composite.addComponent(new SetMissingActTypes());
		
		TaskRunner.run(composite, persons);
		
		TaskRunner.run(new CopyDate2PersonTask(), persons);
		
		logger.info("Done.");
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/invermo/pop.xml", persons);
		
		logger.info("Deleting plans with out of bounds trips.");
		Geometry bounds = (Geometry) FeatureSHP.readFeatures("/home/johannes/gsv/synpop/data/gis/nuts/de.nuts0.shp").iterator().next().getDefaultGeometry();
		TaskRunner.runAndDeleteEpisode(new DeleteOutOfBounds(bounds), persons);
		persons = TaskRunner.runAndDeletePerson(new DeleteNoPlans(), persons);
		logger.info("Population size = " + persons.size());
		
//		logger.info("Cloning persons...");
//		persons = PersonCloner.weightedClones(persons, 3000000, new XORShiftRandom());
//		new ApplySampleProbas(82000000).apply(persons);
//		logger.info("Population size = " + persons.size());
				
		logger.info("Deleting person with no legs...");
		persons = TaskRunner.runAndDeletePerson(new DeleteNoLegs(), persons);
		logger.info("Population size = " + persons.size());
		
		logger.info("Initializing target distances...");
		TaskRunner.run(new InitializeTargetDistance(), persons);
		
		writer.write("/home/johannes/gsv/invermo/pop.de.xml", persons);
		
		logger.info("Deleting persons with no car legs...");
		persons = TaskRunner.runAndDeletePerson(new DeleteModes("car"), persons);
		logger.info("Population size = " + persons.size());
		
		
		
		logger.info("Deleting persons with legs more than 1000 KM...");
		DeleteShortLongTrips task = new DeleteShortLongTrips(100000, false);
		for (PlainPerson person : persons) {
			task.apply(person.getEpisodes().get(0));
		}
		logger.info("Population size = " + persons.size());
		
		writer.write("/home/johannes/gsv/invermo/pop.de.car.xml", persons);
	}
}
