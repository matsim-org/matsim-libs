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
import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.socialnetworks.statistics.Correlations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author johannes
 */
public class DetourData {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/sge/prj/synpop/run/962/output/detours.txt"));

        TDoubleArrayList xvals = new TDoubleArrayList();
        TDoubleArrayList yvals = new TDoubleArrayList();

        String line = reader.readLine();
        while((line = reader.readLine()) != null) {
            String[] tokens = line.split("\t");
            double x = Double.parseDouble(tokens[0]);
            double y = Double.parseDouble(tokens[1]);

            if(x < 1000000) {
                xvals.add(x);
                yvals.add(y / x);
            }
        }

        Discretizer d = FixedSampleSizeDiscretizer.create(xvals.toNativeArray(), 50, 100);
        TDoubleDoubleHashMap corel = Correlations.mean(xvals.toNativeArray(), yvals.toNativeArray(), 20000);
        StatsWriter.writeHistogram(corel, "route", "geo", "/home/johannes/sge/prj/synpop/run/962/output/detours.mean.txt");
    }
}
