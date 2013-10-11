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

package playground.michalm.taxi.optimizer;

import java.io.PrintWriter;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.taxi.schedule.*;


public class TaxiEvaluator
{
    public TaxiEvaluation evaluateVrp(VrpData data)
    {
        TaxiEvaluation evaluation = new TaxiEvaluation();

        for (Vehicle v : data.getVehicles()) {
            evaluateSchedule(data, v, evaluation);
        }

        return evaluation;
    }


    private void evaluateSchedule(VrpData data, Vehicle v, TaxiEvaluation eval)
    {
        Schedule s = v.getSchedule();
        if (s.getStatus().isUnplanned()) {
            return;// do not evaluate - the vehicle is unused
        }

        if (s.getTaskCount() < 1) {
            throw new RuntimeException("count=0 ==> must be unplanned!");
        }

        for (Task t : s.getTasks()) {
            TaxiTask tt = (TaxiTask)t;

            int time = tt.getEndTime() - tt.getBeginTime();

            switch (tt.getTaxiTaskType()) {
                case PICKUP_DRIVE:
                    eval.taxiPickupDriveTime += time;
                    break;

                case DROPOFF_DRIVE:
                    eval.taxiDropoffDriveTime += time;
                    break;

                case CRUISE_DRIVE:
                    eval.taxiCruiseTime += time;
                    break;

                case PICKUP_STAY:
                    eval.taxiPickupTime += time;

                    Request req = ((TaxiPickupStayTask)tt).getRequest();
                    int waitTime = t.getBeginTime() - req.getT0();
                    eval.passengerWaitTime += waitTime;

                    if (eval.maxPassengerWaitTime < waitTime) {
                        eval.maxPassengerWaitTime = waitTime;
                    }
                    break;

                case DROPOFF_STAY:
                    eval.taxiDropoffTime += time;
                    break;

                case WAIT_STAY:
                    eval.taxiWaitTime += time;
            }
        }

        int latestValidEndTime = Schedules.getActualT1(s);
        int actualEndTime = s.getEndTime();

        if (actualEndTime > latestValidEndTime) {
            eval.taxiOverTime += actualEndTime - latestValidEndTime;
        }
    }


    public static class TaxiEvaluation
    {
        public static final String HEADER = "PickupDriveT\t" //
                + "DeliveryDriveT\t"//
                + "PickupT\t" //
                + "DropoffT\t" //
                + "CruiseT\t" //
                + "WaitT\t" //
                + "OverT\t" //
                + "PassengerWaitT\t" //
                + "MaxPassengerWaitT";

        private int taxiPickupDriveTime;
        private int taxiDropoffDriveTime;
        private int taxiPickupTime;
        private int taxiDropoffTime;
        private int taxiCruiseTime;
        private int taxiWaitTime;
        private int taxiOverTime;
        private int passengerWaitTime;
        private int maxPassengerWaitTime;


        public int getTaxiPickupDriveTime()
        {
            return taxiPickupDriveTime;
        }


        public int getTaxiDropoffDriveTime()
        {
            return taxiDropoffDriveTime;
        }


        public int getTaxiPickupTime()
        {
            return taxiPickupTime;
        }


        public int getTaxiDropoffTime()
        {
            return taxiDropoffTime;
        }


        public int getTaxiCruiseTime()
        {
            return taxiCruiseTime;
        }


        public int getTaxiWaitTime()
        {
            return taxiWaitTime;
        }


        public int getTaxiOverTime()
        {
            return taxiOverTime;
        }


        public int getPassengerWaitTime()
        {
            return passengerWaitTime;
        }


        public int getMaxPassengerWaitTime()
        {
            return maxPassengerWaitTime;
        }


        @Override
        public String toString()
        {
            return new StringBuilder().append(taxiDropoffDriveTime).append('\t') //
                    .append(taxiPickupDriveTime).append('\t') //
                    .append(taxiPickupTime).append('\t') //
                    .append(taxiDropoffTime).append('\t') //
                    .append(taxiCruiseTime).append('\t') //
                    .append(taxiWaitTime).append('\t') //
                    .append(taxiOverTime).append('\t') //
                    .append(passengerWaitTime).append('\t') //
                    .append(maxPassengerWaitTime).toString();
        }


        public void print(PrintWriter pw)
        {
            pw.println(HEADER);
            pw.println(toString());
        }
    }
}
