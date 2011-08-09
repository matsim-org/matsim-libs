package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.api.Costs;
import vrp.basics.DepotDelivery;
import vrp.basics.Tour;
import vrp.basics.TourActivity;

/**
 * 
 * @author stefan schroeder
 *
 */

public class TourActivityStatusUpdaterImpl implements TourActivityStatusUpdater{
	
	private Costs costs;
	
	private double tourCost;
	
	public TourActivityStatusUpdaterImpl(Costs costs) {
		super();
		this.costs = costs;
	}
	
	public void update(Tour tour){
		reset(tour);
		double cost = 0.0;
		int loadAtDepot = getLoadAtDepot(tour);
		tour.getActivities().get(0).setCurrentLoad(loadAtDepot);
		for(int i=1;i<tour.getActivities().size();i++){
			TourActivity fromAct = tour.getActivities().get(i-1);
			TourActivity toAct = tour.getActivities().get(i);
			cost += costs.getCost(fromAct.getLocation(),toAct.getLocation());
			tour.costs.generalizedCosts += costs.getCost(fromAct.getLocation(),toAct.getLocation());
			tour.costs.distance += costs.getDistance(fromAct.getLocation(),toAct.getLocation());
			tour.costs.time  += costs.getTime(fromAct.getLocation(),toAct.getLocation());
			int loadAtCustomer = fromAct.getCurrentLoad() + (int)toAct.getCustomer().getDemand();
			toAct.setCurrentLoad(loadAtCustomer);
		}
		int size = tour.getActivities().size();
		assertEqual(tour.getActivities().get(size-2).getCurrentLoad(),tour.getActivities().get(size-1).getCurrentLoad());
		tourCost = cost;
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.time = 0.0;
		tour.costs.distance = 0.0;
	}

	private void assertEqual(int currentLoad, int currentLoad2) {
		if(currentLoad == currentLoad2){
			return;
		}
		else{
			throw new IllegalStateException("currentLoad of second-last activity must be equal to currentLoad of last activity");
		}
		
	}

	private int getLoadAtDepot(Tour tour) {
		int loadAtDepot = 0;
		for(TourActivity tA : tour.getActivities()){
			if(tA instanceof DepotDelivery){
				loadAtDepot += tA.getCustomer().getDemand();
			}
		}
		return loadAtDepot*-1;
	}

}
