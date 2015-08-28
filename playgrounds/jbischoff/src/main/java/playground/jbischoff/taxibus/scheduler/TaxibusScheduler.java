/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.scheduler.TaxibusTask.TaxibusTaskType;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPath;



public class TaxibusScheduler 
{
   
	private TaxibusSchedulerParams params;
    protected final MatsimVrpContext context;
    protected final VrpPathCalculator calculator;

    public TaxibusScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
            TaxibusSchedulerParams params)
    {
        this.context = context;
        this.calculator = calculator;
        this.params = params;

        for (Vehicle veh : context.getVrpData().getVehicles().values()) {
            Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>) veh.getSchedule();
            schedule.addTask(new TaxibusStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
        }
    }


    public TaxibusSchedulerParams getParams()
    {
        return params;
    }


    public boolean isIdle(Vehicle vehicle)
    {
        Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>) vehicle.getSchedule();
        if (context.getTime() >= vehicle.getT1() || schedule.getStatus() != ScheduleStatus.STARTED) {
            return false;
        }

        TaxibusTask currentTask = schedule.getCurrentTask();
        return Schedules.isLastTask(currentTask)
                && currentTask.getTaxibusTaskType() == TaxibusTaskType.STAY;
    }    
    

    public LinkTimePair getImmediateDiversionOrEarliestIdleness(Vehicle veh)
    {
        if (params.vehicleDiversion) {
            LinkTimePair diversion = getImmediateDiversion(veh);
            if (diversion != null) {
                return diversion;
            }
        }

        return getEarliestIdleness(veh);
    }


    public TreeSet<LinkTimePair> getFreeSlotsInUpcomingRidesAndEarliestIdle(Vehicle veh){
    	TreeSet<LinkTimePair> linkTimeSet = new TreeSet<>(new Comparator<LinkTimePair>() {

			@Override
			public int compare(LinkTimePair o1, LinkTimePair o2) {
				Double o1t = o1.time; 
				return o1t.compareTo(o2.time);
			}
		});
    	
    	double capacity = veh.getCapacity();
    	
        Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>) veh.getSchedule();
        for (int i = schedule.getCurrentTask().getTaskIdx()+1; i<Schedules.getLastTask(schedule).getTaskIdx();i++){
        	
        	if (i>=schedule.getTasks().size()) break;
        	
        	TaxibusTask task = schedule.getTasks().get(i);
        	
        	if (task instanceof TaxibusDriveWithPassengerTask){
        		 if (((TaxibusDriveWithPassengerTask) task).getRequests().size()<capacity){
        			 double time = Math.max(task.getBeginTime(), context.getTime());
        			 LinkTimePair ltp = new LinkTimePair(((TaxibusDriveWithPassengerTask) task).getPath().getFromLink(),time);
        			 linkTimeSet.add(ltp);
        		 }
        	}
        }
    	LinkTimePair earliestIdle = getEarliestIdleness(veh);
    	linkTimeSet.add(earliestIdle);
    	
    	return linkTimeSet;
    }
    
    
    public LinkTimePair getEarliestIdleness(Vehicle veh)
    {
        if (context.getTime() >= veh.getT1()) {// time window T1 exceeded
            return null;
        }

        Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>) veh.getSchedule();
        Link link;
        double time;

        switch (schedule.getStatus()) {
            case PLANNED:
            case STARTED:
                TaxibusTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxibusTaskType()) {
                    case STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), context.getTime());//TODO very optimistic!!!
                        return createValidLinkTimePair(link, time, veh);

                    case PICKUP:
                        if (!params.destinationKnown) {
                            return null;
                        }
                        //otherwise: IllegalStateException -- the schedule should and with WAIT

                    default:
                        throw new IllegalStateException();
                }

            case COMPLETED:
                return null;

            case UNPLANNED://there is always at least one WAIT task in a schedule
            default:
                throw new IllegalStateException();
        }
    }


    public LinkTimePair getImmediateDiversion(Vehicle veh)
    {
        if (!params.vehicleDiversion) {
            throw new RuntimeException("Diversion must be on");
        }

        Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>) veh.getSchedule();

        if (/*context.getTime() >= veh.getT1() ||*/schedule.getStatus() != ScheduleStatus.STARTED) {
            return null;
        }

        TaxibusTask currentTask = schedule.getCurrentTask();
        if (!Schedules.isLastTask(currentTask)
                || currentTask.getTaxibusTaskType() != TaxibusTaskType.DRIVE_EMPTY) {
            return null;
        }

        OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker)currentTask.getTaskTracker();
        return filterValidLinkTimePair(tracker.getDiversionPoint(), veh);
    }


    private LinkTimePair filterValidLinkTimePair(LinkTimePair pair, Vehicle veh)
    {
        return pair.time >= veh.getT1() ? null : pair;
    }


    private LinkTimePair createValidLinkTimePair(Link link, double time, Vehicle veh)
    {
        return time >= veh.getT1() ? null : new LinkTimePair(link, time);
    }


    //=========================================================================================

    public void scheduleRequest(TaxibusVehicleRequestPath best)
    {
        best.failIfAnyRequestNotUnplanned();

        Schedule<TaxibusTask> bestSched =  (Schedule<TaxibusTask>) best.vehicle.getSchedule();
        TaxibusTask lastTask  = Schedules.getLastTask(bestSched);
       
        if (lastTask.getTaxibusTaskType() == TaxibusTaskType.STAY) {
        	Iterator<VrpPathWithTravelData> iterator = best.path.iterator(); 
        	VrpPathWithTravelData path = iterator.next();
            scheduleDriveToFirstRequest((TaxibusStayTask)lastTask, bestSched, path);
            Set<TaxibusRequest> onBoard = new LinkedHashSet<>();
            TreeSet<TaxibusRequest> pickUpsForLink = best.getPickUpsForLink(path.getToLink());
			if (pickUpsForLink!=null){
            	schedulePickups(bestSched, path, onBoard, pickUpsForLink);
            	
            }
            else {
            	//it shouldnt be null for the first pickup
            	throw new IllegalStateException();
            }
			
		while (iterator.hasNext()){
        	 path = iterator.next();
        	TreeSet<TaxibusRequest> dropOffsForLink = best.getDropOffsForLink(path.getFromLink());
        	if (dropOffsForLink!=null){
        		double t4 = path.getDepartureTime();
        		scheduleDropOffs(bestSched,onBoard,dropOffsForLink,t4);
        	}
        	
        	scheduleDriveAlongPath(bestSched, path, onBoard);
        	
        	 pickUpsForLink = best.getPickUpsForLink(path.getToLink());
  			if (pickUpsForLink!=null){
              	schedulePickups(bestSched, path, onBoard, pickUpsForLink);
              	
              }
        	
        	
		}
		if (!onBoard.isEmpty()){
			throw new IllegalStateException();
			//we forgot a customer?
		}
			
            
        }
                
        else {
            throw new IllegalStateException();
        }

         appendTasksAfterDropoff(bestSched);
        
    }


	private void scheduleDropOffs(Schedule<TaxibusTask> bestSched, 
			Set<TaxibusRequest> onBoard, TreeSet<TaxibusRequest> dropOffsForLink, double t4) {
		for (TaxibusRequest req : dropOffsForLink){
			
		    bestSched.addTask(new TaxibusDropoffTask(t4, t4+params.dropoffDuration, req));
			if (!onBoard.remove(req)){
				throw new IllegalStateException("Dropoff without pickup.");
			};
		}		
	}


	private void schedulePickups(Schedule<TaxibusTask> bestSched, VrpPathWithTravelData path,
			Set<TaxibusRequest> onBoard, TreeSet<TaxibusRequest> pickUpsForLink) {
		for (TaxibusRequest req : pickUpsForLink){
			double t3 = Math.max(path.getArrivalTime(), req.getT0())
		    		+ params.pickupDuration;
		    bestSched.addTask(new TaxibusPickupTask(path.getArrivalTime(), t3, req));
			onBoard.add(req);
		}
	}


    


	


    private void scheduleDriveAlongPath(Schedule<TaxibusTask> bestSched, VrpPathWithTravelData path, Set<TaxibusRequest> onBoard) {
    	bestSched.addTask(new TaxibusDriveWithPassengerTask(onBoard, path));
	}


	private void scheduleDriveToFirstRequest(TaxibusStayTask lastTask, Schedule<TaxibusTask> bestSched, VrpPathWithTravelData path)
    {
        switch (lastTask.getStatus()) {
        	case STARTED:
        		// bus is already idle
        		lastTask.setEndTime(path.getDepartureTime());// shortening the WAIT task
        		break;
            
        	case PLANNED:
        		        		
                if (lastTask.getBeginTime() == path.getDepartureTime()) { // waiting for 0 seconds!!!
                    bestSched.removeLastTask();// remove WaitTask
                }
                else {
                    // actually this WAIT task will not be performed
                    lastTask.setEndTime(path.getDepartureTime());// shortening the WAIT task
                }
                break;


            case PERFORMED:
            default:
                throw new IllegalStateException();
        }

        if (path.getLinkCount() > 1) {
            bestSched.addTask(new TaxibusDriveTask(path));
        }
    }





    /**
     * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e.
     * beginTime and endTime) of all tasks in the schedule.
     */
    public void updateBeforeNextTask(Schedule<TaxibusTask> schedule)
    {
        // Assumption: there is no delay as long as the schedule has not been started (PLANNED)
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double endTime = context.getTime();
        TaxibusTask currentTask = schedule.getCurrentTask();

        updateTimelineImpl(schedule, endTime);

       
    }



//    protected void appendDropoffandStayTask(Schedule<TaxibusTask> schedule)
//    {
//    	TaxibusDriveWithPassengerTask lastTask =  (TaxibusDriveWithPassengerTask) Schedules.getLastTask(schedule);
//        double t4 = lastTask.getEndTime();
//        double t5 = t4 + params.dropoffDuration;
//        schedule.addTask(new TaxibusDropoffTask(t4, t5, lastTask.getRequests().first()));
//        appendStayTask(schedule);
//    }
//    

    protected void appendTasksAfterDropoff(Schedule<TaxibusTask> schedule)
    {
        appendStayTask(schedule);
    }

    protected void appendStayTask(Schedule<TaxibusTask> schedule)
    {
        double tBegin = schedule.getEndTime();
        double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());//even 0-second WAIT
        Link link = Schedules.getLastLinkInSchedule(schedule);
        schedule.addTask(new TaxibusStayTask(tBegin, tEnd, link));
    }


    public void updateTimeline(Schedule<TaxibusTask> schedule)
    {
        if (schedule.getStatus() != ScheduleStatus.STARTED) {
            return;
        }

        double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(),
                context.getTime());
        updateTimelineImpl(schedule, predictedEndTime);
    }


    private void updateTimelineImpl(Schedule<TaxibusTask> schedule, double newTaskEndTime)
    {
        Task currentTask = schedule.getCurrentTask();
        if (currentTask.getEndTime() == newTaskEndTime) {
            return;
        }

        currentTask.setEndTime(newTaskEndTime);

        List<TaxibusTask> tasks = schedule.getTasks();
        int startIdx = currentTask.getTaskIdx() + 1;
        double t = newTaskEndTime;

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxibusTask task = tasks.get(i);

            switch (task.getTaxibusTaskType()) {
                case STAY: {
                    if (i == tasks.size() - 1) {// last task
                        task.setBeginTime(t);

                        if (task.getEndTime() < t) {// may happen if the previous task is delayed
                            task.setEndTime(t);//do not remove this task!!! A taxi schedule should end with WAIT
                        }
                    }
                    else {
                        // if this is not the last task then some other task (e.g. DRIVE or PICKUP)
                        // must have been added at time submissionTime <= t
                        double endTime = task.getEndTime();
                        if (endTime <= t) {// may happen if the previous task is delayed
                            schedule.removeTask(task);
                            i--;
                        }
                        else {
                            task.setBeginTime(t);
                            t = endTime;
                        }
                    }

                    break;
                }

                case DRIVE_EMPTY:
                case DRIVE_WITH_PASSENGER: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask)task)
                            .getPath();
                    t += path.getTravelTime(); //TODO one may consider recalculation of SP!!!!
                    task.setEndTime(t);

                    break;
                }

                case PICKUP: {
                    task.setBeginTime(t);// t == taxi's arrival time
                    double t0 = ((TaxibusPickupTask)task).getRequest().getT0();// t0 == passenger's departure time
                    t = Math.max(t, t0) + params.pickupDuration; // the true pickup starts at max(t, t0)
                    task.setEndTime(t);

                    break;
                }
                case DROPOFF: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += params.dropoffDuration;
                    task.setEndTime(t);

                    break;
                }
            }
        }
    }


    //=========================================================================================

    private List<TaxibusRequest> removedRequests;


    /**
     * Awaiting == unpicked-up, i.e. requests with status PLANNED or TAXI_DISPATCHED See
     * {@link TaxiRequestStatus}
     */
    public List<TaxibusRequest> removeAwaitingRequestsFromAllSchedules()
    {
        removedRequests = new ArrayList<>();
        for (Vehicle veh : context.getVrpData().getVehicles().values()) {
            removeAwaitingRequestsImpl((Schedule<TaxibusTask>) veh.getSchedule());
        }

        return removedRequests;
    }


    public List<TaxibusRequest> removeAwaitingRequests(Schedule<TaxibusTask> schedule)
    {
        removedRequests = new ArrayList<>();
        removeAwaitingRequestsImpl(schedule);
        return removedRequests;
    }


    private void removeAwaitingRequestsImpl(Schedule<TaxibusTask> schedule)
    {
        switch (schedule.getStatus()) {
            case STARTED:
                Integer unremovableTasksCount = countUnremovablePlannedTasks(schedule);
                if (unremovableTasksCount == null) {
                    return;
                }

                int newLastTaskIdx = schedule.getCurrentTask().getTaskIdx() + unremovableTasksCount;
                removePlannedTasks(schedule, newLastTaskIdx);

                TaxibusTask lastTask = schedule.getTasks().get(newLastTaskIdx);
                double tBegin = schedule.getEndTime();
                double tEnd = Math.max(tBegin, schedule.getVehicle().getT1());

                switch (lastTask.getTaxibusTaskType()) {
                    case STAY:
                        lastTask.setEndTime(tEnd);
                        return;

                    case DROPOFF:
                        Link link = Schedules.getLastLinkInSchedule(schedule);
                        schedule.addTask(new TaxibusStayTask(tBegin, tEnd, link));
                        return;

                    case DRIVE_EMPTY:
                        if (!params.vehicleDiversion) {
                            throw new RuntimeException("Currently won't happen");
                        }

                        //diversion -- no STAY afterwards
                        return;

                    default:
                        throw new RuntimeException();
                }

            case PLANNED:
                removePlannedTasks(schedule, -1);
                Vehicle veh = schedule.getVehicle();
                schedule.addTask(new TaxibusStayTask(veh.getT0(), veh.getT1(), veh.getStartLink()));
                return;

            case COMPLETED:
                return;

            case UNPLANNED:
                throw new IllegalStateException();
        }
    }


    private Integer countUnremovablePlannedTasks(Schedule<TaxibusTask> schedule)
    {
        TaxibusTask currentTask = schedule.getCurrentTask();
        switch (currentTask.getTaxibusTaskType()) {
            case PICKUP:
                return params.destinationKnown ? 2 : null;

            case DRIVE_WITH_PASSENGER:
                return 1;

            case DRIVE_EMPTY:
                if (params.vehicleDiversion) {
                    return 0;
                }
                TaxibusTask nextTask =  schedule.getTasks().get(currentTask.getTaskIdx()+1);
                if (nextTask.getTaxibusTaskType() == TaxibusTaskType.PICKUP) {
                    //if no diversion and driving to pick up sb then serve that request
                    return params.destinationKnown ? 3 : null;
                }

                //potentially: driving back to the rank (e.g. to charge batteries)
                throw new RuntimeException("Currently won't happen");

            case DROPOFF:
            case STAY:
                return 0;

            default:
                throw new RuntimeException();
        }
    }


    private void removePlannedTasks(Schedule<TaxibusTask> schedule, int newLastTaskIdx)
    {
        List<TaxibusTask> tasks = schedule.getTasks();

        for (int i = schedule.getTaskCount() - 1; i > newLastTaskIdx; i--) {
            TaxibusTask task = tasks.get(i);
            schedule.removeTask(task);

            if (task instanceof TaxibusTaskWithRequests) {
                TaxibusTaskWithRequests taskWithReq = (TaxibusTaskWithRequests)task;
                taskWithReq.removeFromAllRequests();

                if (task.getTaxibusTaskType() == TaxibusTaskType.PICKUP) {
                    removedRequests.addAll(taskWithReq.getRequests());
                }
            }
        }
    }


	public void stopAllAimlessDriveTasks() {
		// TODO Auto-generated method stub
		
	}
}
