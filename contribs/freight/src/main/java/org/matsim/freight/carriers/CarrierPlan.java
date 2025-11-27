/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 *
 * A specific plan of a carrier, and its score.
 *
 * @author mzilske, sschroeder
 *
 */
public class CarrierPlan implements BasicPlan, Attributable {

	private Carrier carrier;
	private final Collection<ScheduledTour> scheduledTours;
	private Double score = null;

	private final Attributes attributes = new AttributesImpl();

	@Override
	public String toString() {
		return "carrierPlan=[carrierId=" + carrier.getId() + "][score=" + score + "][#tours=" + scheduledTours.size() + "]";
	}

	/**
	 * @deprecated Use the constructor without carrier argument.
	 * The carrier is set automatically when using Carrier.addPlan().
	 */
	@Deprecated(since = "Feb 2025", forRemoval = true)
	public CarrierPlan(final Carrier carrier, final Collection<ScheduledTour> scheduledTours) {
		this(scheduledTours);
		setCarrier(carrier);
	}

	/**
	 * Creates a new plan for a carrier based on its scheduled tours.
	 */
	public CarrierPlan(final Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;

	}

	public Carrier getCarrier() {
		return carrier;
	}

	/**
	 * Sets the reference to the carrier.
	 * This is done automatically if using Carrier.addPlan().
	 * <p
	 * <b>!! Make sure that the bidirectional reference is set correctly if you are using this method!! </b>
	 */
	public void setCarrier(final Carrier carrier) {
		this.carrier = carrier;
	}

	/**
	 * In future (starting in May'23) this is the score from the MATSim scoring function.
	 * The jsprit score is saved in attribute. Please use the method getJspritScore() to access it.
	 * @return the (MATSim) score
	 */
	@Override
	public Double getScore() {
		return score;
	}

	/**
	 * In future (starting in May'23) this is the score from the MATSim scoring function.
	 * The jsprit score is saved in attribute. Please use the method setJspritScore() to store it.
	 */
	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	/**
	 * Returns the score from the jsprit VRP solving. It is stored in an attribute of the CarrierPlan
	 * This is _not_ the score from the MATSim simulation.
	 * @return score from jsprit.
	 */
	public Double getJspritScore(){
		return CarriersUtils.getJspritScore(this);
	}

	/**
	 * Store the score from the jsprit VRP solving in an attribute of the CarrierPlan.
	 * This is _not_ the score from the MATSim simulation.
	 */
	public void setJspritScore(Double score){
		CarriersUtils.setJspritScore(this, score);
	}

	public Collection<ScheduledTour> getScheduledTours() {
		return scheduledTours;
	}

	/**
	 * Returns a specific scheduledTour
	 *
	 * @param tourId
	 * @return the scheduled tour with the tourId
	 */
	public Tour getScheduledTour(Id<Tour> tourId) {
		//This can be even shorter, once the scheduledTours is a Map and no longer a Collection. KMT Aug'25
		return scheduledTours.stream()
			.filter(st -> st.getTour().getId().equals(tourId))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Could not find scheduled tour for id " + tourId))
			.getTour();
	}

	@Override
	public final Attributes getAttributes() {
		return this.attributes;
	}
}
