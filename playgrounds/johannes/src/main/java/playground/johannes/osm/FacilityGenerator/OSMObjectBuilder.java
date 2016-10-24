package playground.johannes.osm.FacilityGenerator;/* *********************************************************************** *
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

import com.vividsolutions.jts.geom.*;
import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import playground.johannes.gsv.synPop.osm.OSMNode;
import playground.johannes.gsv.synPop.osm.OSMWay;

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class OSMObjectBuilder {

    private static final Logger logger = Logger.getLogger(OSMObjectBuilder.class);

    private GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);

    private Map<String, Integer> typeCounter = new HashMap<>();

//    private Connection connection;

    public OSMObjectBuilder() {
    }


    public OSMObject build(OSMWay way) {
        OSMObject obj = new OSMObject(way.getId());

        if (way.tags().keySet().contains("landuse")) {
            obj.setObjectType(OSMObject.AREA);
        } else {
            obj.setObjectType(OSMObject.BUILDING);

        }

        String type = getTypeFromTags(way.tags());

        obj.setFacilityType(type);

        Geometry geo = buildGeometry(way);
        if (geo == null)
            return null;

        obj.setGeometry(geo);

        if (type == null || (type.equals("unclassified") && obj.getObjectType().equals(OSMObject.AREA))) {
            return null;
        }

        return obj;
    }

    public OSMObject build(OSMNode node) {
        if (!node.isNodeOfWay()) {
            OSMObject obj = new OSMObject(node.getId());
            obj.setObjectType(OSMObject.POI);

            String type = getTypeFromTags(node.tags());
            if (type == null) {
                return null;
            }
            obj.setFacilityType(type);

            Point p = factory.createPoint(new Coordinate(node.getLongitude(),
                    node.getLatitude()));
            obj.setGeometry(p);

            return obj;
        } else {
            return null;
        }
    }

    private String getTypeFromTags(Map<String, String> tags) {

        if (tags == null) {
            return null;
        }

        boolean building = false;
        String type = null;
        for (Map.Entry<String, String> tag : tags.entrySet()) {

            String fullTag = tag.getKey() + "_" + tag.getValue();
            if (fullTag.equals("building_yes")) {
                building = true;
            }

            if (Mapping.tag2Type.containsKey(fullTag)) {
                String tmp = Mapping.tag2Type.get(fullTag);
                if (type != null && !type.contains(tmp)) {
                    type = type + ";" + tmp;
                } else {
                    type = tmp;
                }
            }
        }
        if (type == null) {
            if (building) {
                type = "unclassified";
            } else {
                return null;
            }
        }

        int i = 1;
        if (typeCounter.containsKey(type)) {
            i += typeCounter.get(type);
        }
        typeCounter.put(type, i);
        return type;
    }

    private Geometry buildGeometry(OSMWay way) {
        Coordinate coords[] = new Coordinate[way.getNodes().size()];


        for (int i = 0; i < way.getNodes().size(); i++) {
            OSMNode node = way.getNodes().get(i);
            coords[i] = new Coordinate(node.getLongitude(), node.getLatitude());
        }

        LinearRing ring;
        try {
            ring = factory.createLinearRing(coords);
        } catch (IllegalArgumentException e) {
            logger.trace("Failed to create polygon. Coords = " + coords.length);
            return null;
        }

        Polygon poly = factory.createPolygon(ring, null);

        if (poly.isEmpty()) {
            logger.trace("Empty polygon. Coords = " + coords.length);
            return null;
        }
        return poly;
    }

    public Map<String, Integer> getTypeCounter() {
        return this.typeCounter;
    }
}
