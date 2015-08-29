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

package org.matsim.contrib.dvrp.path;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;


public class VrpPathCalculatorImpl
    implements VrpPathCalculator
{
    private final LeastCostPathCalculator router;
    private final VrpPathFactory vrpPathFactory;


    public VrpPathCalculatorImpl(LeastCostPathCalculator router, VrpPathFactory vrpPathFactory)
    {
        this.router = router;
        this.vrpPathFactory = vrpPathFactory;
    }


    /**
     * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
     */
    @Override
    public VrpPathWithTravelData calcPath(Link fromLink, Link toLink, double departureTime)
    {
        Path path = null;
        if (fromLink != toLink) {
            //calc path for departureTime+1 (we need 1 second to move over the node)
            path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(),
                    departureTime + 1, null, null);
        }

        return vrpPathFactory.createPath(fromLink, toLink, departureTime, path);
    }
}
