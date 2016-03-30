/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 */
public class FacilityData {

    private static final Logger logger = Logger.getLogger(FacilityData.class);

    private Map<String, QuadTree<ActivityFacility>> quadTrees = new HashMap<>();

    private Map<String, List<ActivityFacility>> facilitiesMap;

    private final ActivityFacilities facilities;

    private final Random random;

    public FacilityData(ActivityFacilities facilities, Random random) {
        this.random = random;
        this.facilities = facilities;
    }

    public ActivityFacilities getAll() {
        return facilities;
    }

    public ActivityFacility randomFacility(String type) {
        initMap();
        List<ActivityFacility> list = facilitiesMap.get(type);
        if (list != null) {
            return list.get(random.nextInt(list.size()));
        } else {
            return null;
        }
    }

    public List<ActivityFacility> getFacilities(String type) {
        initMap();
        return facilitiesMap.get(type);
    }

    public ActivityFacility getClosest(Coord coord, String type) {
        QuadTree<ActivityFacility> quadtree = quadTrees.get(type);
        if (quadtree == null) {
            initQuadTree(type);
        }

        QuadTree<ActivityFacility> quadTree = quadTrees.get(type);
        return quadTree.getClosest(coord.getX(), coord.getY());
    }

    public QuadTree<ActivityFacility> getQuadTree(String type) {
        QuadTree<ActivityFacility> quadtree = quadTrees.get(type);
        if (quadtree == null) {
            initQuadTree(type);
        }

        return quadTrees.get(type);
    }

//    public String getAttribute(ActivityFacility facility, String key) {
//        return null;
//    }

    private synchronized void initMap() {
        if (facilitiesMap == null) {
            facilitiesMap = new HashMap<>();

            for (ActivityFacility facility : facilities.getFacilities().values()) {
                for (ActivityOption option : facility.getActivityOptions().values()) {
                    List<ActivityFacility> list = facilitiesMap.get(option.getType());

                    if (list == null) {
                        list = new LinkedList<>();
                        facilitiesMap.put(option.getType(), list);
                    }

                    list.add(facility);
                }
            }

            for (Entry<String, List<ActivityFacility>> entry : facilitiesMap.entrySet()) {
                entry.setValue(new ArrayList<>(entry.getValue()));
            }
        }
    }

    private synchronized void initQuadTree(String type) {
        QuadTree<ActivityFacility> quadtree = quadTrees.get(type);
        if (quadtree == null) {
            logger.debug(String.format("Initializing quad tree for facilities of type %s.", type));

            List<ActivityFacility> facilities = getFacilities(type);
            double minx = Double.MAX_VALUE;
            double miny = Double.MAX_VALUE;
            double maxx = 0;
            double maxy = 0;

            for (ActivityFacility fac : facilities) {
                minx = Math.min(minx, fac.getCoord().getX());
                miny = Math.min(miny, fac.getCoord().getY());
                maxx = Math.max(maxx, fac.getCoord().getX());
                maxy = Math.max(maxy, fac.getCoord().getY());
            }

            quadtree = new QuadTree<>(minx, miny, maxx, maxy);

            for (ActivityFacility fac : facilities) {
                quadtree.put(fac.getCoord().getX(), fac.getCoord().getY(), fac);
            }

            quadTrees.put(type, quadtree);

            logger.debug("Done.");
        }
    }
}
