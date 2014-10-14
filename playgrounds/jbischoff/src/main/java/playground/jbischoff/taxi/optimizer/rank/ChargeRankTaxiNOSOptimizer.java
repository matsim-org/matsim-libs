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

package playground.jbischoff.taxi.optimizer.rank;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.util.DistanceUtils;

import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration;
import playground.michalm.taxi.schedule.TaxiCruiseDriveTask;
import playground.michalm.taxi.schedule.TaxiWaitStayTask;

public class ChargeRankTaxiNOSOptimizer
    extends NOSRankTaxiOptimizer
{
    private TaxiRankHandler rankHandler;
    
    protected ChargeRankTaxiNOSOptimizer(TaxiOptimizerConfiguration optimConfig,
            IdleRankVehicleFinder vehicleFinder)
    {
        super(optimConfig, vehicleFinder);

    }
    private Link findNearestFreeRank(Id positionLinkId)
    {
        Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks().get(positionLinkId);
        Link nearestRankLink = null;
        double bestTravelCost = Double.MAX_VALUE;
        for (Id cid : this.rankHandler.getRanks().keySet()) {

            Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks().get(cid);
            double currentCost = DistanceUtils.calculateSquaredDistance(positionLink, currentLink);
            if ((currentCost < bestTravelCost) && (this.rankHandler.hasCapacityAtRank(cid))) {
                bestTravelCost = currentCost;
                nearestRankLink = currentLink;
            }
        }
        return nearestRankLink;
    }

    private Link findNearestFreeCharger(Id positionLinkId)
    {
        Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks().get(positionLinkId);
        Link nearestRankLink = null;
        double bestTravelCost = Double.MAX_VALUE;
        for (Id cid : ecabhandler.getChargers().keySet()) {

            Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks().get(cid);
            double currentCost = DistanceUtils.calculateSquaredDistance(positionLink, currentLink);
            if ((currentCost < bestTravelCost) && (this.rankHandler.hasCapacityAtRank(cid))) {
                bestTravelCost = currentCost;
                nearestRankLink = currentLink;
            }
        }
        return nearestRankLink;
    }
    
    protected void scheduleRankReturn(Vehicle veh, double time, boolean charge)
    {
        @SuppressWarnings("unchecked")
        Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
        TaxiWaitStayTask last = (TaxiWaitStayTask)Schedules.getLastTask(veh.getSchedule());
        if (last.getStatus() != TaskStatus.STARTED)
            throw new IllegalStateException();

        last.setEndTime(time);
        Link currentLink = last.getLink();
        
        Link nearestRank;
        if (!charge) nearestRank = getNearestFreeRank(currentLink.getId());
        else nearestRank = getNearestCharger(currentLink.getId());

        VrpPathWithTravelData path = optimConfig.calculator
                .calcPath(currentLink, nearestRank, time);
        if (path.getArrivalTime() > veh.getT1())
            return; // no rank return if vehicle is going out of service anyway
        sched.addTask(new TaxiCruiseDriveTask(path));
        sched.addTask(new TaxiWaitStayTask(path.getArrivalTime(), veh.getT1(), nearestRank));

    }
    
    
    private Link getNearestCharger(Id id)
    {
        return findNearestFreeCharger(id);
    }
    protected Link getNearestFreeRank(Id linkId){
        return findNearestFreeRank(linkId);
    }
    
    

}
