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

package playground.johannes.studies.matrix2014.gis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class ActivityLocationLayer {

    public static final String ACTIVITY_TYPE = "activity_type";

    private final Map<String, Feature> locations;

    public ActivityLocationLayer(ActivityFacilities facilities) {
        locations = new LinkedHashMap<>();
        GeometryFactory factory = new GeometryFactory();
        for(ActivityFacility f : facilities.getFacilities().values()) {
            Geometry point = factory.createPoint(new Coordinate(f.getCoord().getX(), f.getCoord().getY()));
            Feature location = new Feature(f.getId().toString(), point);
            locations.put(location.getId(), location);
        }
    }

    public Feature get(String id) {
        return locations.get(id);
    }


}
