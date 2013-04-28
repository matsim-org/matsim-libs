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

package playground.michalm.vrp.taxi;

import org.matsim.api.core.v01.Id;

import playground.michalm.dynamic.DynLegImpl;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;

import com.google.common.collect.Iterators;


public class TaxiLeg
    extends DynLegImpl
{
    public TaxiLeg(ShortestPath path, Id destinationLinkId)
    {
        super(Iterators.forArray(path.linkIds), destinationLinkId);
    }


    public void endLeg(double now)
    {}
}
