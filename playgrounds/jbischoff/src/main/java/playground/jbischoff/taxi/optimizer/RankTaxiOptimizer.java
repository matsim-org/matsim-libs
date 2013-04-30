package playground.jbischoff.taxi.optimizer;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiEvaluator;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.schedule.TaxiDriveTask;
import playground.jbischoff.taxi.optimizer.BestVehicleFinder.BestVehicle;
import playground.jbischoff.taxi.rank.BackToRankTask;

public class RankTaxiOptimizer{

	       private VrpData data;
	       private BestVehicleFinder bestVehicleFinder;

	       private final TaxiEvaluator taxiEvaluator = new TaxiEvaluator();
	       
	       

	       public RankTaxiOptimizer(VrpData data, BestVehicleFinder bestVehicleFinder)
	       {
	           this.data = data;
	           this.bestVehicleFinder = bestVehicleFinder;
	       }


	       //@Override
	       public void optimize()
	       {
	           // find new request & dispatch the closest taxi
	           for (Request req : data.getRequests()) {
	               if (req.getStatus() == ReqStatus.UNPLANNED) {
	                   BestVehicle bestVehicle = (BestVehicle) bestVehicleFinder
	                           .findBestVehicle(req, data.getVehicles());
	                   assignToBestVehicle(bestVehicle, req);
	               }
	           }
	           
	       }


	       protected void assignToBestVehicle(BestVehicle best, Request req)
	       {
	           Vehicle bestVeh = best.getVehicle();
	           Schedule bestSched = bestVeh.getSchedule();
	           int bestDepartureTime = best.getDepartureTime();
	           int bestArrivalTime = best.getArrivalTime();
	           Arc bestArc = best.getArc();

	           if (!bestSched.getStatus().isUnplanned()) {// PLANNED or STARTED
	               Task lastTask = Schedules.getLastTask(bestSched);

	               if (lastTask.getType() != TaskType.WAIT) {
	                   // if DRIVE:
	                   // TODO: Normally, when the last task is DRIVE then the vehicle is
	                   // returning to its depot;
	                   // but in case of this optimizer - vehicle never returns to its depot

	                   throw new IllegalStateException();
	               }

	               switch (lastTask.getStatus()) {
	                   case PLANNED:
	                       if (lastTask.getBeginTime() == bestDepartureTime) { // waiting for 0 seconds!!!
	                           bestSched.removeLastPlannedTask();// remove WaitTask
	                       }
	                       else {// means: lastTask.getBeginTime() < best.departureTime
	                           ((WaitTask)lastTask).setEndTime(bestDepartureTime);// shortening the WAIT
	                                                                              // task
	                       }
	                       break;

	                   case STARTED:
	                       ((WaitTask)lastTask).setEndTime(bestDepartureTime);// shortening the WAIT task
	                       break;

	                   case PERFORMED:
	                   default:
	                       throw new IllegalStateException();
	               }

	           }

	           Vertex reqFromVertex = req.getFromVertex();
	           Vertex reqToVertex = req.getToVertex();

	           if (bestArc.getFromVertex() != reqFromVertex) {// not a loop
	               bestSched.addTask(new TaxiDriveTask(bestDepartureTime, bestArrivalTime, bestArc, req));
	           }

	           int endServeTime = bestArrivalTime + req.getDuration();
	           bestSched
	                   .addTask(new ServeTaskImpl(bestArrivalTime, endServeTime, req.getFromVertex(), req));

	           int startIdling = endServeTime;

	           if (reqFromVertex != reqToVertex) {
	               Arc arc = data.getVrpGraph().getArc(reqFromVertex, reqToVertex);
	               startIdling = endServeTime + arc.getTimeOnDeparture(endServeTime);
	               bestSched.addTask(new TaxiDriveTask(endServeTime, startIdling, arc, req));
//	               System.out.println("scheduling delievery for  "+ bestVeh.getId());

	           }
	           else {
	               // Delivery cannot be skipped otherwise the passenger will never exit the taxi
	               throw new IllegalStateException("Unsupported!!!!!!");
	           }

	           // addWaitTime
	           int tEnd = Math.min(bestSched.getBeginTime() + bestVeh.getTimeLimit(), bestVeh.getT1());
	           int startWaiting = startIdling;
	           if (startIdling < tEnd) {
	        	   
	        	   
	        	   if (reqToVertex != bestVeh.getDepot().getVertex()){
	        		   Arc arc = data.getVrpGraph().getArc(reqToVertex, bestVeh.getDepot().getVertex());
	        		   startWaiting = startIdling + arc.getTimeOnDeparture(startIdling);
//	        		   System.out.println("scheduling back to rank for "+ bestVeh.getId());
	        		   bestSched.addTask(new BackToRankTask(startIdling,startWaiting,arc));
		               bestSched.addTask(new WaitTaskImpl(startWaiting, tEnd, bestVeh.getDepot().getVertex()));

	        	   }
	        	   else
	        	   {
	               bestSched.addTask(new WaitTaskImpl(startIdling, tEnd, reqToVertex));

	        	   }
	           }
	           else {
	               // just a hack to comply with the assumptions, i.e. lastTask is WAIT_TASK
	               bestSched.addTask(new WaitTaskImpl(startIdling, startIdling, reqToVertex));
	           }
	       }


	       @SuppressWarnings("unused")
	       private void debug(Vehicle veh)
	       {
	           if (veh.getId() == 1) {
	               Task currentTask = veh.getSchedule().getCurrentTask();

	               System.err.println(">>>>>>>>>>> " + currentTask + " : delay="
	                       + (data.getTime() - currentTask.getEndTime()));
	           }
	       }


	       public static final SummaryStatistics pickupDelayStats = new SummaryStatistics();
	       public static final SummaryStatistics pickupSpeedupStats = new SummaryStatistics();
	       public static final SummaryStatistics deliveryDelayStats = new SummaryStatistics();
	       public static final SummaryStatistics deliverySpeedupStats = new SummaryStatistics();
	       public static final SummaryStatistics waitDelayStats = new SummaryStatistics();
	       public static final SummaryStatistics waitSpeedupStats = new SummaryStatistics();
	       public static final SummaryStatistics serveDelayStats = new SummaryStatistics();
	       public static final SummaryStatistics serveSpeedupStats = new SummaryStatistics();


	       private static void updateStats(int delay, SummaryStatistics delayStats,
	               SummaryStatistics speedupStats)
	       {
	           if (delay > 0) {
	               delayStats.addValue(delay);
	           }
	           else {
	               speedupStats.addValue(-delay);
	           }
	       }


	       /**
	        * @param vrpVehicle
	        * @return {@code true} if something has been changed; otherwise {@code false}
	        */
	       public boolean updateSchedule(Vehicle vrpVehicle)
	       {
	           int time = data.getTime();

	           Schedule schedule = vrpVehicle.getSchedule();
	           Task currentTask = schedule.getCurrentTask();

	           // debug(vrpVehicle);
	           int delay = time - currentTask.getEndTime();

	           if (delay == 0) {
	               return false;
	           }

	           switch (currentTask.getType()) {
	               case DRIVE:
	            	   if (currentTask instanceof TaxiDriveTask){
	                   switch ( ((TaxiDriveTask)currentTask).getDriveType()) {
	                       case PICKUP:
	                           updateStats(delay, pickupDelayStats, pickupSpeedupStats);
	                           break;

	                       case DELIVERY:
	                           updateStats(delay, deliveryDelayStats, deliverySpeedupStats);
	                           break;

	                       default:
	                           throw new IllegalArgumentException();
	                   }}

	                   break;

	               case WAIT:
	                   updateStats(delay, waitDelayStats, waitSpeedupStats);

	                   break;

	               case SERVE:
	                   updateStats(delay, serveDelayStats, serveSpeedupStats);

	               default:
	                   throw new IllegalArgumentException();

	           }

	           currentTask.setEndTime(time);

	           // TODO the code below must work only iff request.getT0 <= time
	           //

	           // (1) if we were driving to pick up a customer (i.e. next task type is SERVE) .....
	           // Task nextTask = Schedules.getNextTask(currentTask);
	           //
	           // if (nextTask.getType() == TaskType.SERVE) {
	           //
	           // // (2) and we are ahead of schedule (happens only for call-ahead requests)
	           // if (nextTask.getBeginTime() > time) {
	           //
	           // ServeTask serveTask = (ServeTask)nextTask;
	           // // (3) than add WAIT task
	           // schedule.addTask(nextTask.getTaskIdx(),
	           // new WaitTaskImpl(time, nextTask.getBeginTime(), serveTask.getAtVertex()));
	           // }
	           //
	           // return true;
	           // }

	           // debug(vrpVehicle);

	           List<Task> tasks = schedule.getTasks();
	           int startIdx = currentTask.getTaskIdx() + 1;
	           int t = time;

	           Task lastTask = Schedules.getLastTask(schedule);

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

	                       if (task == lastTask) {
	                           task.setBeginTime(t);

	                           if (task.getEndTime() <= t) {// may happen if a previous task was delayed
	                               // I used to remove this WAIT_TASK, but now I keep it in the schedule:
	                               // schedule.removePlannedTask(task.getTaskIdx());
	                               task.setEndTime(t);
	                           }
	                       }
	                       else {
	                           // if this is not the last task then some other task must have been added
	                           // between at time <= t
	                           // THEREFORE: task.endTime() <= t, and so it can be removed
	                           schedule.removePlannedTask(task.getTaskIdx());
	                           i--;
	                       }

	                       break;
	                   }
	                   case DRIVE: {
	                       // cannot be shortened/lengthen, therefore must be moved forward/backward
	                       DriveTask dt = (DriveTask)task;

	                       task.setBeginTime(t);
	                       t += dt.getArc().getTimeOnDeparture(t);
	                       task.setEndTime(t);

	                       break;
	                   }
	                   case SERVE: {
	                       // cannot be shortened/lengthen, therefore must be moved forward/backward
	                       ServeTask st = (ServeTask)task;

	                       task.setBeginTime(t);
	                       t += st.getRequest().getDuration();
	                       task.setEndTime(t);

	                       break;
	                   }
	                   default:
	                       throw new IllegalStateException();
	               }
	           }

	           return true;
	       }
	   }