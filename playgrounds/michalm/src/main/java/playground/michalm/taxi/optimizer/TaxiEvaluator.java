package playground.michalm.taxi.optimizer;

import java.io.PrintWriter;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask;


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
            int time = t.getEndTime() - t.getBeginTime();

            switch (t.getType()) {
                case DRIVE:
                    TaxiDriveTask dt = (TaxiDriveTask)t;

                    switch (dt.getDriveType()) {
                        case DELIVERY:
                            eval.taxiDeliveryDriveTime += time;
                            break;

                        case PICKUP:
                            eval.taxiPickupDriveTime += time;
                            break;

                        case CRUISE:
                            eval.taxiCruiseTime += time;
                            break;

                        default:
                            throw new IllegalStateException();
                    }

                    break;

                case SERVE:
                    Request req = ((ServeTask)t).getRequest();

                    eval.taxiServiceTime += time;

                    int waitTime = t.getBeginTime() - req.getT0();
                    eval.passengerWaitTime += waitTime;

                    if (eval.maxPassengerWaitTime < waitTime) {
                        eval.maxPassengerWaitTime = waitTime;
                    }

                    break;

                case WAIT:
                    eval.taxiWaitTime += time;
                    break;

                default:
                    throw new IllegalStateException();
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
        public static final String HEADER = "PickupT\t" //
                + "DeliveryT\t"//
                + "ServiceT\t" //
                + "CruiseT\t" //
                + "WaitT\t" //
                + "OverT\t" //
                + "PassengerWaitT\t" //
                + "MaxPassengerWaitT";

        private int taxiPickupDriveTime;
        private int taxiDeliveryDriveTime;
        private int taxiServiceTime;
        private int taxiCruiseTime;
        private int taxiWaitTime;
        private int taxiOverTime;
        private int passengerWaitTime;
        private int maxPassengerWaitTime;


        public int getTaxiPickupDriveTime()
        {
            return taxiPickupDriveTime;
        }


        public int getTaxiDeliveryDriveTime()
        {
            return taxiDeliveryDriveTime;
        }


        public int getTaxiServiceTime()
        {
            return taxiServiceTime;
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
            return new StringBuilder().append(taxiDeliveryDriveTime).append('\t') //
                    .append(taxiPickupDriveTime).append('\t') //
                    .append(taxiServiceTime).append('\t') //
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
