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

package playground.johannes.studies.matrix2014.source.mid2008;

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.ConvertRide2Car;
import playground.johannes.gsv.synPop.DeleteModes;
import playground.johannes.gsv.synPop.DeleteNoLegs;
import playground.johannes.gsv.synPop.analysis.DeleteShortLongTrips;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.source.mid2008.processing.AdjustJourneyWeight;
import playground.johannes.synpop.source.mid2008.processing.ReturnEpisodeTask;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class MergePopulations {

    private static final Logger logger = Logger.getLogger(MergePopulations.class);

    public static void main(String args[]) {
        Factory factory = new PlainFactory();
        Set<? extends Person> tripPersons = PopulationIO.loadFromXML(args[0], factory);
        Set<? extends Person> journeyPersons = PopulationIO.loadFromXML(args[1], factory);

        TaskRunner.validateEpisodes(new FilterLegDistance(), tripPersons);

        logger.info("Adjusting weights...");
        new ReweightJourneys().apply(journeyPersons);
        TaskRunner.run(new AdjustJourneyWeight(), journeyPersons);

        logger.info("Adding return episodes...");
        TaskRunner.run(new ReturnEpisodeTask(), journeyPersons);

        Set<Person> persons = new HashSet<>();
        persons.addAll(tripPersons);
        persons.addAll(journeyPersons);

        logger.info("Converting ride legs to car legs...");
        TaskRunner.run(new ConvertRide2Car(), persons);
//        TaskRunner.run(new InputeDaysTask(persons), persons);
//        logger.info("Converting activities to misc type...");
//        TaskRunner.run(new Convert2MiscType(), persons);

        logger.info("Removing non mobile persons...");
        TaskRunner.validatePersons(new DeleteNoLegs(), persons);
        logger.info(String.format("Persons after filter: %s", persons.size()));
//        writer.write(outDir + "pop.mob.xml", persons);

        logger.info("Removing non car persons...");
        TaskRunner.validatePersons(new DeleteModes("car"), persons);
        logger.info(String.format("Persons after filter: %s", persons.size()));
//        writer.write(outDir + "pop.car.xml", persons);


//        logger.info("Removing legs with less than 3 KM...");
//        TaskRunner.run(new DeleteShortLongTrips(3000, true), persons);
//        TaskRunner.validatePersons(new DeleteNoLegs(), persons);
//        logger.info(String.format("Persons after filter: %s", persons.size()));

        logger.info("Removing legs with more than 1000 KM...");
        TaskRunner.run(new DeleteShortLongTrips(1000000, false), persons);
        TaskRunner.validatePersons(new DeleteNoLegs(), persons);
        logger.info(String.format("Persons after filter: %s", persons.size()));

        PopulationIO.writeToXML(args[2], persons);
    }
}
