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
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


public class TaxiStatsCalculator
{
    private final TaxiStats stats = new TaxiStats();


    public TaxiStatsCalculator(Iterable<? extends Vehicle> vehicles)
    {
        for (Vehicle v : vehicles) {
            calculateStatsImpl(v);
        }
    }


    public TaxiStats getStats()
    {
        return stats;
    }


    private void calculateStatsImpl(Vehicle vehicle)
    {
        Schedule<TaxiTask> schedule = TaxiSchedules.asTaxiSchedule(vehicle.getSchedule());

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            return;// do not evaluate - the vehicle is unused
        }

        if (schedule.getTaskCount() < 1) {
            throw new RuntimeException("count=0 ==> must be unplanned!");
        }

        for (TaxiTask t : schedule.getTasks()) {
            double time = t.getEndTime() - t.getBeginTime();

            switch (t.getTaxiTaskType()) {
                case DRIVE:
                    TaxiTaskType nextTaskType = TaxiSchedules.getNextTaxiTask(t).getTaxiTaskType();
                    (nextTaskType == TaxiTaskType.PICKUP ? //
                            stats.pickupDriveTimes : stats.otherDriveTimes).addValue(time);

                    break;

                case DRIVE_WITH_PASSENGER:
                    stats.driveWithPassengerTimes.addValue(time);
                    break;

                case PICKUP:
                    stats.pickupTimes.addValue(time);

                    Request req = ((TaxiPickupTask)t).getRequest();
                    double waitTime = Math.max(t.getBeginTime() - req.getT0(), 0);
                    stats.passengerWaitTimes.addValue(waitTime);

                    break;

                case DROPOFF:
                    stats.dropoffTimes.addValue(time);
                    break;

                case STAY:
                    stats.stayTimes.addValue(time);
            }
        }

        stats.taxiOverTimes.addValue(schedule.getEndTime() - schedule.getVehicle().getT1());
    }


    public static class TaxiStats
    {
        public final DescriptiveStatistics passengerWaitTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics pickupDriveTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics otherDriveTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics driveWithPassengerTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics pickupTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics dropoffTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics stayTimes = new DescriptiveStatistics();
        public final DescriptiveStatistics taxiOverTimes = new DescriptiveStatistics();

        public static final String HEADER = "PassengerWaitT\t" //
                + "MaxPassengerWaitT"//
                //
                + "PickupDriveT\t" //
                + "MaxPickupDriveT\t" //
                //
                + "OtherDriveT" //
                + "DriveWithPassengerT\t"//
                //
                + "PickupT\t" //
                + "DropoffT\t" //
                + "StayT\t" //
                //
                + "OverT";


        @Override
        public String toString()
        {
            return new StringBuilder()//
                    .append(passengerWaitTimes.getMean()).append('\t') //
                    .append(passengerWaitTimes.getMax()).append('\t') //
                    //
                    .append(passengerWaitTimes.getMean()).append('\t') //
                    .append(passengerWaitTimes.getMax()).append('\t') //
                    //
                    .append(otherDriveTimes.getMean()).append('\t') //
                    .append(driveWithPassengerTimes.getMean()).append('\t') //
                    //
                    .append(pickupTimes.getMean()).append('\t') //
                    .append(dropoffTimes.getMean()).append('\t') //
                    .append(stayTimes.getMean()).append('\t') //
                    //
                    .append(taxiOverTimes.getMean()).append('\t')//
                    .toString();
        }
    }
}
