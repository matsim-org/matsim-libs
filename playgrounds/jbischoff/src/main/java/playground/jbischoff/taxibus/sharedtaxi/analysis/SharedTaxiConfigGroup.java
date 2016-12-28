/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.jbischoff.taxibus.sharedtaxi.analysis;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SharedTaxiConfigGroup extends ReflectiveConfigGroup{
	
	
	
	private static final String GROUP_NAME = "sharedTaxi";
	private static final String SCORERIDES = "scoreRides";
	private static final String DISCOUNTFORSHAREDTRIP = "SharedTripDiscount";
	private static final String TAXIFAREPERHOUR = "NonSharedTaxiFarePerHour";

	private double taxiFare = 0.0;
	private double discountForSharing = 0.4;
	private boolean scoreRides = false;
	
	public static SharedTaxiConfigGroup get(Config config) {
		return (SharedTaxiConfigGroup) config.getModule(GROUP_NAME);
	}
	
	public SharedTaxiConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(TAXIFAREPERHOUR)
	public double getHourlyTaxiFare() {
		return taxiFare;
	}
	@StringSetter (TAXIFAREPERHOUR)
	public void setHourlyTaxiFare(double taxiFare) {
		this.taxiFare = taxiFare;
	}
	@StringGetter(DISCOUNTFORSHAREDTRIP)
	public double getDiscountForSharing() {
		return discountForSharing;
	}
	@StringSetter(DISCOUNTFORSHAREDTRIP)
	public void setDiscountForSharing(double discountForSharing) {
		this.discountForSharing = discountForSharing;
	}
	
	@StringGetter(SCORERIDES)
	public boolean isScoreRides() {
		return scoreRides;
	}

	@StringSetter(SCORERIDES)
	public void setScoreRides(boolean scoreRides) {
		this.scoreRides = scoreRides;
	}
	
	
}
