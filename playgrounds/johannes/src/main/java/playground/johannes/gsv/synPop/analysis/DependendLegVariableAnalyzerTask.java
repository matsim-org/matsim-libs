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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 */
public class DependendLegVariableAnalyzerTask extends AnalyzerTask {

    private final String xKey;

    private final String yKey;

    public DependendLegVariableAnalyzerTask(String xKey, String yKey) {
        this.xKey = xKey;
        this.yKey = yKey;
    }

    @Override
    public void analyze(Collection<ProxyPerson> persons, Map<String, DescriptiveStatistics> results) {
        TDoubleArrayList xVals = new TDoubleArrayList();
        TDoubleArrayList yVals = new TDoubleArrayList();

        for(ProxyPerson person : persons) {
            for(ProxyPlan plan : person.getPlans()) {
                for(Element leg : plan.getLegs()) {
                    String xStr = leg.getAttribute(xKey);
                    String yStr = leg.getAttribute(yKey);

                    if(xStr != null && yStr != null) {
                        xVals.add(Double.parseDouble(xStr));
                        yVals.add(Double.parseDouble(yStr));
                    }
                }
            }
        }

        if(!xVals.isEmpty()) {
            if (outputDirectoryNotNull()) {
                try {
                    String filename = String.format("%s/%s.%s.mean.txt", getOutputDirectory(), xKey, yKey);
                    double[] x = xVals.toNativeArray();
                    double[] y = yVals.toNativeArray();
                    Discretizer disc = FixedSampleSizeDiscretizer.create(x, 50, 100);
                    TDoubleDoubleHashMap corr = Correlations.mean(x, y, disc);
                    TXTWriter.writeMap(corr, xKey, yKey, filename);

                    filename = String.format("%s/%s.%s.scatter.txt", getOutputDirectory(), xKey, yKey);
                    TXTWriter.writeScatterPlot(xVals, yVals, xKey, yKey, filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
