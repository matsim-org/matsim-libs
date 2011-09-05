package vrp.basics;

import vrp.api.Customer;

public class BreakActivity extends TourActivity{

	private double breakLength;
	
	private int currentLoad;
	
	public BreakActivity(Customer customer, double breakLenght, int currentLoad) {
		super(customer);
		this.breakLength = breakLenght;
		this.currentLoad = currentLoad;
		super.setTimeWindow(0.0, Double.MAX_VALUE);
	}
	
	public double getServiceTime(){
		return breakLength;
	}
	
	public int getCurrentLoad(){
		return currentLoad;
	}
	
	public String getType(){
		return "Break";
	}
}
