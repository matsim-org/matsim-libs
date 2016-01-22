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

package playground.johannes.studies.matrix2014.sim;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.sim.ValueGenerator;
import playground.johannes.synpop.sim.data.*;
import playground.johannes.synpop.util.Executor;

import java.util.*;

/**
 * @author johannes
 */
public class ProximityFacilityGenerator implements ValueGenerator {

    private static final Logger logger = Logger.getLogger(ProximityFacilityGenerator.class);

    private final List<String> blacklist;

    private final FacilityData facilityData;

    private final Random random;

    private final double proximityProba;

    private final Object facilityDataKey;

    private static final Object PERSON_ZONE_DATA_KEY = new Object();

    private final static Object NO_HOME_ZONE_KEY = new Object();

    private final Map<Object, Map<String, List<ActivityFacility>>> zonedFacilities;

    private final ZoneCollection zones;

    private final Map<Zone, Object> zoneKeys;

    public ProximityFacilityGenerator(FacilityData facilityData, ZoneCollection zones, double proba, Random random) {
        this.facilityData = facilityData;
        this.zones = zones;
        this.proximityProba = proba;
        this.random = random;

        blacklist = new ArrayList<>();
        facilityDataKey = Converters.register(CommonKeys.ACTIVITY_FACILITY, new ActivityFacilityConverter(facilityData));

        zonedFacilities = new IdentityHashMap<>();
        zoneKeys = new IdentityHashMap<>();

        logger.debug("Assigning facilities to zones...");
        ProgressLogger.init(facilityData.getAll().getFacilities().size(), 2, 10);

        int numThreads = Executor.getFreePoolSize();
        List<? extends ActivityFacility>[] segments = CollectionUtils.split(facilityData.getAll().getFacilities().values(),
                numThreads);
        List<RunThread> threads = new ArrayList<>();
        for(List<? extends ActivityFacility> segment : segments) {
            threads.add(new RunThread(segment));
        }
        Executor.submitAndWait(threads);

        ProgressLogger.terminate();

    }

    public void addToBlacklist(String type) {
        blacklist.add(type);
    }


    @Override
    public Object newValue(CachedElement element) {
        CachedSegment act = (CachedSegment) element;
        /*
        Won't work if activity types change.
         */
        String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
        boolean ignore = false;
        if (type != null) {
            if (blacklist.contains(type)) ignore = true;
        }

        if (!ignore) {
            ActivityFacility newFacility = null;

            if(proximityProba >= random.nextDouble()) {
                /*
                Get the person's home zone. Some person do not have a home zone.
                 */
                Object zoneKey = getZoneKey((CachedPerson)act.getEpisode().getPerson());
                if(zoneKey != NO_HOME_ZONE_KEY) {
                    /*
                    Get the type-facility map and draw a random facility.
                     */
                    Map<String, List<ActivityFacility>> typedFacilities = zonedFacilities.get(zoneKey);
                    if (typedFacilities != null) {
                        List<ActivityFacility> facilityList = typedFacilities.get(type);
                        if (!facilityList.isEmpty()) {
                            newFacility = facilityList.get(random.nextInt(facilityList.size()));
                        }
                    }
                }
            }

            if(newFacility == null) {
                return facilityData.randomFacility(type);
            } else {
                return  newFacility;
            }
        } else {
            return null;
        }
    }

    private Object getZoneKey(CachedPerson person) {
        Object zoneKey = person.getData(PERSON_ZONE_DATA_KEY);
        if(zoneKey == null) {
            /*
            Get the person's home facility. If the person has no home facility, mark it to avoid repeated queries
            for the home facility.
             */
            ActivityFacility facility = getHomeFacility(person);
            if(facility != null) {
                Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
                Object key = zoneKeys.get(zone);
                person.setData(PERSON_ZONE_DATA_KEY, key);
                return key;
            } else {
                person.setData(PERSON_ZONE_DATA_KEY, NO_HOME_ZONE_KEY);
                return NO_HOME_ZONE_KEY;
            }
        }
        return zoneKey;
    }

    private ActivityFacility getHomeFacility(CachedPerson person) {
        for(Episode e : person.getEpisodes()) {
            for(Segment act : e.getActivities()) {
                if(ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    return (ActivityFacility) ((CachedSegment)act).getData(facilityDataKey);
                }
            }
        }

        return null;
    }

    private synchronized Object getZoneKey(Zone zone) {
        Object zoneKey = zoneKeys.get(zone);

        if(zoneKey == null) {
            zoneKey = new Object();
            zoneKeys.put(zone, zoneKey);
        }

        return zoneKey;
    }

    private synchronized Map<String, List<ActivityFacility>> getTypedFacilities(Object zoneKey) {
        Map<String, List<ActivityFacility>> typedFacilities = zonedFacilities.get(zoneKey);

        if(typedFacilities == null) {
            typedFacilities = new HashMap<>();
            zonedFacilities.put(zoneKey, typedFacilities);
        }

        return typedFacilities;
    }

    private synchronized void addToFacilityList(Map<String, List<ActivityFacility>> typedFacilities,
                                                                  ActivityFacility facility, String type) {
        List<ActivityFacility> facilityList = typedFacilities.get(type);

        if(facilityList == null) {
            facilityList = new ArrayList<>();
            typedFacilities.put(type, facilityList);
        }

        facilityList.add(facility);
    }

    private class RunThread implements Runnable {

        private final List<? extends ActivityFacility> facilities;

        public RunThread(List<? extends ActivityFacility> facilities) {
            this.facilities = facilities;
        }

        @Override
        public void run() {
            for(ActivityFacility facility : facilities) {
                Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
                if(zone != null) {
                    Object zoneKey = getZoneKey(zone);
                    Map<String, List<ActivityFacility>> typedFacilities = getTypedFacilities(zoneKey);

                    for(ActivityOption opt : facility.getActivityOptions().values()) {
                        addToFacilityList(typedFacilities, facility, opt.getType());
                    }
                }
                ProgressLogger.step();
            }
        }
    }
}
