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

public class TourActivityStatusUpdaterWithTWImpl implements TourActivityStatusUpdater{

	private Costs costs;
	
	private double tourCost;
	
	public TourActivityStatusUpdaterWithTWImpl(Costs costs) {
		super();
		this.costs = costs;
	}

	public void update(Tour tour) {
		updateTimeWindowsAndLoadsAtTourActivities(tour);
	}
	
	private void updateTimeWindowsAndLoadsAtTourActivities(Tour tour) {
		reset(tour);
		TourActivity nextCustomer = null;
		TourActivity lastCustomer = null;
		double costs = 0.0;
		int loadsAtDepot = getLoadAtDepot(tour);
		int nOfCustomers = tour.getActivities().size(); 
		int j=nOfCustomers-1;
		for(int i=0;i<nOfCustomers;i++){
			if(nextCustomer == null){
				nextCustomer = tour.getActivities().get(j);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(j);
				double late = Math.min(currentAct.getLatestArrTime(), nextCustomer.getLatestArrTime() - currentAct.getServiceTime() - getTime(currentAct,nextCustomer));
				currentAct.setLatestArrTime(late);
				nextCustomer = currentAct;
			}
			if(lastCustomer == null){
				lastCustomer = tour.getActivities().get(i);
				lastCustomer.setCurrentLoad(loadsAtDepot);
			}
			else{
				TourActivity currentAct = tour.getActivities().get(i);	
				double early = Math.max(currentAct.getEarliestArrTime(), lastCustomer.getEarliestArrTime() + lastCustomer.getServiceTime() + getTime(lastCustomer,currentAct));
				currentAct.setEarliestArrTime(early);
				int currentLoad = lastCustomer.getCurrentLoad() + (int)currentAct.getCustomer().getDemand();
				currentAct.setCurrentLoad(currentLoad);
				costs += this.costs.getCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.generalizedCosts += this.costs.getCost(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.distance += this.costs.getDistance(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				tour.costs.time  += this.costs.getTime(lastCustomer.getLocation(), currentAct.getCustomer().getLocation());
				lastCustomer = currentAct;
			}
			j--;
		}
		tourCost = costs;
	}
	
	private void reset(Tour tour) {
		tour.costs.generalizedCosts = 0.0;
		tour.costs.distance = 0.0;
		tour.costs.time = 0.0;
		
	}

	private double getTime(TourActivity act1, TourActivity act2) {
		return costs.getTime(act1.getLocation(), act2.getLocation());
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
