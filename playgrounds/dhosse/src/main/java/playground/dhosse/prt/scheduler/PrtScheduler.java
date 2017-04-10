package playground.dhosse.prt.scheduler;

import java.util.*;

import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.*;


public class PrtScheduler
    extends TaxiScheduler
{

    private final TaxiSchedulerParams params;


    public PrtScheduler(Network network, Fleet taxiData, MobsimTimer timer,
            TaxiSchedulerParams params, TravelTime travelTime, TravelDisutility travelDisutility)
    {
        super(null, network, taxiData, timer, params, travelTime, travelDisutility);
        this.params = params;
    }


    public void scheduleRequests(BestDispatchFinder.Dispatch<TaxiRequest> best,
            List<BestDispatchFinder.Dispatch<TaxiRequest>> requests)
    {

        if (best.destination.getStatus() != TaxiRequestStatus.UNPLANNED) {
            throw new IllegalStateException();
        }

        Schedule bestSched = best.vehicle.getSchedule();

        if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
            TaxiTask lastTask = (TaxiTask)Schedules.getLastTask(bestSched);// only WAIT

            if (lastTask.getTaxiTaskType().equals(TaxiTask.TaxiTaskType.PICKUP)) {
                appendRequestToExistingScheduleTasks(best, requests);
                return;
            }

            switch (lastTask.getStatus()) {
                case PLANNED:
                    if (lastTask.getBeginTime() == best.path.getDepartureTime()) { // waiting for 0 seconds!!!
                        bestSched.removeLastTask();// remove WaitTask
                    }
                    else {
                        // actually this WAIT task will not be performed
                        lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task

                    }
                    break;

                case STARTED:
                    lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task
                    break;

                case PERFORMED:
                default:
                    throw new IllegalStateException();
            }
        }

        List<TaxiRequest> req = new ArrayList<TaxiRequest>();

        for (BestDispatchFinder.Dispatch<TaxiRequest> p : requests) {
            req.add(p.destination);
        }

        bestSched.addTask(new NPersonsPickupDriveTask(best.path, req));

        double t3 = Math.max(best.path.getArrivalTime(), best.destination.getEarliestStartTime())
                + params.pickupDuration;
        bestSched.addTask(new NPersonsPickupStayTask(best.path.getArrivalTime(), t3, req));

        if (params.destinationKnown) {
            appendOccupiedDriveAndDropoff(bestSched);
            appendTasksAfterDropoff(best.vehicle);
        }

    }


    private void appendRequestToExistingScheduleTasks(BestDispatchFinder.Dispatch<TaxiRequest> best,
            List<BestDispatchFinder.Dispatch<TaxiRequest>> requests)
    {

        Schedule sched = best.vehicle.getSchedule();

        for (Task task : sched.getTasks()) {

            if (task instanceof NPersonsPickupStayTask) {
                for (BestDispatchFinder.Dispatch<TaxiRequest> vrp : requests) {
                    if (vrp.path.getDepartureTime() < task.getBeginTime()
                            && !task.getStatus().equals(TaskStatus.PERFORMED)) {
                        ((NPersonsPickupStayTask)task).appendRequest(vrp.destination,
                                this.params.pickupDuration);
                    }
                }
            }

        }

    }


    @Override
    protected void appendOccupiedDriveAndDropoff(Schedule schedule)
    {
        NPersonsPickupStayTask pickupStayTask = (NPersonsPickupStayTask)Schedules
                .getLastTask(schedule);

        // add DELIVERY after SERVE
        List<TaxiRequest> reqs = ((NPersonsPickupStayTask)pickupStayTask).getRequests();
        TaxiRequest req = ((NPersonsPickupStayTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new NPersonsDropoffDriveTask(path, reqs));

        double t4 = path.getArrivalTime();
        double t5 = t4 + pickupStayTask.getRequests().size() * params.dropoffDuration;
        schedule.addTask(new NPersonsDropoffStayTask(t4, t5, reqs));
    }


    @Override
    protected void appendTasksAfterDropoff(Vehicle vehicle)
    {
        NPersonsDropoffStayTask dropoffStayTask = (NPersonsDropoffStayTask)Schedules
                .getLastTask(vehicle.getSchedule());

        // addWaitTime at the end (even 0-second WAIT)
        double t5 = dropoffStayTask.getEndTime();
        double tEnd = Math.max(t5, vehicle.getServiceEndTime());
        Link link = dropoffStayTask.getLink();

        vehicle.getSchedule().addTask(new TaxiStayTask(t5, tEnd, link));
    }


    protected void scheduleRankReturn(Vehicle veh, double time, boolean charge, boolean home)
    {
        Schedule sched = (Schedule)veh.getSchedule();
        TaxiStayTask last = (TaxiStayTask)Schedules.getLastTask(veh.getSchedule());
        if (last.getStatus() != TaskStatus.STARTED)
            throw new IllegalStateException();

        last.setEndTime(time);
        Link currentLink = last.getLink();
        Link nearestRank = veh.getStartLink();

        VrpPathWithTravelData path = calcPath(currentLink, nearestRank, time);
        if (path.getArrivalTime() > veh.getServiceEndTime())
            return; // no rank return if vehicle is going out of service anyway
        sched.addTask(new TaxiEmptyDriveTask(path));
        sched.addTask(new TaxiStayTask(path.getArrivalTime(), veh.getServiceEndTime(), nearestRank));

    }
}
