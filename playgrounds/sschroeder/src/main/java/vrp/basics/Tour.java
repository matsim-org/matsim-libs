package vrp.basics;

import java.util.LinkedList;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Tour {
	
	private LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

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
	
}
