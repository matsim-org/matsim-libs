/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.data;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.*;

/**
 * @author jillenberger
 */
public class PersonAttributeUtils {

    public static ActivityFacility getHomeFacility(Person p, ActivityFacilities facilities) {
        for(Episode e : p.getEpisodes()) {
            for(Segment act : e.getActivities()) {
                if(ActivityTypes.HOME.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
                    String attr = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
                    if(attr != null) {
                        Id<ActivityFacility> id = Id.create(attr, ActivityFacility.class);
                        return facilities.getFacilities().get(id);
                    }
                }
            }
        }

        return null;
    }
}
