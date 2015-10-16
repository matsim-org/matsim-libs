/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.sim.data;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.gis.FacilityData;

/**
 * @author jillenberger
 */
public class ActivityFacilityConverter implements Converter {

    private static ActivityFacilityConverter instance;

    public static ActivityFacilityConverter getInstance(FacilityData data) {
        if(instance == null) {
            instance = new ActivityFacilityConverter(data);
        }

        return instance;
    }

    private final FacilityData data;

    public ActivityFacilityConverter(FacilityData data) {
        this.data = data;
    }

    @Override
    public Object toObject(String value) {
        return data.getAll().getFacilities().get(Id.create(value, ActivityFacility.class));
    }

    @Override
    public String toString(Object value) {
        return ((ActivityFacility)value).getId().toString();
    }
}
