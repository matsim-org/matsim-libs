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

package playground.michalm.vrp.taxi.wal;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.DynAction;
import playground.michalm.vrp.taxi.taxicab.TaxiAgentLogic;


public class WalTaxiAgentLogic
    extends TaxiAgentLogic
{
    private final WalTaxiSimEngine taxiSimEngine;
    private final Vehicle vrpVehicle;


    public WalTaxiAgentLogic(Vehicle vrpVehicle, WalTaxiSimEngine taxiSimEngine)
    {
        super(vrpVehicle, taxiSimEngine);

        this.taxiSimEngine = taxiSimEngine;
        this.vrpVehicle = vrpVehicle;
    }


    @Override
    public DynAction computeNextAction(DynAction oldAction, double now)
    {
        DynAction nextAction = super.computeNextAction(oldAction, now);
        taxiSimEngine.nextTask(vrpVehicle);
        return nextAction;
    }


    @Override
    public void notifyMoveOverNode(Id oldLinkId, Id newLinkId)
    {
        taxiSimEngine.notifyMoveOverNode(vrpVehicle, oldLinkId, newLinkId);
    }
}
