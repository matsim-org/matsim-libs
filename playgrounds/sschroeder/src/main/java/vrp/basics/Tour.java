package vrp.basics;

import java.util.LinkedList;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Tour {
	
	public static class Costs {
		public double time;
		public double distance;
		public double generalizedCosts;
	}
	
	private LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	private Costs costs = null;
	
	public LinkedList<TourActivity> getActivities() {
		return tourActivities;
	}
	
	@Override
	public String toString() {
		String tour = null;
		for(TourActivity c : tourActivities){
			if(tour == null){
				tour = "[" + c.getCustomer() + "]";
			}
			else{
				tour += "[" + c.getCustomer() + "]";
			}
		}
		return tour;
	}

	public Costs getCosts() {
		return costs;
	}

}
