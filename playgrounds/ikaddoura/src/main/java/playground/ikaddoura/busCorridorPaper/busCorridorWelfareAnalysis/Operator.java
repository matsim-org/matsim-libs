/* *********************************************************************** *
 * project: org.matsim.*
 * Provider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class Operator {
	private final static Logger log = Logger.getLogger(Operator.class);
	
	private final double COSTS_PER_VEH_HOUR = 33.; 	 // in AUD
	private final double OVERHEAD_PERCENTAGE = 1.21; // on top of direct operating costs

	private int capacity;
	private int numberOfBuses;
	private double costs;
	private final double costsPerVehicleDay;
	private final double costsPerVehicleKm;

	public Operator() {
		this.costsPerVehicleDay = 1.6064 * this.capacity + 22.622; // see linear regression analysis in "BusCostsEstimations.xls"
		log.info("costsPerVehicleDay (AUD): " + costsPerVehicleDay);
		this.costsPerVehicleKm = 0.006 * this.capacity + 0.513;    // see linear regression analysis in "BusCostsEstimations.xls"
		log.info("CostsPerVehicleKm (AUD): " + costsPerVehicleKm);
	}

	public void calculateScore(OperatorUserAnalysis analysis) {
		log.info("Vehicle-km: " + analysis.getVehicleKm());
		log.info("Veh-Time: " + Time.writeTime(analysis.getVehicleHours() * 60 * 60, Time.TIMEFORMAT_HHMMSS));
		this.costs = (analysis.getNumberOfBusesFromEvents() * costsPerVehicleDay) + ((analysis.getVehicleKm() * costsPerVehicleKm) + (analysis.getVehicleHours() * COSTS_PER_VEH_HOUR)) * OVERHEAD_PERCENTAGE;
	}


	public double getCosts() {
		return costs;
	}
	
	public void setParametersForExtIteration(int capacity, int numberOfBuses) {
		this.capacity = capacity;
		this.numberOfBuses = numberOfBuses;
	}
}