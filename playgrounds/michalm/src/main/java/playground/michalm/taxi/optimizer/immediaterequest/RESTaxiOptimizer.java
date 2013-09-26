package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.List;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.WaitTaskImpl;
import playground.michalm.taxi.optimizer.TaxiUtils;
import playground.michalm.taxi.optimizer.schedule.*;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask.TaxiDriveType;


public class RESTaxiOptimizer
    extends ImmediateRequestTaxiOptimizer
{
    private final VrpData data;
    private final boolean destinationKnown;
    private final TaxiOptimizationPolicy optimizationPolicy;


    public RESTaxiOptimizer(VrpData data, boolean destinationKnown, boolean minimizePickupTripTime,
            TaxiOptimizationPolicy optimizationPolicy)
    {
        super(data, destinationKnown, minimizePickupTripTime);
        this.data = data;
        this.destinationKnown = destinationKnown;
        this.optimizationPolicy = optimizationPolicy;
    }


    @Override
    protected boolean shouldOptimizeBeforeNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        if (!scheduleUpdated) {// no changes
            return false;
        }

        return optimizationPolicy.shouldOptimize(vehicle.getSchedule().getCurrentTask());
    }


    @Override
    protected boolean shouldOptimizeAfterNextTask(Vehicle vehicle, boolean scheduleUpdated)
    {
        return false;
    }


    @Override
    protected void optimize()
    {
        for (Vehicle veh : data.getVehicles()) {
            removePlannedRequests(veh.getSchedule());
        }

        super.optimize();
    }


    private void removePlannedRequests(Schedule schedule)
    {
        switch (schedule.getStatus()) {
            case STARTED:
                Task task = schedule.getCurrentTask();

                if (Schedules.isLastTask(task)) {
                    return;
                }

                int obligatoryTasks = 0;// remove all planned tasks

                switch (task.getType()) {
                    case SERVE:

                        if (destinationKnown) {
                            obligatoryTasks = 1;// keep DELIVERY
                            break;
                        }
                        else {
                            return;// this is the last task in the schedule
                        }

                    case DRIVE:
                        if ( ((TaxiDriveTask)task).getDriveType() == TaxiDriveType.PICKUP) {

                            if (destinationKnown) {
                                obligatoryTasks = 2;// keep SERVE + DELIVERY
                            }
                            else {
                                return;// this is the last but one task in the schedule
                            }
                        }

                        break;

                    case WAIT:
                        // this WAIT is not the last task, so it seems that it is delayed
                        // and there are some other planned task
                        if (!TaxiUtils.isCurrentTaskDelayed(schedule, data.getTime())) {
                            throw new IllegalStateException();//
                        }
                }

                removePlannedTasks(schedule, obligatoryTasks);

                int tEnd = Schedules.getActualT1(schedule);
                int scheduleEndTime = schedule.getEndTime();
                Vertex lastVertex = Schedules.getLastVertexInSchedule(schedule);

                if (scheduleEndTime < tEnd) {
                    schedule.addTask(new WaitTaskImpl(scheduleEndTime, tEnd, lastVertex));
                }
                else {
                    // may happen that the previous task ends after tEnd!!!!!!!!!!
                    // just a hack to comply with the assumptions, i.e. lastTask is WAIT_TASK
                    schedule.addTask(new WaitTaskImpl(scheduleEndTime, scheduleEndTime, lastVertex));
                }
                break;

            case UNPLANNED:
            case COMPLETED:
                break;

            case PLANNED:// at time 0 taxi agents should start WAIT (before first taxi call)
                // therefore PLANNED->STARTED happens at the very beginning of time step 0
            default:
                throw new IllegalStateException();
        }
    }


    private void removePlannedTasks(Schedule schedule, int obligatoryTasks)
    {
        List<Task> tasks = schedule.getTasks();
        int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + obligatoryTasks;

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            Task task = tasks.get(i);

            if (task.getType() == TaskType.SERVE) {
                Request req = ((ServeTask)task).getRequest();
                unplannedRequestQueue.add(req);
            }

            schedule.removePlannedTask(i);
        }
    }
}
