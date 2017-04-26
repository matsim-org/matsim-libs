/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.analysis.run;

import playground.johannes.studies.matrix2014.analysis.SetSeason;
import playground.johannes.studies.matrix2014.matrix.postprocess.SeasonTask;
import playground.johannes.synpop.analysis.AnalyzerTaskComposite;
import playground.johannes.synpop.analysis.AnalyzerTaskRunner;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.processing.TaskRunner;

import java.util.Set;

/**
 * @author johannes
 */
public class AnalyseInfermo {

    public static void main(String args[]) {
        Set<Person> persons = PopulationIO.loadFromXML("/Users/johannes/gsv/germany-scenario/invermo/pop2/pop.validated.xml", new PlainFactory());

        FileIOContext ioContext = new FileIOContext("/Users/johannes/gsv/germany-scenario/invermo/analysis");

        TaskRunner.run(new InputeDummyDistance(), persons);
        TaskRunner.run(new SetSeason(), persons);

        AnalyzerTaskComposite tasks = new AnalyzerTaskComposite();
        tasks.addComponent(new SeasonTask(ioContext));
        tasks.addComponent(new DayTask(ioContext));

        AnalyzerTaskRunner.run(persons, tasks, ioContext);
    }

    private static class InputeDummyDistance implements EpisodeTask {

        @Override
        public void apply(Episode episode) {
            for(Segment leg : episode.getLegs()) {
                leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, "100000");
            }
        }
    }

//    private static class InputeLegPurpose implements EpisodeTask {
//
//        @Override
//        public void apply(Episode episode) {
//            for(Segment leg : episode.getLegs()) {
//                if(ActivityTypes.HOME.equalsIgnoreCase(leg.next().getAttribute(CommonKeys.ACTIVITY_TYPE))) {
//                    leg.setAttribute(CommonKeys.LEG_PURPOSE, leg.previous().getAttribute(CommonKeys.ACTIVITY_TYPE));
//                } else {
//                    leg.setAttribute(CommonKeys.LEG_PURPOSE, leg.next().getAttribute(CommonKeys.ACTIVITY_TYPE));
//                }
//            }
//        }
//    }
}
