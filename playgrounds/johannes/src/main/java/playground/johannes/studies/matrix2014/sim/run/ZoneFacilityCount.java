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

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.util.Executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class ZoneFacilityCount  {

    public static final String FACILITY_COUNT_KEY = "facility_count";

    private final ActivityFacilities facilities;

    public ZoneFacilityCount(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    public void apply(ZoneCollection zones) {
        int n = Executor.getFreePoolSize();
        n = Math.max(1, n);

        List<? extends ActivityFacility>[] segments = CollectionUtils.split(facilities.getFacilities().values(), n);

        List<RunThread> threads = new ArrayList<>();
        for(List<? extends ActivityFacility> segment : segments) {
            threads.add(new RunThread(segment, zones));
        }

        Executor.submitAndWait(threads);

        for(Zone zone : zones.getZones()) {
            int count = 0;
            for (RunThread thread : threads) {
                count +=  thread.getCounts().get(zone);
            }

            zone.setAttribute(FACILITY_COUNT_KEY, String.valueOf(count));
        }
    }

    private static class RunThread implements Runnable {

        private final Collection<? extends ActivityFacility> facilities;

        private final ZoneCollection zones;

        private final TObjectIntMap<Zone> counts = new TObjectIntHashMap<>();

        public RunThread(Collection<? extends ActivityFacility> facilities, ZoneCollection zones) {
            this.facilities = facilities;
            this.zones = zones;
        }

        public TObjectIntMap<Zone> getCounts() {
            return counts;
        }

        @Override
        public void run() {
            for(ActivityFacility facility : facilities) {
                Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
                if(zone != null) {
                    counts.adjustOrPutValue(zone, 1, 1);
                }
            }
        }
    }
}
