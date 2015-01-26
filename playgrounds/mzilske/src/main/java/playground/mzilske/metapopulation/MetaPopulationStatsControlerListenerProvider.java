/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulationStatsControlerListener.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.metapopulation;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import playground.mzilske.ant2014.StreamingOutput;
import playground.mzilske.util.IterationSummaryFileControlerListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

class MetaPopulationStatsControlerListenerProvider implements javax.inject.Provider<ControlerListener> {

    @javax.inject.Inject
    OutputDirectoryHierarchy controlerIO;

    @javax.inject.Inject
    MetaPopulations metaPopulations;

    @Override
    public ControlerListener get() {
        Map<String, IterationSummaryFileControlerListener.Writer> things = new HashMap<>();
        things.put("metapopulationplans.txt", new IterationSummaryFileControlerListener.Writer() {
            @Override
            public StreamingOutput notifyStartup(StartupEvent event) {
                return new StreamingOutput() {
                    @Override
                    public void write(PrintWriter pw) throws IOException {
                        pw.printf("%s\t%s\t%s\t%s\n",
                                "iteration",
                                "metapopulation",
                                "scalefactor",
                                "score");
                    }
                };
            }

            @Override
            public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                return new StreamingOutput() {
                    @Override
                    public void write(PrintWriter pw) throws IOException {
                        int i = 0;
                        for (MetaPopulation countLink : metaPopulations.getMetaPopulations()) {
                            for (MetaPopulationPlan plan : countLink.getPlans()) {
                                pw.printf("%d\t%d\t%f\t%f\n",
                                        event.getIteration(),
                                        i,
                                        plan.getScaleFactor(),
                                        plan.getScore());
                            }
                            i++;
                        }
                    }
                };
            }
        });
        return new IterationSummaryFileControlerListener(controlerIO, things);
    }
}
