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
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.studies.matrix2014.data.PersonAttributeUtils;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.sim.ValueGenerator;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.CachedSegment;

import java.util.*;

/**
 * @author johannes
 */
public class SegmentedFacilityGenerator implements ValueGenerator {

    private final Object FACILITY_SEGMENTS_KEY = new Object();

    private final ActivityFacilities facilities;

    private final FacilityData facilityData;

    private final ZoneCollection zones;

    private final Map<String, Map<Zone, List<ActivityFacility>[]>> typeMap;

    private final List<ActivityFacility>[] allFacilities;

    private final double localProba = 0.95;

    private final double threshold = 50000;

    private final Random random;

    private final List<String> blacklist;

    public SegmentedFacilityGenerator(DataPool dataPool, String layer, Random random) {
        facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        facilities = facilityData.getAll();
        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
        zones = zoneData.getLayer(layer);
        this.random = random;
        typeMap = new HashMap<>();

        ArrayList<ActivityFacility> list = new ArrayList(facilities.getFacilities().values());
        allFacilities = new List[]{list, list};

        blacklist = new ArrayList<>();
    }

    public void addToBlacklist(String type) {
        blacklist.add(type);
    }

    private Map<Zone, List<ActivityFacility>[]> buildSegments(String type) {
        QuadTree<ActivityFacility> spatialIndex = facilityData.getQuadTree(type);
        Map<Zone, List<ActivityFacility>[]> map = new HashMap<>();

        for(Zone zone : zones.getZones()) {
            double x = zone.getGeometry().getCentroid().getX();
            double y = zone.getGeometry().getCentroid().getY();
            List<ActivityFacility> local = new ArrayList<>(spatialIndex.getDisk(x, y, threshold));
            List<ActivityFacility> global = new ArrayList<>(facilities.getFacilities().values());
            global.removeAll(local);
            map.put(zone, new List[]{local, global});
        }

        return map;
    }

    @Override
    public Object newValue(CachedElement act) {
        CachedPerson person = (CachedPerson) ((CachedSegment) act).getEpisode().getPerson();

        String type = person.getAttribute(CommonKeys.ACTIVITY_TYPE);
        boolean ignore = true;
        if (type != null) {
            ignore = blacklist.contains(type);
        }

        if (!ignore) {
            List<ActivityFacility>[] segments = (List<ActivityFacility>[]) person.getData(FACILITY_SEGMENTS_KEY);
            if (segments == null) segments = initSegments(person);

            List<ActivityFacility> facilityList;
            if (random.nextDouble() > localProba) facilityList = segments[1];
            else facilityList = segments[0];

            ActivityFacility facility = facilityList.get(random.nextInt(facilityList.size()));

            return facility;

        } else {
            return null;
        }
    }

    private List<ActivityFacility>[] initSegments(CachedElement act) {
        CachedPerson person = (CachedPerson) ((CachedSegment)act).getEpisode().getPerson();
        ActivityFacility home = PersonAttributeUtils.getHomeFacility(person, facilities);

        if(home == null) {
            return allFacilities;
        } else {
            String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);

            Map<Zone, List<ActivityFacility>[]> zoneMap = typeMap.get(type);
            if (zoneMap == null) {
                zoneMap = buildSegments(type);
                typeMap.put(type, zoneMap);
            }


            Zone zone = zones.get(new Coordinate(home.getCoord().getX(), home.getCoord().getY()));

            return zoneMap.get(zone);
        }
    }
}
