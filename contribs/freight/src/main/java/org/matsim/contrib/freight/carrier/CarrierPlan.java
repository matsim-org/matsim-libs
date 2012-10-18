package org.matsim.contrib.freight.carrier;

import java.util.Collection;

/**
 * 
 * Ein CarrierPlan kann sinnvoll nur dem Carrier hinzugefügt werden, nach dessen
 * CarrierCapabilities und mit dessen Shipments er erzeugt worden ist. Trotzdem
 * hat er keinen expliziten Verweis auf seinen Carrier. Naja. Mal sehen.
 * 
 * @author michaz
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
