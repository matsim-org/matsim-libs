/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.P2.helper;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

/**
 * Config group to configure p
 * 
 * @author aneumann
 *
 */
public class PConfigGroup extends Module{
	
	/**
	 * TODO [AN] This one has to be checked
	 */
	private static final long serialVersionUID = 4840713748058034511L;
	private static final Logger log = Logger.getLogger(PConfigGroup.class);
	
	// Tags
	
	public static final String GROUP_NAME = "p";
	
	private static final String MIN_X = "minX";
	private static final String MIN_Y = "minY";
	private static final String MAX_X = "maxX";
	private static final String MAX_Y = "maxY";
	private static final String NUMBER_OF_COOPERATIVES = "numberOfCooperatives";
	private static final String COST_PER_KILOMETER = "costPerKilometer";
	private static final String EARNINGS_PER_KILOMETER_AND_PASSENGER = "earningsPerKilometerAndPassenger";
	private static final String COST_PER_VEHICLE = "costPerVehicle";
	
	// Defaults
	private double minX = Double.MIN_VALUE;	
	private double minY = Double.MIN_VALUE;	
	private double maxX = Double.MAX_VALUE;
	private double maxY = Double.MAX_VALUE;
	private int numberOfCooperatives = 1;
	private double costPerKilometer = 0.30;
	private double earningsPerKilometerAndPassenger = 0.50;
	private double costPerVehicle= 1000;
	
	public PConfigGroup(){
		super(GROUP_NAME);
		log.info("Started...");
		log.warn("SerialVersionUID has to be checked. Current one is " + PConfigGroup.serialVersionUID);
	}
	
	// Setter
	
	@Override
	public void addParam(final String key, final String value) {
		if (MIN_X.equals(key)) {
			this.minX = Double.parseDouble(value);
		} else if (MIN_Y.equals(key)) {
			this.minY = Double.parseDouble(value);
		} else if (MAX_X.equals(key)) {
			this.maxX = Double.parseDouble(value);
		} else if (MAX_Y.equals(key)) {
			this.maxY = Double.parseDouble(value);
		} else if (NUMBER_OF_COOPERATIVES.equals(key)) {
			this.numberOfCooperatives = Integer.parseInt(value);
		} else if (COST_PER_KILOMETER.equals(key)){
			this.costPerKilometer = Double.parseDouble(value);
		} else if (EARNINGS_PER_KILOMETER_AND_PASSENGER.equals(key)){
			this.earningsPerKilometerAndPassenger = Double.parseDouble(value);
		} else if (COST_PER_VEHICLE.equals(key)){
			this.costPerVehicle = Double.parseDouble(value);
		}
	}
	
	// Getter
	
	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(MIN_X, Double.toString(this.minX));
		map.put(MIN_Y, Double.toString(this.minY));
		map.put(MAX_X, Double.toString(this.maxX));
		map.put(MAX_Y, Double.toString(this.maxY));
		map.put(NUMBER_OF_COOPERATIVES, Integer.toString(this.numberOfCooperatives));
		map.put(COST_PER_KILOMETER, Double.toString(this.costPerKilometer));
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, Double.toString(this.earningsPerKilometerAndPassenger));
		map.put(COST_PER_VEHICLE, Double.toString(costPerVehicle));
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(MIN_X, "min x coordinate for service area");
		map.put(MIN_Y, "min y coordinate for service area");
		map.put(MAX_X, "max x coordinate for service area");
		map.put(MAX_Y, "max y coordinate for service area");
		map.put(NUMBER_OF_COOPERATIVES, "number of cooperatives operating");
		map.put(COST_PER_KILOMETER, "cost per vehicle and kilometer travelled");
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, "earnings per passenger kilometer");
		map.put(COST_PER_VEHICLE, "cost to purchase or sell a vehicle");

		return map;
	}
	
	public double getMinX() {
		return this.minX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMaxY() {
		return this.maxY;
	}

	public int getNumberOfCooperatives() {
		return this.numberOfCooperatives;
	}
	
	public double getCostPerKilometer() {
		return costPerKilometer;
	}

	public double getEarningsPerKilometerAndPassenger() {
		return earningsPerKilometerAndPassenger;
	}
		
	public double getCostPerVehicle() {
		return costPerVehicle;
	}	
}