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

package playground.michalm.dynamic;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;


public class DynLegImpl
    implements DynLeg
{
    private Iterator< ? extends Id> linkIdIter;
    private Id destinationLinkId;


    public DynLegImpl(NetworkRoute route)
    {
        this(route.getLinkIds().iterator(), route.getEndLinkId());
    }


    public DynLegImpl(Iterator< ? extends Id> linkIdIter, Id destinationLinkId)
    {
        this.linkIdIter = linkIdIter;
        this.destinationLinkId = destinationLinkId;
    }


    @Override
    public Id getNextLinkId()
    {
        if (linkIdIter.hasNext()) {
            return linkIdIter.next();
        }

        return null;
    }


    @Override
    public Id getDestinationLinkId()
    {
        return destinationLinkId;
    }
}
