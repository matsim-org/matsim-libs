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
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.time = 0.0;
		tour.costs.distance = 0.0;
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
