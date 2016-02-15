/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.berlin.demand;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.*;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.demand.DefaultActivityCreator;


public class BerlinTaxiActivityCreator
    extends DefaultActivityCreator
{
    public BerlinTaxiActivityCreator(Scenario scenario)
    {
        super(scenario);
    }


    @Override
    public Activity createActivity(Zone zone, String actType)
    {
        Link link;
        if (zone.getId().equals(BerlinZoneUtils.TXL_LOR_ID)) {
            if (actType.equals("arrival")) {
                link = network.getLinks().get(BerlinZoneUtils.TO_TXL_LINK_ID);
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;
            }
            else {
                link = network.getLinks().get(BerlinZoneUtils.FROM_TXL_LINK_ID);
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;

            }
        }
        else if (zone.getId().equals(BerlinZoneUtils.SXF_LOR_ID)) {
            if (actType.equals("arrival")) {
                link = network.getLinks().get(BerlinZoneUtils.TO_SXF_LINK_ID);
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;
            }
            else {
                link = network.getLinks().get(BerlinZoneUtils.FROM_SXF_LINK_ID);
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;

            }
        }
        else {
            return createActivityWithCoordinateTransformation(zone, actType);
        }
    }
    
    public Activity createActivityWithCoordinateTransformation(Zone zone, String actType)
    {
        Geometry geometry = geometryProvider.getGeometry(zone, actType);
        Envelope envelope = geometry.getEnvelopeInternal();
        double minX = envelope.getMinX();
        double maxX = envelope.getMaxX();
        double minY = envelope.getMinY();
        double maxY = envelope.getMaxY();

        Point p = null;

        do {
            double x = uniform.nextDouble(minX, maxX);
            double y = uniform.nextDouble(minY, maxY);
            p = MGC.xy2Point(x, y);
        }
        while (!geometry.contains(p) || !pointAcceptor.acceptPoint(zone, actType, p));

        Coord coord = new Coord(p.getX(), p.getY());
        Coord coordt = BerlinZoneUtils.ZONE_TO_NETWORK_COORD_TRANSFORMATION.transform(coord);
        Link link = NetworkUtils.getNearestLink(network, coordt);

        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coordt);
        activity.setLinkId(link.getId());
        return activity;
    }
}
