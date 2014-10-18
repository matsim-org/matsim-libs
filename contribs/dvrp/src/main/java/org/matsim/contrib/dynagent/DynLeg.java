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

package org.matsim.contrib.dynagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public interface DynLeg
    extends DynAction
{
    String getMode();


    void movedOverNode(Id<Link> newLinkId);


    void arrivedOnLinkByNonNetworkMode(Id<Link> linkId);


    Id<Link> getCurrentLinkId();


    Id<Link> getNextLinkId();


    Id<Link> getDestinationLinkId();


    Double getExpectedTravelTime();
}
