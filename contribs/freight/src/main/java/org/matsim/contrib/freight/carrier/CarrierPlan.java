package org.matsim.contrib.freight.carrier;

import java.util.Collection;

/**
 * 
 * A specific plan of a carrier, and its score. 
 * 
 * @author mzilske, sschroeder
 * 
 */
public class CarrierPlan {

	private final Carrier carrier;
	
	private final Collection<ScheduledTour> scheduledTours;

	private Double score = null;

	public CarrierPlan(final Carrier carrier, final Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;
		this.carrier = carrier;
	}

	public Carrier getCarrier() {
		return carrier;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public Collection<ScheduledTour> getScheduledTours() {
		return scheduledTours;
	}

}
