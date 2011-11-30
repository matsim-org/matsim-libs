package playground.tnicolai.matsim4opus.utils.helperObjects;


public class AccessibilityStorage {

	private double travelTimeAccessibility = - Double.MAX_VALUE;
	private double travelCostAccessibility = - Double.MAX_VALUE;
	private double travelDistanceAccessibility = - Double.MAX_VALUE;

	/**
	 * constructor
	 * 
	 * @param tTimeAccessibility
	 * @param tCostAccessibility
	 * @param tDistanceAccessibility
	 */
	public AccessibilityStorage(double tTimeAccessibility, double tCostAccessibility, double tDistanceAccessibility){
		
		this.travelTimeAccessibility = tTimeAccessibility;
		this.travelCostAccessibility = tCostAccessibility;
		this.travelDistanceAccessibility = tDistanceAccessibility;
	}
	
	public double getTravelTimeAccessibility(){
		return this.travelTimeAccessibility;
	}
	public double getTravelCostAccessibility(){
		return this.travelCostAccessibility;
	}
	public double getTravelDistanceAccessibility(){
		return this.travelDistanceAccessibility;
	}

}
