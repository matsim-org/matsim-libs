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

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.AnalyzerTask;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.util.Executor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class ValidatePopulationDensity implements AnalyzerTask<Collection<? extends Person>> {

    private final static Logger logger = Logger.getLogger(ValidatePopulationDensity.class);

    private final ZoneCollection zones;

    private final ActivityFacilities facilities;

    private FileIOContext ioContext;

    public final TObjectDoubleMap<Zone> zoneWeights;

    public ValidatePopulationDensity(DataPool dataPool, TObjectDoubleMap<Zone> zoneWeights, String layerName) {
        zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer(layerName);
        facilities = ((FacilityData) dataPool.get(FacilityDataLoader.KEY)).getAll();
        this.zoneWeights = zoneWeights;
    }

    public void setIoContext(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        int n = Executor.getFreePoolSize();
        n = Math.max(n, 1);
        List<? extends Person>[] segments = org.matsim.contrib.common.collections.CollectionUtils.split(persons, n);

        List<RunThread> threads = new ArrayList<>();
        for (List<? extends Person> segment : segments) {
            threads.add(new RunThread(zones, facilities, segment));
        }

        ProgressLogger.init(persons.size(), 2, 10);
        Executor.submitAndWait(threads);
        ProgressLogger.terminate();

        TObjectDoubleMap<Zone> simCounts = new TObjectDoubleHashMap<>();
        double simSum = 0;
        for (RunThread thread : threads) {
            TObjectDoubleIterator<Zone> it = thread.getCounts().iterator();
            for (int i = 0; i < thread.getCounts().size(); i++) {
                it.advance();
                simCounts.adjustOrPutValue(it.key(), it.value(), it.value());
                simSum += it.value();
            }
        }

        TObjectDoubleMap<Zone> refCounts = new TObjectDoubleHashMap<>();
        double refSum = 0;
        for (Zone zone : zones.getZones()) {
            double inhabs = 0;
            String val = zone.getAttribute(ZoneData.POPULATION_KEY);
            double w = zoneWeights.get(zone);
            if(val != null) {
                try {
                    inhabs = Double.parseDouble(val) * w;
                } catch (NumberFormatException e) {
                    logger.debug(e.getLocalizedMessage());
                }
            }

            refCounts.adjustOrPutValue(zone, inhabs, inhabs);
            refSum += inhabs;
        }

        double scale = refSum / simSum;

        TObjectDoubleMap<Zone> errors = new TObjectDoubleHashMap<>();
        for (Zone zone : zones.getZones()) {
            double simVal = simCounts.get(zone) * scale;
            double refVal = refCounts.get(zone);

            errors.put(zone, (simVal - refVal) / refVal);
        }

        containers.add(new StatsContainer("popdensity", errors.values()));

        if (ioContext != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/popdensity.txt", ioContext.getPath())));
                writer.write("id\tref\tsim\terr");
                writer.newLine();

                TObjectDoubleIterator<Zone> it = errors.iterator();
                for (int i = 0; i < errors.size(); i++) {
                    it.advance();
                    writer.write(it.key().getAttribute(zones.getPrimaryKey()));
                    writer.write("\t");
                    writer.write(String.valueOf(refCounts.get(it.key())));
                    writer.write("\t");
                    writer.write(String.valueOf(simCounts.get(it.key()) * scale));
                    writer.write("\t");
                    writer.write(String.valueOf(it.value()));
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static class RunThread implements Runnable {

        private final ZoneCollection zones;

        private final ActivityFacilities facilities;

        private final Collection<? extends Person> persons;

        private final TObjectDoubleMap<Zone> counts;

        public RunThread(ZoneCollection zones, ActivityFacilities facilities, Collection<? extends Person> persons) {
            this.zones = zones;
            this.facilities = facilities;
            this.persons = persons;
            counts = new TObjectDoubleHashMap<>();
        }

        public TObjectDoubleMap<Zone> getCounts() {
            return counts;
        }

        @Override
        public void run() {
            for (Person p : persons) {
                ActivityFacility f = getHomeLocation(p);
                if (f != null) {
                    Zone zone = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
                    if (zone != null) {
                        double w = Double.parseDouble(p.getAttribute(CommonKeys.PERSON_WEIGHT));
                        counts.adjustOrPutValue(zone, w, w);
                    }
                }

                ProgressLogger.step();
            }
        }

        private ActivityFacility getHomeLocation(Person p) {
            for (Episode e : p.getEpisodes()) {
                for (Segment act : e.getActivities()) {
                    if (ActivityTypes.HOME.equals(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                        String idStr = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                        return facilities.getFacilities().get(Id.create(idStr, ActivityFacility.class));
                    }
                }
            }

            return null;
        }
    }
}
