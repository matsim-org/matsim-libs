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

package playground.johannes.studies.matrix2014.analysis;


import gnu.trove.map.TDoubleDoubleMap;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.sim.HistogramBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class HistogramComparator implements AnalyzerTask<Collection<? extends Person>>{

    private final String dimension;

    private final TDoubleDoubleMap refHist;

    private final HistogramBuilder builder;

    private FileIOContext ioContext;

    public HistogramComparator(TDoubleDoubleMap refHist, HistogramBuilder builder, String dimension) {
        this.refHist = refHist;
        this.builder = builder;
        this.dimension = dimension;

        Histogram.normalize((TDoubleDoubleHashMap) refHist);
    }

    public void setFileIoContext(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        TDoubleDoubleMap simHist = builder.build(persons);
        Histogram.normalize((TDoubleDoubleHashMap) simHist);

        TDoubleSet keySet = new TDoubleHashSet(refHist.keySet());
        keySet.addAll(simHist.keySet());

        TDoubleDoubleMap errHist = new TDoubleDoubleHashMap();
        double[] keyArray = keySet.toArray();
        for(double bin : keyArray) {
            double err = (simHist.get(bin) - refHist.get(bin))/refHist.get(bin);
            errHist.put(bin, err);
        }

        containers.add(new StatsContainer(String.format("%s.errHist", dimension), errHist.values()));

        if(ioContext != null) {
            try {
                StatsWriter.writeHistogram(
                        (TDoubleDoubleHashMap) errHist,
                        "bin",
                        "error",
                        String.format("%s/%s.errHist.txt", ioContext.getPath(), dimension));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
