package org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents;

import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.router.TransitRouterConfig;


public class TravelComponent {

	public static double walkSpeed = new TransitRouterConfig(ConfigUtils.createConfig()).getBeelineWalkSpeed();
	public double getDuration() {
		return getEndTime() - getStartTime();
	}

	private static int id = 0; // for enumeration

	private double startTime;
	private double endTime = 30 * 3600;

	private int elementId;

	public TravelComponent() {
		elementId = id++;
	}

	public int getElementId() {
		return elementId;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}


	





	

	 


}
