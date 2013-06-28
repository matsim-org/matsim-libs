///* *********************************************************************** *
// * project: org.matsim.*
// * Provider.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// * 
// */
//package playground.ikaddoura.optimization.operatorProfitModel;
//
//import org.apache.log4j.Logger;
//import org.matsim.core.utils.misc.Time;
//
//import playground.ikaddoura.optimization.OperatorUserAnalysis;
//
///**
// * @author Ihab
// *
// */
//public class Operator {
//	private final static Logger log = Logger.getLogger(Operator.class);
//	
//	private final double COSTS_PER_VEH_HOUR = 33.; 	 // in AUD
//	private final double OVERHEAD_PERCENTAGE = 1.21; // on top of direct operating costs
//
//	private int capacity;
//	private double costs;
//
//	public void calculateCosts(OperatorUserAnalysis analysis) {
//		
//		if (capacity > 120) {
//			log.warn("Capacity is unrealistic high. Expects a value below 120 pax/veh." +
//					" Can't garantee right calculation of operator costs per vehicle day and operator costs per vehicle-km." +
//					" Setting capacity for cost calculations to 120 pax/veh.");
//			
//			this.capacity = 120;
//		}
//		double costsPerVehicleDay = 1.6064 * this.capacity + 22.622; // see linear regression analysis in "BusCostsEstimations.xls"
//		double costsPerVehicleKm = 0.006 * this.capacity + 0.513;    // see linear regression analysis in "BusCostsEstimations.xls"
//		
//		log.info("CostsPerVehicleKm (AUD): " + costsPerVehicleKm);
//		log.info("costsPerVehicleDay (AUD): " + costsPerVehicleDay);
//		log.info("Vehicle-km: " + analysis.getVehicleKm());
//		log.info("Veh-Time: " + Time.writeTime(analysis.getVehicleHours() * 3600, Time.TIMEFORMAT_HHMMSS));
//		log.info("Number of Buses from Events: " + analysis.getNumberOfBusesFromEvents());
//		
//		double capitalCosts = analysis.getNumberOfBusesFromEvents() * costsPerVehicleDay;
//		double kmCosts = analysis.getVehicleKm() * costsPerVehicleKm;
//		double hCosts = analysis.getVehicleHours() * COSTS_PER_VEH_HOUR;
//		
//		this.costs = capitalCosts + ((kmCosts + hCosts) * OVERHEAD_PERCENTAGE);
//		log.info("Operator Costs (AUD): "+this.costs);
//	}
//
//
//	public double getCosts() {
//		return costs;
//	}
//	
//	public void setParametersForExtIteration(int capacity) {
//		this.capacity = capacity;
//	}
//}