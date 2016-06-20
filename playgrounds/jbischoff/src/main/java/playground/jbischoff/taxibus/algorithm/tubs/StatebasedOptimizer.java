package playground.jbischoff.taxibus.algorithm.tubs;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.jbischoff.taxibus.algorithm.optimizer.AbstractTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusDriveTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusDriveWithPassengerTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusPickupTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusStayTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusTask;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

public class StatebasedOptimizer extends AbstractTaxibusOptimizer {

	Id<Link> commonDestination;
	Vehicle veh;
	TaxibusDispatch dispatch = null;;
	StateSpace stateSpace;
	double currentValue;
	double currentSlack;
	MultiNodeDijkstra router;
	TaxibusConfigGroup tbcg;
	public StatebasedOptimizer(TaxibusOptimizerContext optimContext, boolean doUnscheduleAwaitingRequests,
			 StateSpace stateSpace, TaxibusConfigGroup tbcg) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.tbcg = tbcg;
		this.stateSpace = stateSpace;
		this.commonDestination = Id.createLinkId(tbcg.getDestinationLinkId());
		if (this.optimContext.vrpData.getVehicles().size() > 1) {
			throw new RuntimeException("optimizer only supports one bus");
		}
		veh = null;
		for (Vehicle allV : this.optimContext.vrpData.getVehicles().values()) {
			veh = allV;
		}
		router = new MultiNodeDijkstra(optimContext.scenario.getNetwork(),
                optimContext.travelDisutility, optimContext.travelTime, false);

	}

	@Override
	protected void scheduleUnplannedRequests() {
		Set<TaxibusRequest> handledRequests = new HashSet<>();
		Schedule<TaxibusTask> schedule = (Schedule<TaxibusTask>)veh.getSchedule();
		for (TaxibusRequest req : this.unplannedRequests) {
			if (!req.getToLink().getId().equals(this.commonDestination)) {
				throw new RuntimeException("optimizer only supports one single destination");
			}
			TaxibusPickupTask lastScheduledPickup = getLastPickupTask(schedule);
			Path lastPickupToNextPickup = null;
			double lastEndTime;
			Link lastLink;
			double oldSlack;
			if (lastScheduledPickup == null ){
				// this is a new dispatch
				TaxibusStayTask lastStayTask =   (TaxibusStayTask) schedule.getTasks().get(0);
				lastEndTime = optimContext.timer.getTimeOfDay();
				lastLink = lastStayTask.getLink();
				oldSlack = this.stateSpace.getCurrentLastArrivalTime(lastEndTime) - lastEndTime;
			} else{
			 lastEndTime = lastScheduledPickup.getEndTime();
			 lastLink = lastScheduledPickup.getLink();
			 TaxibusDriveWithPassengerTask alternativePassengerTask = (TaxibusDriveWithPassengerTask) schedule.getTasks().get(lastScheduledPickup.getTaskIdx()+1);
			 oldSlack = this.stateSpace.getCurrentLastArrivalTime(lastEndTime) - alternativePassengerTask.getEndTime();
			 }
			lastPickupToNextPickup = router.calcLeastCostPath(lastLink.getToNode(), req.getFromLink().getFromNode(), lastEndTime, null , null);
			double nextPickupEndTime = lastEndTime + lastPickupToNextPickup.travelTime + tbcg.getPickupDuration();
			Path nextPickupToDestination = router.calcLeastCostPath(req.getFromLink().getToNode(), req.getToLink().getFromNode(), nextPickupEndTime, null,null);
			double newEstimatedSlack = this.stateSpace.getCurrentLastArrivalTime(optimContext.timer.getTimeOfDay()) - (lastEndTime + nextPickupToDestination.travelTime);
			if (newEstimatedSlack>=0){
			double valueWithoutCustomer = this.stateSpace.getValue(optimContext.timer.getTimeOfDay(), oldSlack);
			
			double valueWithCustomer = 1+ this.stateSpace.getValue(optimContext.timer.getTimeOfDay(), newEstimatedSlack);
			if (valueWithCustomer>=valueWithoutCustomer)
			{
				VrpPathWithTravelData pickupPath = VrpPaths.createPath(lastLink, req.getFromLink(), lastEndTime, lastPickupToNextPickup, optimContext.travelTime);
				VrpPathWithTravelData dropoffPath = VrpPaths.createPath(req.getFromLink(), req.getToLink(),nextPickupEndTime,nextPickupToDestination, optimContext.travelTime);
				if (this.dispatch == null){
					
					this.dispatch = new TaxibusDispatch(veh, req, pickupPath);
					
					// new dispatch, i.e. first customer
				} else {
					removeAllTasksSinceLastPickup(schedule);
					
					this.dispatch.path.clear(); // otherwise we would add the same path several times 
					this.dispatch.addRequestAndPath(req, pickupPath);
				}
				this.dispatch.addPath(dropoffPath);
				// mitnehmen
				stateSpace.incBookingCounter();
				handledRequests.add(req);
				this.stateSpace.addExperiencedTimeSlack(optimContext.timer.getTimeOfDay(), newEstimatedSlack,dispatch.requests.size());
				optimContext.scheduler.scheduleRequest(dispatch);
				continue;
			}
			else {	
				int confirmations = 0;
				if (dispatch!=null) confirmations= dispatch.requests.size(); 
				this.stateSpace.addExperiencedTimeSlack(optimContext.timer.getTimeOfDay(), oldSlack, confirmations);
			}
			}
			req.setRejected(true);
			handledRequests.add(req);
		}
		
		
		unplannedRequests.removeAll(handledRequests);

	}

	/**
	 * @param schedule
	 */
	private void removeAllTasksSinceLastPickup(Schedule<TaxibusTask> schedule) {
		int idx = schedule.getTaskCount()-1;
		for (int i = idx; i>= 0; i--){
			TaxibusTask task = schedule.getTasks().get(i);
			if (task instanceof TaxibusPickupTask) {
				return;
			}
			else schedule.removeLastTask();
		}
		
	}

	/**
	 * @param schedule
	 * @return
	 */
	private TaxibusPickupTask getLastPickupTask(Schedule<TaxibusTask> schedule) {
		int idx = schedule.getTaskCount()-1;
		for (int i = idx; i>= 0; i--){
			TaxibusTask task = schedule.getTasks().get(i);
			if (task instanceof TaxibusPickupTask) return (TaxibusPickupTask) task;
		}
		return null;
	}
	
	private boolean isSimilarPathAlreadyInTour(Schedule<TaxibusTask> schedule, VrpPathWithTravelData path){
		int idx = schedule.getTaskCount()-1;
		for (int i = idx; i>= 0; i--){
			TaxibusTask task = schedule.getTasks().get(i);
			if (task instanceof TaxibusDriveTask){
				TaxibusDriveTask dtask = (TaxibusDriveTask) task;
				if ((dtask.getPath().getFromLink().equals(path.getFromLink())) &&(dtask.getPath().getToLink().equals(path.getToLink()))){
					return true;
				}
			} 
		}
		
		return false;
	}

}
