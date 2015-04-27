package playground.dhosse.prt.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.schedule.TaxiSchedules;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.schedule.TaxiStayTask;
import playground.michalm.taxi.scheduler.TaxiScheduler;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.vehreqpath.VehicleRequestPath;

public class PrtScheduler extends TaxiScheduler {

	private final TaxiSchedulerParams params;
	private final VrpPathCalculator calculator;
	
	public PrtScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
			TaxiSchedulerParams params) {
		super(context, calculator, params);
		this.params = params;
		this.calculator = calculator;
	}
	
	public void scheduleRequests(VehicleRequestPath best, List<VehicleRequestPath> requests) {
		
		if (best.request.getStatus() != TaxiRequestStatus.UNPLANNED) {
          throw new IllegalStateException();
      }

      Schedule<TaxiTask> bestSched = TaxiSchedules.asTaxiSchedule(best.vehicle.getSchedule());

      if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
          TaxiTask lastTask = Schedules.getLastTask(bestSched);// only WAIT
          
          if(lastTask.getTaxiTaskType().equals(TaxiTask.TaxiTaskType.PICKUP)){
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
      
      for(VehicleRequestPath p : requests){
    	  req.add(p.request);
      }

      bestSched.addTask(new NPersonsPickupDriveTask(best.path, req));

      double t3 = Math.max(best.path.getArrivalTime(), best.request.getT0())
              + params.pickupDuration;
      bestSched.addTask(new NPersonsPickupStayTask(best.path.getArrivalTime(), t3, req));

      if (params.destinationKnown) {
          appendDriveAndDropoffAfterPickup(bestSched);
          appendTasksAfterDropoff(bestSched);
      }
		
	}
	
	private void appendRequestToExistingScheduleTasks(VehicleRequestPath best,
			List<VehicleRequestPath> requests) {
		
		Schedule<TaxiTask> sched = TaxiSchedules.asTaxiSchedule(best.vehicle.getSchedule());
		
		for(TaxiTask task : sched.getTasks()){
			
			if(task instanceof NPersonsPickupStayTask){
				for(VehicleRequestPath vrp : requests){
					if(vrp.path.getDepartureTime() < task.getBeginTime() && !task.getStatus().equals(TaskStatus.PERFORMED)){
						((NPersonsPickupStayTask)task).appendRequest(vrp.request, this.params.pickupDuration);
					}
				}
			}
			
		}
		
	}

	@Override
	protected void appendDriveAndDropoffAfterPickup(Schedule<TaxiTask> schedule)
    {
        NPersonsPickupStayTask pickupStayTask = (NPersonsPickupStayTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        List<TaxiRequest> reqs = ((NPersonsPickupStayTask)pickupStayTask).getRequests();
        TaxiRequest req = ((NPersonsPickupStayTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calculator.calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new NPersonsDropoffDriveTask(path, reqs));

        double t4 = path.getArrivalTime();
        double t5 = t4 + pickupStayTask.getRequests().size()*params.dropoffDuration;
        schedule.addTask(new NPersonsDropoffStayTask(t4, t5, reqs));
    }

	@Override
	protected void appendTasksAfterDropoff(Schedule<TaxiTask> schedule)
    {
        NPersonsDropoffStayTask dropoffStayTask = (NPersonsDropoffStayTask)Schedules.getLastTask(schedule);

        // addWaitTime at the end (even 0-second WAIT)
        double t5 = dropoffStayTask.getEndTime();
        double tEnd = Math.max(t5, schedule.getVehicle().getT1());
        Link link = dropoffStayTask.getLink();

        schedule.addTask(new TaxiStayTask(t5, tEnd, link));
    }
	
}
