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
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.SetActivityTypeTask;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.processing.ValidateNoPlans;
import playground.johannes.synpop.source.mid2008.processing.AdjustJourneyWeight;
import playground.johannes.synpop.source.mid2008.processing.ReturnEpisodeTask;
import playground.johannes.synpop.source.mid2008.processing.SetFirstActivityTypeTask;
import playground.johannes.synpop.source.mid2008.processing.VacationsTypeTask;

import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class JourneysValidator {

    private static final Logger logger = Logger.getLogger(JourneysValidator.class);

    public static final void main(String args[]) {
        Factory factory = new PlainFactory();
        Set<? extends Person> persons = PopulationIO.loadFromXML(args[0], factory);

        TaskRunner.validatePersons(new ValidateNoPlans(), persons);

        logger.info("Setting activity types...");
        TaskRunner.run(new SetActivityTypeTask(), persons);
        logger.info("Setting first activity type...");
        TaskRunner.run(new SetFirstActivityTypeTask(), persons);
        logger.info("Setting vacations type...");
        TaskRunner.run(new VacationsTypeTask(), persons);
        logger.info("Adding return episodes...");
        TaskRunner.run(new ReturnEpisodeTask(), persons);
        logger.info("Adjusting weights...");
        TaskRunner.run(new AdjustJourneyWeight(), persons);

        logger.info("Writing validated population...");
        PopulationIO.writeToXML(args[1], persons);
        logger.info("Done.");
    }

    private static int countActivities(Collection<? extends Person> persons) {
        int cnt = 0;
        for (Person p : persons) {
            for (Episode e : p.getEpisodes()) {
                cnt += e.getActivities().size();
            }
        }

        return cnt;
    }
}
