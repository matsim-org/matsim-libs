package vrp.algorithms.ruinAndRecreate.basics;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.api.Costs;
import vrp.basics.Delivery;
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
	
	public double getTourCost(){
		return tourCost;
	}

	public void update(Tour tour){
		double cost = 0.0;
		int loadAtDepot = getLoadAtDepot(tour);
		tour.getActivities().get(0).setCurrentLoad(loadAtDepot);
		for(int i=1;i<tour.getActivities().size();i++){
			cost += costs.getCost(tour.getActivities().get(i-1).getLocation(),tour.getActivities().get(i).getLocation());
			int loadAtCustomer = tour.getActivities().get(i-1).getCurrentLoad() + (int)tour.getActivities().get(i).getCustomer().getDemand();
			tour.getActivities().get(i).setCurrentLoad(loadAtCustomer);
		}
		int size = tour.getActivities().size();
		try{
			assertEqual(tour.getActivities().get(size-2).getCurrentLoad(),tour.getActivities().get(size-1).getCurrentLoad());
		}
		catch(IllegalStateException e){
			System.out.println("!!!!!!!!!!!!" + tour);
			System.exit(1);
		}
		tourCost = cost;
	}
	
	private void assertEqual(int currentLoad, int currentLoad2) {
		if(currentLoad == currentLoad2){
			return;
		}
		else{
			throw new IllegalStateException();
		}
		
	}

	private int getLoadAtDepot(Tour tour) {
		int loadAtDepot = 0;
		for(TourActivity tA : tour.getActivities()){
			if(tA instanceof Delivery){
				loadAtDepot += tA.getCustomer().getDemand();
			}
		}
		return loadAtDepot*-1;
	}

}
