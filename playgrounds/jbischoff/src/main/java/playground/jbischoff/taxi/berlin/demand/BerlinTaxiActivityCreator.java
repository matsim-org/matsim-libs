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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;

import playground.michalm.demand.DefaultActivityCreator;
import playground.michalm.zone.Zone;


public class BerlinTaxiActivityCreator
    extends DefaultActivityCreator
{
    private final static Id<Zone> TXLLORID = Id.create("12214125", Zone.class);
    private final static Id<Zone> SXFLORID = Id.create("12061433", Zone.class);


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
            return super.createActivity(zone, actType);
        }
    }
}
