package playground.pieter.travelsummary.travelcomponents;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.pieter.singapore.utils.events.EventsToPlanElementsSingapore;

public class TravelComponent {

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
