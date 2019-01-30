package org.matsim.contrib.freight.carrier;

import java.util.Collection;
import org.matsim.api.core.v01.population.BasicPlan;

/**
 * 
 * A specific plan of a carrier, and its score. 
 * 
 * @author mzilske, sschroeder
 * 
 */
public class CarrierPlan implements BasicPlan {

	private final Carrier carrier;
	
	private final Collection<ScheduledTour> scheduledTours;

	private Double score = null;

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder(  ) ;
		strb.append( "[carrierId=" ).append( carrier.getId() ).append("; score=").append( score ).append( "; tours=") ;
		for( ScheduledTour tour : scheduledTours ){
			strb.append( tour.toString() ) ;
		}
		strb.append( "]" ) ;
		return strb.toString() ;
	}

	public CarrierPlan(final Carrier carrier, final Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;
		this.carrier = carrier;
	}

	public Carrier getCarrier() {
		return carrier;
	}

	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	public Collection<ScheduledTour> getScheduledTours() {
		return scheduledTours;
	}

}
