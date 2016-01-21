/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantLinkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;


public class VariableIntervalTimeVariantLinkFactory
    implements LinkFactory
{

    @Override
    public Link createLink(Id<Link> id, Node from, Node to, Network network, double length,
            double freespeed, double capacity, double nOfLanes)
    {
        return TimeVariantLinkImpl.createLinkWithVariableIntervalAttributes(id, from, to, network,
                length, freespeed, capacity, nOfLanes);
    }

}
