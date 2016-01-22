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

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.*;
import org.matsim.contrib.util.DistanceUtils;
import org.matsim.core.router.util.*;

import playground.jbischoff.energy.charging.taxi.ElectricTaxiChargingHandler;
import playground.michalm.taxi.optimizer.TaxiOptimizerContext;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.scheduler.*;


/**
 * @author jbischoff
 */

public class NOSRankTaxiOptimizer
    extends RuleBasedTaxiOptimizer
{
    protected final TaxiOptimizerContext optimConfig;
    protected final IdleRankVehicleFinder idleVehicleFinder;
    protected final RankModeTaxiScheduler rankModeTaxiScheduler;
    protected ElectricTaxiChargingHandler ecabhandler;
    private TaxiRankHandler rankHandler;
    protected final static double NEEDS_TO_CHARGE_SOC = 0.2;
    private int homebound = 0;
    protected boolean idleRankMode;

    //    private final List<Id> shortTimeIdlers;

    private Map<Id, Link> nearestRanks;
    private HashMap<Id, Link> nearestChargers;
    private static Logger log = Logger.getLogger(NOSRankTaxiOptimizer.class);


    public static NOSRankTaxiOptimizer createNOSRankTaxiOptimizer(MatsimVrpContext context,
            TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility,
            String workingDir)
    {
        RankModeTaxiScheduler scheduler = new RankModeTaxiScheduler(context, params, travelTime,
                travelDisutility);
        final IdleRankVehicleFinder idleVehicleFinder = new IdleRankVehicleFinder(context,
                scheduler);

        //        FilterFactory filterFactory = new FilterFactory() {
        //            @Override
        //            public RequestFilter createRequestFilter()
        //            {
        //                return RequestFilter.NO_FILTER;
        //            }
        //
        //
        //            @Override
        //            public VehicleFilter createVehicleFilter()
        //            {
        //                return idleVehicleFinder;
        //            }
        //        };

        //        TaxiOptimizerContext optimConfig = new TaxiOptimizerContext(context, travelTime,
        //                travelDisutility, scheduler, 0, 0, Goal.MIN_WAIT_TIME, workingDir, null);

        throw new RuntimeException("Lots of changes prevent this from running");

        //return new NOSRankTaxiOptimizer(optimConfig, idleVehicleFinder);
    }


    protected NOSRankTaxiOptimizer(TaxiOptimizerContext optimConfig,
            IdleRankVehicleFinder vehicleFinder)
    {
        super(optimConfig);
        this.optimConfig = optimConfig;
        this.idleVehicleFinder = vehicleFinder;
        this.rankModeTaxiScheduler = (RankModeTaxiScheduler)optimConfig.scheduler;
        rankModeTaxiScheduler.optimizer = this;
    }


    public void setRankHandler(TaxiRankHandler rankHandler)
    {
        this.rankHandler = rankHandler;
    }


    public void setRankMode(boolean rankMode)
    {
        ((RankModeTaxiScheduler)optimConfig.scheduler).rankmode = rankMode;
    }


    private static class RankModeTaxiScheduler
        extends TaxiScheduler
    {
        private boolean rankmode;
        private NOSRankTaxiOptimizer optimizer;


        public RankModeTaxiScheduler(MatsimVrpContext context, TaxiSchedulerParams params,
                TravelTime travelTime, TravelDisutility travelDisutility)
        {
            super(context, params, travelTime, travelDisutility);
        }


        @Override
        protected void appendTasksAfterDropoff(Schedule<TaxiTask> schedule)
        {
            if (rankmode) {
                TaxiDropoffTask dropoffStayTask = (TaxiDropoffTask)Schedules.getLastTask(schedule);

                Link link = dropoffStayTask.getLink();
                Link startLink = schedule.getVehicle().getStartLink();

                if (link != startLink) {
                    double t5 = dropoffStayTask.getEndTime();
                    VrpPathWithTravelData path = calcPath(link, startLink, t5);
                    schedule.addTask(new TaxiDriveTask(path));
                }
            }

            appendStayTask(schedule);
        }


        protected void scheduleRankReturn(Vehicle veh, double time, boolean charge, boolean home)
        {
            @SuppressWarnings("unchecked")
            Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
            TaxiStayTask last = (TaxiStayTask)Schedules.getLastTask(veh.getSchedule());
            if (last.getStatus() != TaskStatus.STARTED)
                throw new IllegalStateException();

            last.setEndTime(time);
            Link currentLink = last.getLink();
            Link nearestRank;
            if (charge)

            {
                nearestRank = optimizer.getNearestFreeCharger(currentLink.getId());
                log.info("veh" + veh.getId() + " to charge");
            }
            else if (home) {
                nearestRank = veh.getStartLink();
                //        log.info("start" + veh.getId()+ " at " + time + " t1 " + veh.getT1());
                optimizer.homebound++;
            }
            else
                nearestRank = optimizer.getNearestFreeRank(currentLink.getId());

            VrpPathWithTravelData path = calcPath(currentLink, nearestRank, time);
            if (path.getArrivalTime() > veh.getT1())
                return; // no rank return if vehicle is going out of service anyway
            sched.addTask(new TaxiDriveTask(path));
            sched.addTask(new TaxiStayTask(path.getArrivalTime(), veh.getT1(), nearestRank));

        }
    }


    public void setEcabhandler(ElectricTaxiChargingHandler ecabhandler)
    {
        this.ecabhandler = ecabhandler;
        this.idleVehicleFinder.addEcabHandler(ecabhandler);
    }


    public void setIdleRankMode(boolean b)
    {
        this.idleRankMode = b;
    }


    public void doSimStep(double time)
    {
        if (time % 60. == 0.) {
            checkWaitingVehiclesBatteryState(time);

        }
        if (this.idleRankMode) {
            if (time % 60. == 5.) {

                sendIdlingTaxisBackToRank(time);

            }
        }
        if (time % 3600 == 0) {
            sendIdlingTaxisHome(time);
        }
    }


    private void checkWaitingVehiclesBatteryState(double time)
    {

        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles().values()) {

            if (!optimConfig.scheduler.isIdle(veh))
                continue;
            if (veh.getSchedule().getStatus() != ScheduleStatus.STARTED)
                continue;
            if (! (Schedules.getLastTask(veh.getSchedule()).getTaskIdx() == veh.getSchedule()
                    .getCurrentTask().getTaskIdx()))
                continue;

            if (veh.getSchedule().getCurrentTask().getType().equals(TaskType.STAY)) {

                TaxiStayTask twst = (TaxiStayTask)veh.getSchedule().getCurrentTask();

                if (!this.ecabhandler.isAtCharger(twst.getLink().getId())) {
                    if (this.needsToCharge(veh.getId())) {
                        log.info("veh" + veh.getId() + " to charge");
                        rankModeTaxiScheduler.scheduleRankReturn(veh, time, true, false);
                    }

                }

            }

        }
    }


    protected Link getNearestFreeRank(Id linkId)
    {
        Link nearestLink = null;
        if (this.rankHandler.getRanks().get(this.nearestRanks.get(linkId).getId()).hasCapacity()) {
            nearestLink = this.nearestRanks.get(linkId);
        }
        else {
            Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks()
                    .get(linkId);

            double bestTravelCost = Double.MAX_VALUE;
            for (Id cid : rankHandler.getRanks().keySet()) {

                Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks()
                        .get(cid);
                double currentCost = DistanceUtils.calculateSquaredDistance(positionLink,
                        currentLink);
                if ( (currentCost < bestTravelCost) && (this.rankHandler.getRanks()
                        .get(this.nearestRanks.get(linkId).getId()).hasCapacity())) {
                    bestTravelCost = currentCost;
                    nearestLink = currentLink;
                }
            }
        }
        if (nearestLink == null)
            nearestLink = this.nearestRanks.get(linkId);

        //assumption: all ranks full --> drive to next rank and wait until something becomes available

        return nearestLink;
    }


    protected Link getNearestFreeCharger(Id linkId)
    {
        Link nearestLink = null;
        if (this.ecabhandler.getChargers().get(this.nearestChargers.get(linkId).getId())
                .hasCapacity()) {
            nearestLink = this.nearestChargers.get(linkId);
        }
        else {
            log.info("no capacity at charger" + this.nearestChargers.get(linkId));
            Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks()
                    .get(linkId);

            double bestTravelCost = Double.MAX_VALUE;
            for (Id cid : ecabhandler.getChargers().keySet()) {

                Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks()
                        .get(cid);
                double currentCost = DistanceUtils.calculateSquaredDistance(positionLink,
                        currentLink);
                if ( (currentCost < bestTravelCost) && (this.ecabhandler.getChargers()
                        .get(this.nearestChargers.get(linkId).getId()).hasCapacity())) {
                    bestTravelCost = currentCost;
                    nearestLink = currentLink;
                }
            }
        }
        if (nearestLink == null)
            nearestLink = this.nearestChargers.get(linkId);
        //assumption: all chargers full --> drive to next charger and wait until something becomes available

        return nearestLink;

    }


    private Link findNearestRank(Id positionLinkId)
    {
        Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks()
                .get(positionLinkId);
        Link nearestRankLink = null;
        double bestTravelCost = Double.MAX_VALUE;
        for (Id cid : rankHandler.getRanks().keySet()) {

            Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks().get(cid);
            double currentCost = DistanceUtils.calculateSquaredDistance(positionLink, currentLink);
            if (currentCost < bestTravelCost) {
                bestTravelCost = currentCost;
                nearestRankLink = currentLink;
            }
        }
        return nearestRankLink;
    }


    private Link findNearestCharger(Id positionLinkId)
    {
        Link positionLink = optimConfig.context.getScenario().getNetwork().getLinks()
                .get(positionLinkId);
        Link nearestChargerLink = null;
        double bestTravelCost = Double.MAX_VALUE;
        for (Id cid : ecabhandler.getChargers().keySet()) {

            Link currentLink = optimConfig.context.getScenario().getNetwork().getLinks().get(cid);
            double currentCost = DistanceUtils.calculateSquaredDistance(positionLink, currentLink);
            if (currentCost < bestTravelCost) {
                bestTravelCost = currentCost;
                nearestChargerLink = currentLink;
            }
        }
        return nearestChargerLink;
    }


    public void sendIdlingTaxisBackToRank(double time)
    {
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles().values()) {
            if (!optimConfig.scheduler.isIdle(veh))
                continue;
            if (veh.getSchedule().getStatus() != ScheduleStatus.STARTED)
                continue;
            if (! (Schedules.getLastTask(veh.getSchedule()).getTaskIdx() == veh.getSchedule()
                    .getCurrentTask().getTaskIdx()))
                continue;

            if (veh.getSchedule().getCurrentTask().getType().equals(TaskType.STAY)) {

                TaxiStayTask twst = (TaxiStayTask)veh.getSchedule().getCurrentTask();
                if (!this.rankHandler.isRankLocation(twst.getLink().getId())) {
                    if (time - twst.getBeginTime() > 60.) {
                        rankModeTaxiScheduler.scheduleRankReturn(veh, time, false, false);

                    }

                }

            }

        }
    }


    private void sendIdlingTaxisHome(double time)
    {
        int homeboundthishour = 0;
        for (Vehicle veh : optimConfig.context.getVrpData().getVehicles().values()) {
            if (time + 3600 < veh.getT1())
                continue;
            if (!optimConfig.scheduler.isIdle(veh))
                continue;
            if (veh.getSchedule().getStatus() != ScheduleStatus.STARTED)
                continue;
            if (! (Schedules.getLastTask(veh.getSchedule()).getTaskIdx() == veh.getSchedule()
                    .getCurrentTask().getTaskIdx()))
                continue;

            if (veh.getSchedule().getCurrentTask().getType().equals(TaskType.STAY)) {

                rankModeTaxiScheduler.scheduleRankReturn(veh, time, false, true);
                homeboundthishour++;

            }

        }
        log.info(time % 3600 + " hrs: " + homeboundthishour + " sent home" + this.homebound
                + " total.");
    }


    public void createNearestRankDb()
    {
        this.nearestRanks = new HashMap<Id, Link>();

        for (Link l : optimConfig.context.getScenario().getNetwork().getLinks().values()) {
            Link rankLink = findNearestRank(l.getId());
            this.nearestRanks.put(l.getId(), rankLink);

        }

        log.info("...done");

    }


    public void createNearestChargerDb()
    {
        this.nearestChargers = new HashMap<Id, Link>();

        for (Link l : optimConfig.context.getScenario().getNetwork().getLinks().values()) {
            Link chargerLink = findNearestCharger(l.getId());
            this.nearestChargers.put(l.getId(), chargerLink);

        }

        log.info("...done");

    }


    private boolean needsToCharge(Id vid)
    {
        return (this.ecabhandler.getRelativeTaxiSoC(vid) <= NEEDS_TO_CHARGE_SOC);
    }

}