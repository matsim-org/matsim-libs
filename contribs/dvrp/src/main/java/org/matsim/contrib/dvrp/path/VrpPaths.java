/* *********************************************************************** *
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

package org.matsim.contrib.dvrp.path;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class VrpPaths
{
    /**
     * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
     */
    public static VrpPathWithTravelData calcAndCreatePath(Link fromLink, Link toLink,
            double departureTime, LeastCostPathCalculator router, TravelTime travelTime)
    {
        Path path = null;
        if (fromLink != toLink) {
            //calc path for departureTime+1 (we need 1 second to move over the node)
            path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departureTime + 1, null, null);
        }

        return VrpPaths.createPath(fromLink, toLink, departureTime, path, travelTime);
    }


    public static VrpPathWithTravelData createZeroLengthPath(Link fromTolink, double departureTime)
    {
        return new VrpPathWithTravelDataImpl(departureTime, 0, new Link[] { fromTolink },
                new double[] { 0 });
    }


    public static VrpPathWithTravelData createPath(Link fromLink, Link toLink, double departureTime,
            Path path, TravelTime travelTime)
    {
        if (fromLink == toLink) {
            return createZeroLengthPath(fromLink, departureTime);
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
        double linkTT = FIRST_LINK_TT;
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
        linkTT = getLastLinkTT(toLink, currentTime);//as long as we cannot divert from the last link this is okay
        linkTTs[count + 1] = linkTT;
        double totalTT = 1 + path.travelTime + linkTT;

        return new VrpPathWithTravelDataImpl(departureTime, totalTT, links, linkTTs);
    }


    public static final double FIRST_LINK_TT = 1;


    public static double getLastLinkTT(Link lastLink, double time)
    {
        return lastLink.getLength() / lastLink.getFreespeed(time);
    }
}
