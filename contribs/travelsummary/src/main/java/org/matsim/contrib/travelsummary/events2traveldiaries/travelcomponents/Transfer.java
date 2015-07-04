package org.matsim.contrib.travelsummary.events2traveldiaries.travelcomponents;

import java.util.LinkedList;


public class Transfer extends TravelComponent {
	public Journey journey;
	private Trip fromTrip;
	private Trip toTrip;
	private LinkedList<Wait> waits = new LinkedList<Wait>();
	private LinkedList<Walk> walks = new LinkedList<Walk>();

	public String toString() {
		return String.format(
				"TRANSFER start: %f end: %f walkTime: %f waitTime: %f \n",
				getStartTime(), getEndTime(), getWalkTime(), getWaitTime());
	}

	 public double getWaitTime() {
		try {
			double waitTime = 0;
			for (Wait w : getWaits()) {
				waitTime += w.getDuration();
			}
			return waitTime;
		} catch (NullPointerException e) {

		}
		return 0;
	}

	 public double getWalkTime() {
		try {
			double walkTime = 0;
			for (Walk w : getWalks()) {
				walkTime += w.getDuration();
			}
			return walkTime;
		} catch (NullPointerException e) {

		}
		return 0;
	}

	 public double getWalkDistance() {
		try {
			double walkDist = 0;
			for (Walk w : getWalks()) {
				walkDist += w.getDistance();
			}
			return walkDist;
		} catch (NullPointerException e) {

		}
		return 0;
	}

	public Trip getFromTrip() {
		return fromTrip;
	}

	public void setFromTrip(Trip fromTrip) {
		this.fromTrip = fromTrip;
	}

	public Trip getToTrip() {
		return toTrip;
	}

	public void setToTrip(Trip toTrip) {
		this.toTrip = toTrip;
	}

	public LinkedList<Walk> getWalks() {
		return walks;
	}

	public void setWalks(LinkedList<Walk> walks) {
		this.walks = walks;
	}

	public LinkedList<Wait> getWaits() {
		return waits;
	}

	public void setWaits(LinkedList<Wait> waits) {
		this.waits = waits;
	}
}