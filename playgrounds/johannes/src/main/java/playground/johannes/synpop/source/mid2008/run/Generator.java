/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.mid2008.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.IsolateEpisodes;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.source.mid2008.generator.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class Generator {

    private static final Logger logger = Logger.getLogger(Generator.class);

    public static final String MODULE_NAME = "mid2008Generator";

    public static final String PERSONS_FILE = "personsFile";

    public static final String TRIPS_FILE = "tripsFile";

    public static final String JOURNEYS_FILE = "journeysFile";

    public static final String OUTPUT_DIR = "output";

    public static void main(String args[]) throws IOException {
        Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        String personsFile = config.getParam(MODULE_NAME, PERSONS_FILE);
        String tripsFile = config.getParam(MODULE_NAME, TRIPS_FILE);
        String journeysFile = config.getParam(MODULE_NAME, JOURNEYS_FILE);
        String outDir = config.getParam(MODULE_NAME, OUTPUT_DIR);

        PlainFactory factory = new PlainFactory();
        FileReader fileReader = new FileReader(factory);

        fileReader.addPersonAttributeHandler(new PersonAgeHandler());
        fileReader.addPersonAttributeHandler(new PersonCarAvailHandler());
        fileReader.addPersonAttributeHandler(new PersonDayHandler());
        fileReader.addPersonAttributeHandler(new PersonHHIncomeHandler());
        fileReader.addPersonAttributeHandler(new PersonHHMembersHandler());
        fileReader.addPersonAttributeHandler(new PersonMonthHandler());
        fileReader.addPersonAttributeHandler(new PersonMunicipalityClassHandler());
        fileReader.addPersonAttributeHandler(new PersonSexHandler());
        fileReader.addPersonAttributeHandler(new PersonNUTS1Handler());
        fileReader.addPersonAttributeHandler(new PersonWeightHandler());

        fileReader.addLegAttributeHandler(new LegDistanceHandler());
        fileReader.addLegAttributeHandler(new LegTimeHandler());
        fileReader.addLegAttributeHandler(new LegPurposeHandler());
        fileReader.addLegAttributeHandler(new LegDestinationHandler());
        fileReader.addLegAttributeHandler(new LegOriginHandler());
        fileReader.addLegAttributeHandler(new LegModeHandler());
        fileReader.addLegAttributeHandler(new LegIndexHandler());

        fileReader.addJourneyAttributeHandler(new JourneyDistanceHandler());
        fileReader.addJourneyAttributeHandler(new JourneyModeHandler());
        fileReader.addJourneyAttributeHandler(new JourneyPurposeHandler());
        fileReader.addJourneyAttributeHandler(new JourneyDestinationHandler());

        fileReader.addEpisodeAttributeHandler(new JourneyDaysHandler());

        logger.info("Generating persons...");
        Set<PlainPerson> persons = (Set<PlainPerson>)fileReader.read(personsFile, tripsFile, journeysFile);
        logger.info(String.format("Generated %s persons.", persons.size()));

        logger.info("Inserting dummy activities...");
        TaskRunner.run(new InsertActivitiesTask(factory), persons);

        logger.info("Writing persons...");
        PopulationIO.writeToXML(String.format("%s/mid2008.xml", outDir), persons);

        logger.info("Isolating persons...");
        IsolateEpisodes isolator = new IsolateEpisodes(CommonKeys.DATA_SOURCE, factory);
        TaskRunner.run(isolator, persons);
        Map<String, Set<Person>> populations = isolator.getPopulations();
        for(Map.Entry<String, Set<Person>> entry : populations.entrySet()) {
            logger.info(String.format("Writing persons %s...", entry.getKey()));
            PopulationIO.writeToXML(String.format("%s/mid2008.%s.xml", outDir, entry.getKey()), entry.getValue());
        }

        logger.info("Done.");
    }
}
