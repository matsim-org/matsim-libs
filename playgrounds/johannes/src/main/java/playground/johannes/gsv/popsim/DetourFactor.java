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

import gnu.trove.TDoubleArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.gsv.synPop.analysis.AnalyzerTask;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class DetourFactor extends AnalyzerTask {

    private final ActivityFacilities facilities;

    public DetourFactor(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
        List<Double> routeDistances = new LegDoubleCollector(CommonKeys.LEG_ROUTE_DISTANCE).collect(persons);
        List<Double> beelineDistances = new LegBeelineDistance(facilities).collect(persons);

        if(routeDistances.size() != beelineDistances.size()) {

        } else {
            TDoubleArrayList xvalues = new TDoubleArrayList(routeDistances.size());
            TDoubleArrayList yvalues = new TDoubleArrayList(routeDistances.size());

            for(int i = 0; i < routeDistances.size(); i++) {
                if(routeDistances.get(i) != null && beelineDistances.get(i) != null) {
                    xvalues.add(routeDistances.get(i));
                    yvalues.add(beelineDistances.get(i));
                }
            }

            try {
                String filename = getOutputDirectory() + "/detours.txt";
                TXTWriter.writeScatterPlot(xvalues, yvalues, "d.route", "d.geo", filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
