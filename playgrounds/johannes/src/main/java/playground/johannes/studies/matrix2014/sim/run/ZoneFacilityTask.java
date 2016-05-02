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

package playground.johannes.studies.matrix2014.sim.run;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author johannes
 */
public class ZoneFacilityTask implements AnalyzerTask<ZoneCollection> {

    private final ActivityFacilities facilities;

    private final FileIOContext ioContext;

    public ZoneFacilityTask(ActivityFacilities facilities, FileIOContext ioContext) {
        this.facilities = facilities;
        this.ioContext = ioContext;
    }

    @Override
    public void analyze(ZoneCollection zones, List<StatsContainer> containers) {
        new ZoneFacilityCount(facilities).apply(zones);
        new ZoneFacilityDensity().apply(zones);

        TDoubleArrayList rhoValues = new TDoubleArrayList();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/zoneStats.txt", ioContext.getPath())));

            writer.write("id\tfDensity\tfCount\tpop");
            writer.newLine();

            for (Zone zone : zones.getZones()) {
                String rhoVal = zone.getAttribute(ZoneFacilityDensity.FACILITY_DENSITY_KEY);
                if (rhoVal != null) {
                    rhoValues.add(Double.parseDouble(rhoVal));
                }

                writer.write(zone.getAttribute(zones.getPrimaryKey()));
                writer.write("\t");
                writer.write(rhoVal);
                writer.write("\t");
                writer.write(zone.getAttribute(ZoneFacilityCount.FACILITY_COUNT_KEY));
                writer.write("\t");
                writer.write(zone.getAttribute(ZoneData.POPULATION_KEY));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[] nativeValues = rhoValues.toArray();

        Discretizer discr20 = FixedSampleSizeDiscretizer.create(nativeValues, 1, 20);
        TDoubleDoubleHashMap hist20 = Histogram.createHistogram(nativeValues, discr20, true);

        Discretizer discr10 = FixedSampleSizeDiscretizer.create(nativeValues, 1, 10);
        TDoubleDoubleHashMap hist10 = Histogram.createHistogram(nativeValues, discr10, true);
        try {
            StatsWriter.writeHistogram(hist20, "density", "proba", String.format("%s/facilityDensity20.txt", ioContext
                    .getPath()));
            StatsWriter.writeHistogram(hist10, "density", "proba", String.format("%s/facilityDensity10.txt", ioContext
                    .getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
