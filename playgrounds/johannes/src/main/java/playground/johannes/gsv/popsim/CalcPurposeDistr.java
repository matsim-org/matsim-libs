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

package playground.johannes.gsv.popsim;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.gsv.matrices.plans2matrix.ReplaceMiscType;
import playground.johannes.gsv.synPop.ConvertRide2Car;
import playground.johannes.gsv.synPop.DeleteNoLegs;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class CalcPurposeDistr {

    private static final Logger logger = Logger.getLogger(CalcPurposeDistr.class);

    public static void main(String args[]) throws IOException {
        Factory factory = new PlainFactory();
        Set<? extends Person> tripPersons = PopulationIO.loadFromXML("/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.midtrips.validated.xml", factory);
        Set<? extends Person> journeyPersons = PopulationIO.loadFromXML("/home/johannes/gsv/germany-scenario/mid2008/pop/mid2008.midjourneys.validated.xml", factory);

        Set<PlainPerson> persons = new HashSet<>();
        persons.addAll((Collection<? extends PlainPerson>) tripPersons);
        persons.addAll((Collection<? extends PlainPerson>) journeyPersons);

        logger.info("Converting ride legs to car legs...");
        TaskRunner.run(new ConvertRide2Car(), persons);
        TaskRunner.run(new InputeDaysTask(persons), persons);
//        logger.info("Converting activities to misc type...");
//        TaskRunner.run(new Convert2MiscType(), persons);

        logger.info("Removing non mobile persons...");
        TaskRunner.validatePersons(new DeleteNoLegs(), persons);
        logger.info(String.format("Persons after filter: %s", persons.size()));
//        writer.write(outDir + "pop.mob.xml", persons);

        TaskRunner.run(new ReplaceActTypes(), persons);
//        new GuessMissingActTypes(random).apply(refPersons);
        TaskRunner.run(new Route2GeoDistance(new Simulator.Route2GeoDistFunction()), persons);

        logger.info("Replacing misc types...");
        new ReplaceMiscType().apply(persons);
//        logger.info("Removing non car persons...");
//        TaskRunner.validatePersons(new DeleteModes("car"), persons);
//        logger.info(String.format("Persons after filter: %s", persons.size()));
//        writer.write(outDir + "pop.car.xml", persons);


//        logger.info("Removing legs with less than 3 KM...");
//        TaskRunner.run(new DeleteShortLongTrips(3000, true), persons);
//        TaskRunner.validatePersons(new DeleteNoLegs(), persons);
//        logger.info(String.format("Persons after filter: %s", persons.size()));
//        PopulationIO.writeToXML(args[2], persons);

        persons = (Set<PlainPerson>) PersonUtils.weightedCopy(persons, new PlainFactory(), 500000, new XORShiftRandom
                ());
        AnalyzerTask task = new DistDayTypeTask();
        ProxyAnalyzer.analyze(persons, task, "/home/johannes/gsv/miv-matrix/purpose-fix/");
    }

    private static class ReplaceActTypes implements EpisodeTask {

        private static Map<String, String> typeMapping;

        public Map<String, String> getTypeMapping() {
            if (typeMapping == null) {
                typeMapping = new HashMap<String, String>();
                typeMapping.put("visit", ActivityTypes.LEISURE);
                typeMapping.put("culture", ActivityTypes.LEISURE);
                typeMapping.put("gastro", ActivityTypes.LEISURE);
                typeMapping.put("private", ActivityTypes.LEISURE);
                typeMapping.put("pickdrop", ActivityTypes.LEISURE);
                typeMapping.put("sport", ActivityTypes.LEISURE);
            }

            return typeMapping;
        }

        @Override
        public void apply(Episode plan) {
            for (Attributable act : plan.getActivities()) {
                    String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    String newType = getTypeMapping().get(type);
                    if (newType != null) {
                        act.setAttribute(CommonKeys.ACTIVITY_TYPE, newType);
                    }

            }

        }
    }
}
