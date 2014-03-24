/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.util.stats;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import playground.michalm.taxi.schedule.*;


public class TaxiStatsCalculator
{
    public TaxiStats calculateStats(VrpData data)
    {
        TaxiStats evaluation = new TaxiStats();

        for (Vehicle v : data.getVehicles()) {
            evaluateSchedule(data, TaxiSchedules.getSchedule(v), evaluation);
        }

        return evaluation;
    }


    private void evaluateSchedule(VrpData data, Schedule<TaxiTask> schedule, TaxiStats eval)
    {
        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        if (schedule.getTaskCount() < 1) {
            throw new RuntimeException("count=0 ==> must be unplanned!");
        }

        for (TaxiTask t : schedule.getTasks()) {
            double time = t.getEndTime() - t.getBeginTime();

            switch (t.getTaxiTaskType()) {
                case PICKUP_DRIVE:
                    eval.pickupDriveTime += time;

                    if (eval.maxPickupDriveTime < time) {
                        eval.maxPickupDriveTime = time;
                    }

                    eval.pickupDriveTimeStats.addValue(time);

                    break;

                case DROPOFF_DRIVE:
                    eval.dropoffDriveTime += time;
                    break;

                case CRUISE_DRIVE:
                    eval.cruiseTime += time;
                    break;

                case PICKUP_STAY:
                    eval.pickupTime += time;

                    Request req = ((TaxiPickupStayTask)t).getRequest();
                    double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                    eval.passengerWaitTime += waitTime;

                    if (eval.maxPassengerWaitTime < waitTime) {
                        eval.maxPassengerWaitTime = waitTime;
                    }

                    eval.passengerWaitTimeStats.addValue(waitTime);

                    break;

                case DROPOFF_STAY:
                    eval.dropoffTime += time;
                    break;

                case CHARGE_STAY:
                    eval.chargeTime += time;
                    
                case WAIT_STAY:
                    eval.waitTime += time;
            }
        }

        double latestValidEndTime = schedule.getVehicle().getT1();
        double actualEndTime = schedule.getEndTime();

        eval.overTime += Math.max(actualEndTime - latestValidEndTime, 0);
    }


    public static class TaxiStats
    {
        private double pickupDriveTime;
        private double dropoffDriveTime;
        private double pickupTime;
        private double dropoffTime;
        private double cruiseTime;
        private double chargeTime;
        private double waitTime;
        private double overTime;
        private double passengerWaitTime;

        private double maxPickupDriveTime;
        private double maxPassengerWaitTime;

        private final DescriptiveStatistics pickupDriveTimeStats = new DescriptiveStatistics();
        private final DescriptiveStatistics passengerWaitTimeStats = new DescriptiveStatistics();


        public double getPickupDriveTime()
        {
            return pickupDriveTime;
        }


        public double getDropoffDriveTime()
        {
            return dropoffDriveTime;
        }


        public double getPickupTime()
        {
            return pickupTime;
        }


        public double getDropoffTime()
        {
            return dropoffTime;
        }


        public double getCruiseTime()
        {
            return cruiseTime;
        }
        
        
        public double getChargeTime()
        {
            return chargeTime;
        }


        public double getWaitTime()
        {
            return waitTime;
        }


        public double getOverTime()
        {
            return overTime;
        }


        public double getPassengerWaitTime()
        {
            return passengerWaitTime;
        }


        public double getMaxPickupDriveTime()
        {
            return maxPickupDriveTime;
        }


        public double getMaxPassengerWaitTime()
        {
            return maxPassengerWaitTime;
        }


        public DescriptiveStatistics getPickupDriveTimeStats()
        {
            return pickupDriveTimeStats;
        }


        public DescriptiveStatistics getPassengerWaitTimeStats()
        {
            return passengerWaitTimeStats;
        }


        public static final String HEADER = "PickupDriveT\t" //
                + "MaxPickupDriveT" //
                + "DeliveryDriveT\t"//
                + "PickupT\t" //
                + "DropoffT\t" //
                + "WaitT\t" //
                + "PassengerWaitT\t" //
                + "MaxPassengerWaitT";


        @Override
        public String toString()
        {
            return new StringBuilder().append(pickupDriveTime).append('\t') //
                    .append(maxPickupDriveTime).append('\t') //
                    .append(dropoffDriveTime).append('\t') //
                    .append(pickupTime).append('\t') //
                    .append(dropoffTime).append('\t') //
                    .append(waitTime).append('\t') //
                    .append(passengerWaitTime).append('\t') //
                    .append(maxPassengerWaitTime).toString();
        }
    }
}
