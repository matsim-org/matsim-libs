package org.matsim.contrib.matsim4urbansim.utils.helperobjects;


public class AccessibilityStorage {

	private double congestedTravelTimeAccessibility = - Double.MAX_VALUE;
	private double freespeedTravelCostAccessibility = - Double.MAX_VALUE;
	private double walkTravelTimeAccessibility 		= - Double.MAX_VALUE;

	/**
	 * constructor
	 * 
	 * @param conTravelTimeAccessibility
	 * @param freeTravelTimeAccessibility
	 * @param walkTravelTimeAccessibility
	 */
	public AccessibilityStorage(double conTravelTimeAccessibility, double freeTravelTimeAccessibility, double walkTravelTimeAccessibility){
		
		this.congestedTravelTimeAccessibility = conTravelTimeAccessibility;
		this.freespeedTravelCostAccessibility = freeTravelTimeAccessibility;
		this.walkTravelTimeAccessibility = walkTravelTimeAccessibility;
	}
	
	public double getCongestedTravelTimeAccessibility(){
		return this.congestedTravelTimeAccessibility;
	}
	public double getFreespeedTravelCostAccessibility(){
		return this.freespeedTravelCostAccessibility;
	}
	public double getWalkTravelTimeAccessibility(){
		return this.walkTravelTimeAccessibility;
	}

}
