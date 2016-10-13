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

import com.vividsolutions.jts.geom.Geometry;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class OSMObject {

    public static final String AREA = "area";

    public static final String BUILDING = "building";

    public static final String POI = "poi";

    private String id;

    private String objectType;

    private String facilityType;

    private Geometry geometry;

    private Set<String> activityOptions = new HashSet<String>();

    public OSMObject(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * @return the type
     */
    public String getObjectType() {
        return objectType;
    }

    /**
     * @param type the type to set
     */
    public void setObjectType(String type) {
        this.objectType = type;
    }

    /**
     * @return the type
     */
    public String getFacilityType() {
        return facilityType;
    }

    /**
     * @param type the type to set
     */
    public void setFacilityType(String type) {
        this.facilityType = type;
    }

    /**
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * @param geometry the geometry to set
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * @return the activityOptions
     */
    public Set<String> getActivityOptions() {
        return activityOptions;
    }

    public void addActivityOption(String option) {
        activityOptions.add(option);
    }
}
