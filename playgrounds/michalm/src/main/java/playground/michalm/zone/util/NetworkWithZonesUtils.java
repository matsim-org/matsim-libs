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

package playground.michalm.zone.util;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;

import playground.michalm.zone.*;


public class NetworkWithZonesUtils
{
    //if SRSs of the network and zones are different, zoneFinder should convert between CRSs
    public static Map<Id<Link>, Zone> createLinkToZoneMap(Network network, ZoneFinder zoneFinder)
    {
        Map<Id<Link>, Zone> linkToZone = new HashMap<>();

        for (Link l : network.getLinks().values()) {
            linkToZone.put(l.getId(), zoneFinder.findZone(l.getToNode().getCoord()));
        }

        return linkToZone;
    }


    public static Map<Id<Node>, Zone> createNodeToZoneMap(Network network, ZoneFinder zoneFinder)
    {
        Map<Id<Node>, Zone> nodeToZone = new HashMap<>();

        for (Node n : network.getNodes().values()) {
            nodeToZone.put(n.getId(), zoneFinder.findZone(n.getCoord()));
        }

        return nodeToZone;
    }
}
