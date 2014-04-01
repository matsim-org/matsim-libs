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
    private TaxiStats stats;


    public TaxiStats calculateStats(Iterable<? extends Vehicle> vehicles)
    {
        stats = new TaxiStats();

        for (Vehicle v : vehicles) {
            calculateStatsImpl(v);
        }

        return stats;
    }


    public TaxiStats calculateStats(Vehicle vehicle)
    {
        stats = new TaxiStats();
        calculateStatsImpl(vehicle);
        return stats;
    }


    private void calculateStatsImpl(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(vehicle);

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
                    stats.pickupDriveTime += time;

                    if (stats.maxPickupDriveTime < time) {
                        stats.maxPickupDriveTime = time;
                    }

                    stats.pickupDriveTimeStats.addValue(time);

                    break;

                case DROPOFF_DRIVE:
                    stats.dropoffDriveTime += time;
                    break;

                case CRUISE_DRIVE:
                    stats.cruiseTime += time;
                    break;

                case PICKUP_STAY:
                    stats.pickupTime += time;

                    Request req = ((TaxiPickupStayTask)t).getRequest();
                    double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                    stats.passengerWaitTime += waitTime;

                    if (stats.maxPassengerWaitTime < waitTime) {
                        stats.maxPassengerWaitTime = waitTime;
                    }

                    stats.passengerWaitTimeStats.addValue(waitTime);

                    break;

                case DROPOFF_STAY:
                    stats.dropoffTime += time;
                    break;

                case WAIT_STAY:
                    stats.waitTime += time;
            }
        }

        double latestValidEndTime = schedule.getVehicle().getT1();
        double actualEndTime = schedule.getEndTime();

        stats.overTime += Math.max(actualEndTime - latestValidEndTime, 0);
    }


    public static class TaxiStats
    {
        private double pickupDriveTime;
        private double dropoffDriveTime;
        private double pickupTime;
        private double dropoffTime;
        private double cruiseTime;
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
                + "MaxPickupDriveT\t" //
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
