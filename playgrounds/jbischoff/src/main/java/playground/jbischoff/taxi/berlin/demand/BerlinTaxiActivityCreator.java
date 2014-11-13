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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.michalm.demand.DefaultActivityCreator;
import playground.michalm.zone.Zone;


public class BerlinTaxiActivityCreator
    extends DefaultActivityCreator
{
    private final static Id<Zone> TXLLORID = Id.create("12214125", Zone.class);
    private final static Id<Zone> SXFLORID = Id.create("12061433", Zone.class);
    private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            "EPSG:25833", TransformationFactory.DHDN_GK4);

    public BerlinTaxiActivityCreator(Scenario scenario)
    {
        super(scenario);
    }


    @Override
    public Activity createActivity(Zone zone, String actType)
    {
        Link link;
        if (zone.getId().equals(TXLLORID)) {
            if (actType.equals("arrival")) {
                link = network.getLinks().get(Id.create(-35954, Link.class));
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;
            }
            else {
                link = network.getLinks().get(Id.create(-35695, Link.class));
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;

            }
        }
        else if (zone.getId().equals(SXFLORID)) {
            if (actType.equals("arrival")) {
                link = network.getLinks().get(Id.create(-35829, Link.class));
                Coord coord = link.getCoord();
                ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
                activity.setLinkId(link.getId());
                return activity;
            }
            else {
                link = network.getLinks().get(Id.create(-35828, Link.class));
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

        Coord coord = scenario.createCoord(p.getX(), p.getY());
        Coord coordt = ct.transform(coord);
        Link link = NetworkUtils.getNearestLink(network, coordt);

        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coordt);
        activity.setLinkId(link.getId());
        return activity;
    }
}
