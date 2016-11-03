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

package playground.johannes.synpop.gis;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import playground.johannes.synpop.util.Executor;

import java.util.*;

/**
 * @author johannes
 */
public class FacilityUtils {

    public static Map<Zone, List<ActivityFacility>> mapFacilities2Zones(ZoneCollection zoneCollection, ActivityFacilities facilities) {
        int nThreads = Executor.getFreePoolSize();
        /*
        Split activities into separate lists.
         */
        List<? extends ActivityFacility>[] segments = CollectionUtils.split(facilities.getFacilities().values(), nThreads);
        /*
        Initialize threads.
         */
        List<ThreadMapFacilities> threads = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            threads.add(new ThreadMapFacilities(segments[i], zoneCollection));
        }
        /*
        Run threads.
         */
        ProgressLogger.init(facilities.getFacilities().size(), 2, 10);
        Executor.submitAndWait(threads);
        ProgressLogger.terminate();
        /*
        Merge results.
         */
        Set<Zone> zones = zoneCollection.getZones();
        Map<Zone, List<ActivityFacility>> map = new HashMap<>();
        for (Zone zone : zones) {
            List<ActivityFacility> mergeList = new ArrayList<>();
            for (int i = 0; i < nThreads; i++) {
                List<ActivityFacility> list = threads.get(i).getMap().get(zone);
                if (list != null) mergeList.addAll(list);
            }
            if(!mergeList.isEmpty()) map.put(zone, mergeList);
        }

        return map;
    }

    private static class ThreadMapFacilities implements Runnable {

        private List<? extends ActivityFacility> facilities;

        private ZoneCollection zoneCollection;

        private Map<Zone, List<ActivityFacility>> map;

        public ThreadMapFacilities(List<? extends ActivityFacility> facilities, ZoneCollection zoneCollection) {
            this.facilities = facilities;
            this.zoneCollection = zoneCollection;
        }

        public Map<Zone, List<ActivityFacility>> getMap() {
            return map;
        }

        @Override
        public void run() {
            map = new HashMap<>();

            for (ActivityFacility f : facilities) {
                Coordinate c = new Coordinate(f.getCoord().getX(), f.getCoord().getY());
                Zone zone = zoneCollection.get(c);
                if (zone != null) {
                    List<ActivityFacility> list = map.get(zone);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(zone, list);
                    }
                    list.add(f);
                }

                ProgressLogger.step();
            }
        }
    }

    public static void connect2Network(ActivityFacilitiesImpl facilities, Network network) {
        int nThreads = Executor.getFreePoolSize();
        List<? extends ActivityFacility>[] segments = CollectionUtils.split(facilities.getFacilities().values(), nThreads);
        List<ThreadConnectFacilities> threads = new ArrayList<>(nThreads);
        for(int i = 0; i < nThreads; i++) {
            threads.add(new ThreadConnectFacilities(segments[i], network));
        }

        Executor.submitAndWait(threads);
    }

    private static class ThreadConnectFacilities implements Runnable {

        private List<? extends ActivityFacility> facilities;

        private Network network;

        public ThreadConnectFacilities(List<? extends ActivityFacility> facilities, Network network) {
            this.facilities = facilities;
            this.network = network;
        }

        @Override
        public void run() {
            for(ActivityFacility facility : facilities) {
                Link link = NetworkUtils.getNearestLinkExactly(network, facility.getCoord());
                ((ActivityFacilityImpl)facility).setLinkId(link.getId());
            }
        }
    }
}
