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

	private final Carrier carrier;
	private final Collection<ScheduledTour> scheduledTours;
	private Double score = null;

	private final Attributes attributes = new AttributesImpl();

	@Override
	public String toString() {
		return "carrierPlan=[carrierId=" + carrier.getId() + "][score=" + score + "][#tours=" + scheduledTours.size() + "]";
	}

	public CarrierPlan(final Carrier carrier, final Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;
		this.carrier = carrier;
	}

	public Carrier getCarrier() {
		return carrier;
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

	@Override
	public final Attributes getAttributes() {
		return this.attributes;
	}
}
