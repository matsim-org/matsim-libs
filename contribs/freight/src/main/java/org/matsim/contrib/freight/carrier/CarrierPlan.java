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
		return "carrierPlan=[carrierId=" + carrier.getId() + "][score=" + score + "][#tours=" + scheduledTours.size() + "]";
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
