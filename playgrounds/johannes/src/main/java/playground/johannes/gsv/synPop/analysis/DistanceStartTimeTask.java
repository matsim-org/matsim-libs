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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 */
public class DistanceStartTimeTask extends AnalyzerTask {
    @Override
    public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
        TDoubleArrayList distVals = new TDoubleArrayList();
        TDoubleArrayList startVals = new TDoubleArrayList();

        for(Person person : persons) {
            for(Episode plan : person.getEpisodes()) {
                for(Attributable leg : plan.getLegs()) {
                    String xStr = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
                    String startVal = leg.getAttribute(CommonKeys.LEG_START_TIME);

                    if(xStr != null && startVal != null) {
                        distVals.add(Double.parseDouble(xStr));
                        startVals.add(Double.parseDouble(startVal));
                    }
                }
            }
        }

        if(outputDirectoryNotNull()) {
            try {
                TDoubleDoubleHashMap corr = Correlations.mean(startVals.toArray(), distVals.toArray(), 3600);
                StatsWriter.writeHistogram(corr, "startTime", "distance", getOutputDirectory() + "/distStartTime.txt");

                StatsWriter.writeScatterPlot(startVals, distVals, "startTime", "distance", getOutputDirectory() + "/distStartTime.scatter.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
