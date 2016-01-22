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

package playground.johannes.studies.matrix2014.matrix;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.util.Executor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 */
public class MatrixBuilder {

    private static final Logger logger = Logger.getLogger(MatrixBuilder.class);

    private final ActivityFacilities facilities;

    private final ZoneCollection zones;

    private final Map<String, String> zoneIds;

    public MatrixBuilder(ActivityFacilities facilities, ZoneCollection zones) {
        this.facilities = facilities;
        this.zones = zones;
        zoneIds = new ConcurrentHashMap<>();
    }

    public NumericMatrix build(Collection<? extends Person> persons, Predicate<Segment> predicate, boolean useWeights) {
//        int n = 1;
        int n = persons.size() / 10000;
        n = Math.min(n, Executor.getFreePoolSize());
        n = Math.max(2, n);
        List<? extends Person>[] segments = org.matsim.contrib.common.collections.CollectionUtils.split(persons, n);

        List<RunThread> runnables = new ArrayList<>(n);
        for(List<? extends Person> segment : segments) {
            runnables.add(new RunThread(segment, predicate, useWeights));
        }

        Executor.submitAndWait(runnables);

        int errors = 0;
        Set<NumericMatrix> matrices = new HashSet<>();
        for(RunThread runnable : runnables) {
            matrices.add(runnable.getMatrix());
            errors += runnable.getErrors();
        }
        NumericMatrix m = new NumericMatrix();
        MatrixOperations.accumulate(matrices, m);

        if(errors > 0) {
            logger.warn(String.format("%s facilities cannot be located in a zone.", errors));
        }
        return m;
    }

    private String getZoneId(String facilityId) {
        String zoneId = zoneIds.get(facilityId);

        if(zoneId == null) {
            Id<ActivityFacility> facilityObjId = Id.create(facilityId, ActivityFacility.class);
            ActivityFacility facility = facilities.getFacilities().get(facilityObjId);
            Coordinate c = new Coordinate(facility.getCoord().getX(), facility.getCoord().getY());

            Zone zone = zones.get(c);
            if(zone != null) {
                zoneId = zone.getAttribute(zones.getPrimaryKey());
                zoneIds.put(facilityId, zoneId);
            }
        }

        return zoneId;
    }

    public class RunThread implements Runnable {

        private final Collection<? extends Person> persons;

        private final Predicate<Segment> predicate;

        private final NumericMatrix m;

        private final boolean useWeights;

        private int errors;

        public RunThread(Collection<? extends Person> persons, Predicate<Segment> predicate, boolean useWeights) {
            this.persons = persons;
            this.predicate = predicate;
            this.useWeights = useWeights;

            m = new NumericMatrix();
        }

        public NumericMatrix getMatrix() {
            return m;
        }

        public int getErrors() {
            return  errors;
        }

        @Override
        public void run() {
            for(Person person : persons) {
                for (Episode episode : person.getEpisodes()) {
                    for (int i = 0; i < episode.getLegs().size(); i++) {
                        Segment leg = episode.getLegs().get(i);
                        if (predicate == null || predicate.test(leg)) {
                            Segment prev = episode.getActivities().get(i);
                            Segment next = episode.getActivities().get(i + 1);

                            String originFacId = prev.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                            String origin = getZoneId(originFacId);

                            String destFacId = next.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                            String dest = getZoneId(destFacId);

                            if (origin != null && dest != null) {
                                double w = 1.0;
                                if(useWeights) w = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));
                                m.add(origin, dest, w);
                            } else {
                                errors++;
                            }
                        }
                    }
                }
            }
        }
    }
}
