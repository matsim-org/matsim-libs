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

package org.matsim.contrib.dynagent.examples.random;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dynagent.DynLeg;
import org.matsim.core.gbl.MatsimRandom;


public class RandomDynLeg
    implements DynLeg
{
    private final Network network;

    private Id currentLinkId;
    private Id nextLinkId;
    private Id destinationLinkId;


    public RandomDynLeg(Id fromLinkId, Network network)
    {
        this.network = network;
        currentLinkId = fromLinkId;

        doRandomChoice();
    }


    @Override
    public void finalizeAction(double now)
    {}


    @Override
    public void movedOverNode(Id newLinkId)
    {
        currentLinkId = newLinkId;
        doRandomChoice();
    }


    @Override
    public Id getCurrentLinkId()
    {
        return currentLinkId;
    }


    @Override
    public Id getNextLinkId()
    {
        return nextLinkId;
    }


    @Override
    public Id getDestinationLinkId()
    {
        return destinationLinkId;
    }


    private void doRandomChoice()
    {
        //Do I want to end at this link?
        if (MatsimRandom.getRandom().nextInt(10) == 0) {//10% chance
            nextLinkId = null;
            destinationLinkId = currentLinkId;
        }
        else {
            //Where do I want to move next?
            Link currentLink = network.getLinks().get(currentLinkId);
            Map<Id, ?> possibleNextLinks = currentLink.getToNode().getOutLinks();

            //Let's choose the next link randomly
            nextLinkId = RandomDynAgentLogic.chooseRandomElement(possibleNextLinks.keySet());

            //at this point the destination can be anything, QSim does not take it into account
            destinationLinkId = null;
        }
    }


    @Override
    public String getMode()
    {
        return TransportMode.car;
    }


    @Override
    public void arrivedOnLinkByNonNetworkMode(Id linkId)
    {
        currentLinkId = linkId;
    }


    public Double getExpectedTravelTime()
    {
        return MatsimRandom.getRandom().nextDouble() * 3600;
    }
}
