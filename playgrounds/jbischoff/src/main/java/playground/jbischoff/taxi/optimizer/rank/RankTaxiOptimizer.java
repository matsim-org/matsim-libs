package playground.jbischoff.taxi.optimizer.rank;

import java.util.List;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.schedule.TaxiDriveTask;
import playground.jbischoff.taxi.rank.BackToRankTask;


/**
 * The main assumption: Requests are scheduled at the last request in the schedule (of a given
 * vehicle)
 * <p>
 * However, different strategies/policies may be used for:
 * <li>vehicle selection set (only idle, idle+delivering, all)
 * <li>rescheduling on/off (a reaction t changes in a schedule after updating it)
 * 
 * @author michalm
 */
public abstract class RankTaxiOptimizer
    extends AbstractTaxiOptimizer
{
    protected static class VehicleDrive
    {
        public static final VehicleDrive NO_VEHICLE_DRIVE_FOUND = new VehicleDrive(null, null,
                Integer.MAX_VALUE, Integer.MAX_VALUE);

        public final Vehicle vehicle;
        public final Arc arc;
        public final int t1;
        public final int t2;


        public VehicleDrive(Vehicle vehicle, Arc arc, int t1, int t2)
        {
            this.vehicle = vehicle;
            this.arc = arc;
            this.t1 = t1;
            this.t2 = t2;
        }
    }


    protected static class VertexTimePair
    {
        public static final VertexTimePair NO_VERTEX_TIME_PAIR_FOUND = new VertexTimePair(null,
                Integer.MAX_VALUE);

        public final Vertex vertex;
        public final int time;


        public VertexTimePair(Vertex vertex, int time)
        {
            this.vertex = vertex;
            this.time = time;
        }
    }


    private TaxiDelaySpeedupStats delaySpeedupStats;
    private final boolean destinationKnown;


    public RankTaxiOptimizer(VrpData data, boolean destinationKnown)
    {
        super(data);
        this.destinationKnown = destinationKnown;
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    protected void scheduleRequest(Request request)
    {
        VehicleDrive bestVehicle = findBestVehicle(request, data.getVehicles());

        if (bestVehicle != VehicleDrive.NO_VEHICLE_DRIVE_FOUND) {
            scheduleRequestImpl(bestVehicle, request);
        }
    }


    protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles)
    {
        int currentTime = data.getTime();
        VehicleDrive best = VehicleDrive.NO_VEHICLE_DRIVE_FOUND;

        for (Vehicle veh : vehicles) {
            Schedule schedule = veh.getSchedule();

            // COMPLETED or STARTED but delayed (time window T1 exceeded)
            if (schedule.getStatus() == ScheduleStatus.COMPLETED
                    || currentTime >= Schedules.getActualT1(schedule)) {
                // skip this vehicle
                continue;
            }

            // status = UNPLANNED/PLANNED/STARTED
            VertexTimePair departure = calculateDeparture(veh, currentTime);

            if (departure == VertexTimePair.NO_VERTEX_TIME_PAIR_FOUND) {
                continue;
            }

            Arc arc = data.getVrpGraph().getArc(departure.vertex, req.getFromVertex());
            int t2 = departure.time + arc.getTimeOnDeparture(departure.time);

            if (t2 < best.t2) {
                // TODO: in the future: add a check if the taxi time windows are satisfied
                best = new VehicleDrive(veh, arc, departure.time, t2);
            }
        }

        return best;
    }


    protected VertexTimePair calculateDeparture(Vehicle vehicle, int currentTime)
    {
        Schedule schedule = vehicle.getSchedule();
        Vertex vertex;
        int time;

        switch (schedule.getStatus()) {
            case UNPLANNED:
                vertex = vehicle.getDepot().getVertex();
                time = Math.max(vehicle.getT0(), currentTime);
                return new VertexTimePair(vertex, time);

            case PLANNED:
            case STARTED:
                Task lastTask = Schedules.getLastTask(vehicle.getSchedule());

                switch (lastTask.getType()) {
                    case WAIT:
                        vertex = ((WaitTask)lastTask).getAtVertex();
                        time = Math.max(lastTask.getBeginTime(), currentTime);
                        return new VertexTimePair(vertex, time);

                    case SERVE:
                        if (!destinationKnown) {
                            return VertexTimePair.NO_VERTEX_TIME_PAIR_FOUND;
                        }
                }
        }

        throw new IllegalStateException();
    }


    protected void scheduleRequestImpl(VehicleDrive best, Request req)
    {
        Schedule bestSched = best.vehicle.getSchedule();

        if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
            WaitTask lastTask = (WaitTask)Schedules.getLastTask(bestSched);// only WAIT

            switch (lastTask.getStatus()) {
                case PLANNED:
                    if (lastTask.getBeginTime() == best.t1) { // waiting for 0 seconds!!!
                        bestSched.removeLastPlannedTask();// remove WaitTask
                    }
                    else {
                        // TODO actually this WAIT task will not be performed
                        // so maybe we can remove it right now?

                        lastTask.setEndTime(best.t1);// shortening the WAIT task
                    }
                    break;

                case STARTED:
                    lastTask.setEndTime(best.t1);// shortening the WAIT task
                    break;

                case PERFORMED:
                default:
                    throw new IllegalStateException();
            }

        }

        Vertex reqFromVertex = req.getFromVertex();

        if (best.arc.getFromVertex() != reqFromVertex) {// not a loop
            bestSched.addTask(new TaxiDriveTask(best.t1, best.t2, best.arc, req));
        }

        int t3 = best.t2 + req.getDuration();
        bestSched.addTask(new ServeTaskImpl(best.t2, t3, req.getFromVertex(), req));

        if (destinationKnown) {
            appendDeliveryAndWaitTasksAfterServeTask(bestSched);
        }
    }


    @Override
    protected boolean updateBeforeNextTask(Vehicle vehicle)
    {
        int time = data.getTime();
        Schedule schedule = vehicle.getSchedule();
        Task currentTask = schedule.getCurrentTask();

        int delay = time - currentTask.getEndTime();

        if (delay != 0) {
            if (delaySpeedupStats != null) {// optionally, one may record delays
                delaySpeedupStats.updateStats(currentTask, delay);
            }

            currentTask.setEndTime(time);
            updatePlannedTasks(vehicle);
        }

        if (!destinationKnown) {
            if (currentTask.getType() == TaskType.SERVE) {
                appendDeliveryAndWaitTasksAfterServeTask(schedule);
                return true;
            }
        }

        return delay != 0;
    }


    protected void appendDeliveryAndWaitTasksAfterServeTask(Schedule schedule)
    {
        ServeTask serveTask = (ServeTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        Request req = ((ServeTask)serveTask).getRequest();
        Vertex reqFromVertex = req.getFromVertex();
        Vertex reqToVertex = req.getToVertex();
        int t3 = serveTask.getEndTime();

        if (reqFromVertex == reqToVertex) {
            // Delivery cannot be skipped otherwise the passenger will never exit the taxi
            throw new IllegalStateException("Unsupported!!!!!!");
        }

        Arc arc = data.getVrpGraph().getArc(reqFromVertex, reqToVertex);
        int startIdling = t3 + arc.getTimeOnDeparture(t3);
        schedule.addTask(new TaxiDriveTask(t3, startIdling, arc, req));
         // addWaitTime at the end (even 0-second WAIT)
        int tEnd = Math.max(startIdling, Schedules.getActualT1(schedule));
        int startWaiting = startIdling;
        if (reqToVertex != schedule.getVehicle().getDepot().getVertex()){
        	Arc darc = data.getVrpGraph().getArc(reqToVertex,schedule.getVehicle().getDepot().getVertex());
  		    startWaiting = startIdling + darc.getTimeOnDeparture(startIdling);
  		    schedule.addTask(new BackToRankTask(startIdling,startWaiting,darc));
  		    schedule.addTask(new WaitTaskImpl(startWaiting, tEnd, schedule.getVehicle().getDepot().getVertex()));
        	
        }
        else{
        schedule.addTask(new WaitTaskImpl(startIdling, tEnd, reqToVertex));
        }
    }


    /**
     * @param vehicle
     * @return {@code true} if something has been changed; otherwise {@code false}
     */
    protected void updatePlannedTasks(Vehicle vehicle)
    {
        Schedule schedule = vehicle.getSchedule();
        List<Task> tasks = schedule.getTasks();
        int startIdx = schedule.getCurrentTask().getTaskIdx() + 1;
        int t = data.getTime();

        for (int i = startIdx; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            switch (task.getType()) {
                case WAIT: {
                    // wait can be at the end of the schedule
                    //
                    // BUT:
                    //
                    // t1 - beginTime for WAIT
                    // t2 - arrival of new task
                    // t3 - updateSchedule() called -- end of the current task (STARTED)
                    // t4 - endTime for WAIT (according to some earlier plan)
                    //
                    // t1 <= t2 <= t3 < t4
                    //
                    // it may also be kept in situation where we have WAIT for [t1;t4) and at t2
                    // (t1 < t2 < t4) arrives new tasks are to be added but WAIT is still PLANNED
                    // (not STARTED) since there has been a delay in performing some previous tasks
                    // so we shorten WAIT to [t1;t2) and plan the newly arrived task at [t2; t2+x)
                    // but of course there will probably be a delay in performing this task, as
                    // we do not know when exactly the current task (one that is STARTED) will end
                    // (t3).
                    //
                    // Of course, the WAIT task will never be performed as we have now time t2
                    // and it is planned for [t1;t2); it will be removed on the nearest
                    // updateSchedule(), just like here:

                    if (i == tasks.size() - 1) {// last task
                        task.setBeginTime(t);

                        if (task.getEndTime() < t) {// may happen if a previous task was delayed
                            // I used to remove this WAIT_TASK, but now I keep it in the schedule:
                            // schedule.removePlannedTask(task.getTaskIdx());
                            task.setEndTime(t);
                        }
                    }
                    else {
                        // if this is not the last task then some other task must have been added
                        // at time <= t
                        // THEREFORE: task.endTime() <= t, and so it can be removed
                        schedule.removePlannedTask(task.getTaskIdx());
                        i--;
                    }

                    break;
                }
                case DRIVE: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += ((DriveTask)task).getArc().getTimeOnDeparture(t);
                    task.setEndTime(t);

                    break;
                }
                case SERVE: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += ((ServeTask)task).getRequest().getDuration();
                    task.setEndTime(t);

                    break;
                }
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
