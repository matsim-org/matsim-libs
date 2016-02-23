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
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.PersonsTask;
import playground.johannes.synpop.util.Executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class ValidatePopulationDensity implements PersonsTask {

    private final ZoneCollection zones;

    private final ActivityFacilities facilities;

    public ValidatePopulationDensity(DataPool dataPool) {
        zones = ((ZoneData)dataPool.get(ZoneDataLoader.KEY)).getLayer("modena");
        facilities = ((FacilityData)dataPool.get(FacilityDataLoader.KEY)).getAll();
    }

    @Override
    public void apply(Collection<? extends Person> persons) {
        int n = Executor.getFreePoolSize();
        n = Math.max(n, 1);
        List<? extends Person>[] segments = org.matsim.contrib.common.collections.CollectionUtils.split(persons, n);

        List<RunThread> threads = new ArrayList<>();
        for(List<? extends Person> segment : segments) {
            threads.add(new RunThread(zones, facilities, segment));
        }

        ProgressLogger.init(persons.size(), 2, 10);
        Executor.submitAndWait(threads);
        ProgressLogger.terminate();
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
            for(Person p : persons) {
                ActivityFacility f = getHomeLocation(p);
                if(f != null) {
                    Zone zone = zones.get(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
                    if(zone != null) {
                        double w = Double.parseDouble(p.getAttribute(CommonKeys.PERSON_WEIGHT));
                        counts.adjustOrPutValue(zone, w, w);
                    }
                }

                ProgressLogger.step();
            }
        }

        private ActivityFacility getHomeLocation(Person p) {
            for(Episode e : p.getEpisodes()) {
                for(Segment act : e.getActivities()) {
                    if(ActivityTypes.HOME.equals(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                        String idStr = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                        return  facilities.getFacilities().get(Id.create(idStr, ActivityFacility.class));
                    }
                }
            }

            return null;
        }
    }
}
