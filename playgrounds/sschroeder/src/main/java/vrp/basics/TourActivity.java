package vrp.basics;

import vrp.api.Customer;
import vrp.api.Node;

/**
 * 
 * @author stefan schroeder
 *
 */

public class TourActivity {
	
	static class ActivityRelation {
		private TourActivity relatedActivity;

		public ActivityRelation(TourActivity relatedActivity) {
			super();
			this.relatedActivity = relatedActivity;
		}

		public TourActivity getRelatedActivity() {
			return relatedActivity;
		}
	}
	
	private Customer customer;

	private double practical_earliestArrivalTime = 0.0;
	
	private double practical_latestArrivalTime = Double.MAX_VALUE;
	
	private Integer currentLoad = null; //after this activity has occured
	
	public TourActivity(Customer customer) {
		super();
		this.customer = customer;
	}

	public int getCurrentLoad() {
		return currentLoad;
	}

	public void setCurrentLoad(int currentLoad) {
		this.currentLoad = currentLoad;
	}
	
	public Node getLocation() {
		return customer.getLocation();
	}
	
	public Customer getCustomer(){
		return customer;
	}
	
	
	public double getServiceTime(){
		return customer.getServiceTime();
	}
	
	public void setEarliestArrTime(double early){
		practical_earliestArrivalTime = early;
	}
	
	public void setTimeWindow(double start, double end){
		practical_earliestArrivalTime = start;
		practical_latestArrivalTime = end;
	}
	
	public double getEarliestArrTime(){
		return practical_earliestArrivalTime;
	}
	
	public double getLatestArrTime(){
		return practical_latestArrivalTime;
	}
	
	public void setLatestArrTime(double late){
		practical_latestArrivalTime = late;
	}
	
	public boolean hasTimeWindowConflict(){
		if(practical_earliestArrivalTime > practical_latestArrivalTime){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[customer=" + customer.getId() + "][currentLoad="+currentLoad+"][theoTimeWindow=" + customer.getTheoreticalTimeWindow() + 
			"][practTimeWindow=[start=" + practical_earliestArrivalTime  + "][end=" + practical_latestArrivalTime + "]]";
	}
}
