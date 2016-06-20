package playground.jbischoff.taxibus.algorithm.tubs;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.jbischoff.taxibus.algorithm.optimizer.AbstractTaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizerContext;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusDriveWithPassengerTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusPickupTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusStayTask;
import playground.jbischoff.taxibus.algorithm.scheduler.TaxibusTask;
import playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

public class StatebasedOptimizer extends AbstractTaxibusOptimizer {

	Id<Link> commonDestination;
	Vehicle veh;
	StateSpace stateSpace;
	double currentValue;
	double currentSlack;
	MultiNodeDijkstra router;
	TaxibusConfigGroup tbcg;
	public StatebasedOptimizer(TaxibusOptimizerContext optimContext, boolean doUnscheduleAwaitingRequests,
			Id<Link> commonDestination, StateSpace statespace, TaxibusConfigGroup tbcg) {
		super(optimContext, doUnscheduleAwaitingRequests);
		this.tbcg = tbcg;
		this.commonDestination = commonDestination;
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
			int lastPickupTaskCount = schedule.getTaskCount()-3;
			Path lastPickupToNextPickup = null;
			double lastEndTime;
			Link lastLink;
			double oldSlack;
			if (lastPickupTaskCount < 0 ){
				TaxibusStayTask lastStayTask =   (TaxibusStayTask) schedule.getTasks().get(0);
				lastEndTime = optimContext.timer.getTimeOfDay();
				lastLink = lastStayTask.getLink();
				oldSlack = this.stateSpace.getCurrentLastArrivalTime(lastEndTime) - lastEndTime;
			} else{
			TaxibusPickupTask lastScheduledPickup = (TaxibusPickupTask) schedule.getTasks().get(lastPickupTaskCount);
			 lastEndTime = lastScheduledPickup.getEndTime();
			 lastLink = lastScheduledPickup.getLink();
			 TaxibusDriveWithPassengerTask alternativePassengerTask = (TaxibusDriveWithPassengerTask) schedule.getTasks().get(lastPickupTaskCount+1);
			 oldSlack = this.stateSpace.getCurrentLastArrivalTime(lastEndTime) - alternativePassengerTask.getEndTime();
			 }
			lastPickupToNextPickup = router.calcLeastCostPath(lastLink.getToNode(), req.getFromLink().getFromNode(), lastEndTime, null , null);
			double nextPickupEndTime = lastEndTime + lastPickupToNextPickup.travelTime + tbcg.getPickupDuration();
			Path nextPickupToDestination = router.calcLeastCostPath(req.getFromLink().getToNode(), req.getToLink().getFromNode(), nextPickupEndTime, null,null);
			double newEstimatedSlack = this.stateSpace.getCurrentLastArrivalTime(optimContext.timer.getTimeOfDay()) - (lastEndTime + nextPickupToDestination.travelTime);
			if (newEstimatedSlack>=0){
			double valueWithoutCustomer = this.stateSpace.getValue(optimContext.timer.getSimStartTime(), oldSlack);
			
			double valueWithCustomer = 1+ this.stateSpace.getValue(optimContext.timer.getSimStartTime(), newEstimatedSlack);
			if (valueWithCustomer>=valueWithoutCustomer)
			{
				// mitnehmen
				continue;
			}	
			}
			req.setRejected(true);
		}
		
		unplannedRequests.removeAll(handledRequests);

	}

}
