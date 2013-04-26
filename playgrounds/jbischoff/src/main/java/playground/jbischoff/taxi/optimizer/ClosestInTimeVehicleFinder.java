package playground.jbischoff.taxi.optimizer;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;


/**
 * Finds a vehicle that is expected to be first at the customer's location
 * 
 * @author michalm
 */
public class ClosestInTimeVehicleFinder
    implements BestVehicleFinder
{
    private static class ClosestInTimeVehicle
        implements BestVehicle
    {
        private Vehicle vehicle = null;
        private int departureTime;
        private int arrivalTime = Integer.MAX_VALUE;
        private Arc arc;


        @Override
        public Vehicle getVehicle()
        {
            return vehicle;
        }


        @Override
        public int getDepartureTime()
        {
            return departureTime;
        }


        @Override
        public int getArrivalTime()
        {
            return arrivalTime;
        }


        @Override
        public Arc getArc()
        {
            return arc;
        }
    }


    private VrpData data;


    public ClosestInTimeVehicleFinder(VrpData data)
    {
        this.data = data;
    }


    public BestVehicle findBestVehicle(Request req, List<Vehicle> vehicles)
    {
        ClosestInTimeVehicle best = new ClosestInTimeVehicle();

        for (Vehicle veh : vehicles) {
            updateBestVehicle(req, veh, best);
        }

        if (best.vehicle == null) {
            throw new RuntimeException("No available taxicabs!!!");
        }

        return best;
    }


    private void updateBestVehicle(Request req, Vehicle veh, ClosestInTimeVehicle currentBest)
    {
        Schedule sched = veh.getSchedule();
        int time = data.getTime();
        Vertex departVertex;
        int departTime;

        switch (sched.getStatus()) {
            case COMPLETED:
                return;

            case UNPLANNED:
                departVertex = veh.getDepot().getVertex();
                departTime = Math.max(veh.getT0(), time);
                break;

            case PLANNED:
            case STARTED:
                Task lastTask = Schedules.getLastTask(sched);

                if (lastTask.getType() != TaskType.WAIT) {
                    // if DRIVE:
                    //
                    // aVertex = ((DriveTask)lastTask).getToVertex();
                    // departTime = Math.max(lastTask.getEndTime(), time);
                    // break;
                    //
                    // TODO: Normally, when the last task is DRIVE then the vehicle is
                    // returning to its depot;
                    // but in case of this optimizer - vehicle never returns to its depot

                    throw new IllegalStateException();
                }

                departVertex = ((WaitTask)lastTask).getAtVertex();
                departTime = Math.max(lastTask.getBeginTime(), time);
                break;

            default:
                throw new IllegalStateException();
        }

        // consider CLOSING (T1) time windows of the vehicle
        if (time > Schedules.getActualT1(sched)) {
            return;
        }

        Arc arc = data.getVrpGraph().getArc(departVertex, req.getFromVertex());
        int arrivalTime = departTime + arc.getTimeOnDeparture(departTime);

        if (arrivalTime < currentBest.arrivalTime) {
            // TODO: in the future: add a check if the taxi time windows will be satisfied
            currentBest.vehicle = veh;
            currentBest.departureTime = departTime;
            currentBest.arrivalTime = arrivalTime;
            currentBest.arc = arc;
        }
    }


    @SuppressWarnings("unused")
    private static void debug(Vehicle veh)
    {
        Schedule sched = veh.getSchedule();
        ScheduleStatus status = sched.getStatus();

        String currentTaskId;

        switch (status) {
            case UNPLANNED:
                currentTaskId = "-/";
                break;

            case PLANNED:
                currentTaskId = "0/";
                break;

            case STARTED:
                currentTaskId = (sched.getCurrentTask().getTaskIdx() + 1) + "/";
                break;

            case COMPLETED:
                currentTaskId = "-/";
                break;

            default:
                throw new IllegalStateException("Unsupported state");
        }

        StringBuilder schedLine = new StringBuilder("Veh: " + veh.getName() + " : " + status + " ["
                + currentTaskId + sched.getTaskCount() + "]");
        Iterator<ServeTask> stIter = Schedules.createServeTaskIter(sched);

        while (stIter.hasNext()) {
            ServeTask st = stIter.next();
            schedLine.append("-").append(st.getRequest());
        }

        System.err.println(schedLine.toString());
    }

}
