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

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.util.Executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 */
public class MatrixBuilder {

    private static final Logger logger = Logger.getLogger(MatrixBuilder.class);

    private final FacilityData fData;

    private final ZoneCollection zones;

    private final Map<String, String> zoneIds;

    public MatrixBuilder(FacilityData fData, ZoneCollection zones) {
        this.fData = fData;
        this.zones = zones;
        zoneIds = new ConcurrentHashMap<>();
    }

    public KeyMatrix build(Collection<? extends Person> persons, Predicate<Segment> predicate) {
        int n = persons.size() / 10000;
        n = Math.min(n, Executor.getFreePoolSize());
        n = Math.max(2, n);
        List<? extends Person>[] segments = org.matsim.contrib.common.collections.CollectionUtils.split(persons, n);

        List<RunThread> runnables = new ArrayList<>(n);
        for(List<? extends Person> segment : segments) {
            runnables.add(new RunThread(segment, predicate));
        }

        Executor.submitAndWait(runnables);

        KeyMatrix m = new KeyMatrix();
        int errors = 0;
        for(RunThread runnable : runnables) {
            KeyMatrix m_dash = runnable.getMatrix();
            MatrixOperations.add(m, m_dash);
            errors += runnable.getErrors();
        }

        if(errors > 0) {
            logger.warn(String.format("%s facilities cannot be located in a zone.", errors));
        }
        return m;
    }

    private String getZoneId(String facilityId) {
        String zoneId = zoneIds.get(facilityId);

        if(zoneId == null) {
            Id<ActivityFacility> facilityObjId = Id.create(facilityId, ActivityFacility.class);
            ActivityFacility facility = fData.getAll().getFacilities().get(facilityObjId);
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

        private final KeyMatrix m;

        private int errors;

        public RunThread(Collection<? extends Person> persons, Predicate<Segment> predicate) {
            this.persons = persons;
            this.predicate = predicate;
            m = new KeyMatrix();
        }

        public KeyMatrix getMatrix() {
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
                                m.add(origin, dest, 1);
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
