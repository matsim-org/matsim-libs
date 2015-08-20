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

package playground.johannes.gsv.synPop.mid.run;

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.*;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.*;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.source.mid2008.generator.PersonAgeHandler;
import playground.johannes.synpop.source.mid2008.generator.PersonCarAvailHandler;
import playground.johannes.synpop.source.mid2008.generator.FileReader;
import playground.johannes.synpop.source.mid2008.generator.PersonDayHandler;

import java.io.IOException;
import java.util.Collection;

/**
 * @author johannes
 *
 */
public class PopulationGenerator {
	
	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String rootDir = "/home/johannes/gsv/germany-scenario/mid2008/raw/";
		String personFile = rootDir + "MiD2008_PUF_Personen.txt";
		String legFile = rootDir + "MiD2008_PUF_Wege.txt";
		String outFile = "/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml";
		String journeyFile = rootDir + "MiD2008_PUF_Reisen.txt";
		/*
		 * setup text parser
		 */
		FileReader reader = new FileReader(new PlainFactory());
		reader.addPersonAttributeHandler(new PersonMunicipalityClassHandler());
		reader.addPersonAttributeHandler(new PersonWeightHandler());
		reader.addPersonAttributeHandler(new PersonDayHandler());
		reader.addPersonAttributeHandler(new PersonStateHandler());
		reader.addPersonAttributeHandler(new PersonMonthHandler());
		reader.addPersonAttributeHandler(new PersonHHIncomeHandler());
		reader.addPersonAttributeHandler(new PersonAgeHandler());
		reader.addPersonAttributeHandler(new PersonHHMembersHandler());
		reader.addPersonAttributeHandler(new PersonSexHandler());
		reader.addPersonAttributeHandler(new PersonCarAvailHandler());
		
		reader.addLegAttributeHandler(new LegSortedIdHandler());
		reader.addLegAttributeHandler(new LegMainPurposeHandler());
		reader.addLegAttributeHandler(new LegOriginHandler());
		reader.addLegAttributeHandler(new LegRoundTrip());
		reader.addLegAttributeHandler(new LegStartTimeHandler());
		reader.addLegAttributeHandler(new LegEndTimeHandler());
		reader.addLegAttributeHandler(new LegDistanceHandler());
		reader.addLegAttributeHandler(new LegModeHandler());
		
		reader.addJourneyAttributeHandler(new JourneyDestinationHandler());
		reader.addJourneyAttributeHandler(new JourneyDistanceHandler());
		reader.addJourneyAttributeHandler(new JourneyModeHandler());
		reader.addJourneyAttributeHandler(new JourneyPurposeHandler());
		
		reader.addEpisodeAttributeHandler(new JourneyDaysHandler());
		/*
		 * read files
		 */
		logger.info("Reading persons...");
		Collection<PlainPerson> persons = (Collection<PlainPerson>)reader.read(personFile, legFile,
				journeyFile);
		logger.info(String.format("Read %s persons.", persons.size()));
		/*
		 * sort legs
		 */
		ProxyPlanTaskComposite composite = new ProxyPlanTaskComposite();
//		ProxyPlanTaskComposite composite = new ConstrainedPlanTaskComposite("datasource", "midtrips");
		composite.addComponent(new SortLegsTimeTask());
		logger.info("Sorting legs...");
		ProxyTaskRunner.run(new ConstrainedPlanTask("datasource", "midtrips", composite), persons);
		/*
		 * filter legs		
		 */
		ProxyPersonTaskComposite pComposite = new ProxyPersonTaskComposite();
		pComposite.addComponent(new DeleteNegativeDurationTask());
		pComposite.addComponent(new DeleteMissingTimesTask());
		pComposite.addComponent(new DeleteOverlappingLegsTask());
		
		logger.info(String.format("Filtering %s legs...", persons.size()));
		persons = ProxyTaskRunner.runAndDeletePerson(new ConstrainedPersonTask("datasource", "midtrips", pComposite), persons);
		logger.info(String.format("After filter: %s persons.", persons.size()));
		/*
		 * generate activities
		 */
		composite = new ProxyPlanTaskComposite();
		composite.addComponent(new InsertActivitiesTask());
		composite.addComponent(new SetActivityTypeTask());
		composite.addComponent(new SetFirstActivityTypeTask());
		composite.addComponent(new RoundTripTask());
		composite.addComponent(new SetActivityTimeTask());
		composite.addComponent(new FixMissingActTimesTask());
		
		logger.info("Applying person tasks...");
		ProxyTaskRunner.run(new ConstrainedPlanTask("datasource", "midtrips", composite), persons);
		logger.info("Done.");
		/*
		 * process journeys
		 */
		composite = new ProxyPlanTaskComposite();
		composite.addComponent(new InsertActivitiesTask());
		composite.addComponent(new SetActivityTypeTask());
		composite.addComponent(new SetFirstActivityTypeTask());
		composite.addComponent(new InfereVacationsType());
		ProxyTaskRunner.run(new ConstrainedPlanTask("datasource", "midjourneys", composite), persons);
		
		ProxyTaskRunner.run(new DeletePlansDestination(), persons);
		
		ProxyTaskRunner.run(new AddReturnPlan(), persons);
		
		JourneyPlans2PersonTask plans2persons = new JourneyPlans2PersonTask(); 
		ProxyTaskRunner.run(plans2persons, persons);
		for(PlainPerson person : plans2persons.getPersons()) {
			persons.add(person);
		}
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(outFile, persons);
		logger.info("Done.");
		
	}

}
