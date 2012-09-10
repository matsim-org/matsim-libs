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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;


/**
 * This class is used for route visualization in OTFVis 
 *
 * @author michalm
 *
 */
public class DynAgentWithPlan
    implements MobsimDriverAgent, PlanAgent
{
    private final DynAgent dynAgent;
    private final DynPlanFactory planFactory;


    public DynAgentWithPlan(DynAgent dynAgent, DynPlanFactory planFactory)
    {
        this.dynAgent = dynAgent;
        this.planFactory = planFactory;
    }


    @Override
    public double getActivityEndTime()
    {
        return dynAgent.getActivityEndTime();
    }


    @Override
    public void endActivityAndComputeNextState(double now)
    {
        dynAgent.endActivityAndComputeNextState(now);
    }


    @Override
    public void endLegAndComputeNextState(double now)
    {
        dynAgent.endLegAndComputeNextState(now);
    }

    @Override
    public void abort(double now) {
    	dynAgent.abort(now) ;
    }



    @Override
    public Double getExpectedTravelTime()
    {
        return dynAgent.getExpectedTravelTime();
    }


    @Override
    public MobsimAgent.State getState()
    {
        return dynAgent.getState();
    }


    @Override
    public String getMode()
    {
        return dynAgent.getMode();
    }


    @Override
    public void notifyArrivalOnLinkByNonNetworkMode(Id linkId)
    {
        dynAgent.notifyArrivalOnLinkByNonNetworkMode(linkId);
    }


    @Override
    public Id getCurrentLinkId()
    {
        return dynAgent.getCurrentLinkId();
    }


    @Override
    public Id getDestinationLinkId()
    {
        return dynAgent.getDestinationLinkId();
    }


    @Override
    public Id getId()
    {
        return dynAgent.getId();
    }


    @Override
    public Id chooseNextLinkId()
    {
        return dynAgent.chooseNextLinkId();
    }


    @Override
    public void notifyMoveOverNode(Id newLinkId)
    {
        dynAgent.notifyMoveOverNode(newLinkId);
    }


    @Override
    public void setVehicle(MobsimVehicle veh)
    {
        dynAgent.setVehicle(veh);
    }


    @Override
    public MobsimVehicle getVehicle()
    {
        return dynAgent.getVehicle();
    }


    @Override
    public Id getPlannedVehicleId()
    {
        return dynAgent.getPlannedVehicleId();
    }


    @Override
    public PlanElement getCurrentPlanElement()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public PlanElement getNextPlanElement()
    {
        throw new UnsupportedOperationException();

    }


    @Override
    public Plan getSelectedPlan()
    {
        return planFactory.create(dynAgent);
    }
}
