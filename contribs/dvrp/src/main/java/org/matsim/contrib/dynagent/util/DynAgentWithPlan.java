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

package org.matsim.contrib.dynagent.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;


/**
 * This class is used for route visualization in OTFVis
 * 
 * @author michalm
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
    public void setStateToAbort(double now)
    {
        dynAgent.setStateToAbort(now);
    }


    @Override
    public Double getExpectedTravelTime()
    {
        return dynAgent.getExpectedTravelTime();
    }

    @Override
    public Double getExpectedTravelDistance() {
        return dynAgent.getExpectedTravelDistance();
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
    public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId)
    {
        dynAgent.notifyArrivalOnLinkByNonNetworkMode(linkId);
    }


    @Override
    public Id<Link> getCurrentLinkId()
    {
        return dynAgent.getCurrentLinkId();
    }


    @Override
    public Id<Link> getDestinationLinkId()
    {
        return dynAgent.getDestinationLinkId();
    }


    @Override
    public Id<Person> getId()
    {
        return dynAgent.getId();
    }


    @Override
    public Id<Link> chooseNextLinkId()
    {
        return dynAgent.chooseNextLinkId();
    }


    @Override
    public void notifyMoveOverNode(Id<Link> newLinkId)
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
    public Id<Vehicle> getPlannedVehicleId()
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
    public Plan getCurrentPlan()
    {
        return planFactory.create(dynAgent);
    }
    
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return dynAgent.isWantingToArriveOnCurrentLink();
	}

}

