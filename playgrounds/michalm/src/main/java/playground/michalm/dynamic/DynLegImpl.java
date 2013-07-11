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

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;


public class DynLegImpl
    implements DynLeg
{
    private final NetworkRoute route;
    private int currentLinkIdx;


    public DynLegImpl(NetworkRoute route)
    {
        this.route = route;
        currentLinkIdx = -1;
    }


    @Override
    public void movedOverNode(Id oldLinkId, Id newLinkId, int time)
    {
        currentLinkIdx++;
    }


    @Override
    public Id getCurrentLinkId()
    {
        if (currentLinkIdx == -1) {
            return route.getStartLinkId();
        }

        List<Id> linkIds = route.getLinkIds();

        if (currentLinkIdx == linkIds.size()) {
            return route.getEndLinkId();
        }

        return linkIds.get(currentLinkIdx);
    }


    @Override
    public Id getNextLinkId()
    {
        List<Id> linkIds = route.getLinkIds();

        if (currentLinkIdx == linkIds.size()) {
            return null;
        }

        if (currentLinkIdx == linkIds.size() - 1) {
            return route.getEndLinkId();
        }

        return linkIds.get(currentLinkIdx + 1);
    }


    @Override
    public Id getDestinationLinkId()
    {
        return route.getEndLinkId();
    }
}
