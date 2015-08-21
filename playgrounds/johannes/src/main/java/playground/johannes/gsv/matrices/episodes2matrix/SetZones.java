/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.episodes2matrix;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.synpop.source.mid2008.processing.EpisodeTask;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class SetZones implements EpisodeTask {

    public static final String ZONE_KEY = "zone";

    private ZoneCollection zones;

    private ActivityFacilities facilities;

    private MathTransform transform;

    private String idKey;

    private int notfound;

    public SetZones(ZoneCollection zones, ActivityFacilities facilities, String idKey, MathTransform transform) {
        this.zones = zones;
        this.facilities = facilities;
        this.idKey = idKey;
        this.transform = transform;
    }

    @Override
    public void apply(Episode plan) {
        for(Segment act : plan.getActivities()) {
            String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
            ActivityFacility facility = facilities.getFacilities().get(Id.create(id, ActivityFacility.class));

            Coordinate c = new Coordinate(facility.getCoord().getX(), facility.getCoord().getY());
            CRSUtils.transformCoordinate(c, transform);

            Zone zone = zones.get(c);

            if(zone != null) {
                act.setAttribute(ZONE_KEY, zone.getAttribute(idKey));
            } else {
                notfound++;
            }
        }
    }

    public int getNotFound() {
        return notfound;
    }
}
