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

package org.matsim.contrib.dvrp.path;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class VrpPathFactoryImpl implements VrpPathFactory
{
    private final TravelTime travelTime;
    private final TravelDisutility travelDisutility;


    public VrpPathFactoryImpl(TravelTime travelTime, TravelDisutility travelDisutility)
    {
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
    }


    @Override
    public VrpPathWithTravelData createPath(Link fromLink, Link toLink, double departureTime,
            Path path)
    {
        if (fromLink == toLink) {
            return new VrpPathWithTravelDataImpl(departureTime, 0, 0, new Link[] { fromLink },
                    new double[] { 0 });
        }

        int count = path.links.size();
        Link[] links = new Link[count + 2];
        double[] linkTTs = new double[count + 2];

        //we start at the end of fromLink
        //actually, in QSim, it usually takes 1 second to move over the first node
        //(when INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES is ON;
        //otherwise it can take much longer)
        double currentTime = departureTime;
        links[0] = fromLink;
        double linkTT = 1.;
        linkTTs[0] = linkTT;
        currentTime += linkTT;

        for (int i = 1; i <= count; i++) {
            Link link = path.links.get(i - 1);
            links[i] = link;
            linkTT = travelTime.getLinkTravelTime(link, currentTime, null, null);
            linkTTs[i] = linkTT;
            currentTime += linkTT;
        }

        //there is no extra time spent on queuing at the end of the last link
        links[count + 1] = toLink;
        linkTT = toLink.getLength() / toLink.getFreespeed(currentTime);//as long as we cannot divert from the last link this is okay
        linkTTs[count + 1] = linkTT;

        double totalTT = 1 + path.travelTime + linkTT;
        double totalCost = path.travelCost
                + travelDisutility.getLinkMinimumTravelDisutility(toLink);

        return new VrpPathWithTravelDataImpl(departureTime, totalTT, totalCost, links, linkTTs);
    }

}
