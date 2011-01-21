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

package playground.andreas.bvgScoringFunction;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * Cnofig group to configure missing scoring paramteters of {@link BvgLegScoringFunction}
 * 
 * @author aneumann
 *
 */
public class BvgScoringFunctionConfigGroup extends Module{
	
	/**
	 * TODO [AN] This one has to be checked
	 */
	private static final long serialVersionUID = 4840713748058034511L;

	private static final Logger log = Logger.getLogger(BvgScoringFunctionConfigGroup.class);
	
	// Tags
	
	public static final String GROUP_NAME = "bvgScoring";
	
	private static final String OFFSET_CAR = "offsetCar";
	private static final String OFFSET_PT = "offsetPt";
	private static final String OFFSET_RIDE = "offsetRide";
	private static final String OFFSET_BIKE = "offsetBike";
	private static final String OFFSET_WALK = "offsetWalk";
	
	private static final String BETA_OFFSET_CAR = "betaOffsetCar";
	private static final String BETA_OFFSET_PT = "betaOffsetPt";
	private static final String BETA_OFFSET_RIDE = "betaOffsetRide";
	private static final String BETA_OFFSET_BIKE = "betaOffsetBike";
	private static final String BETA_OFFSET_WALK = "betaOffsetWalk";
	
	private static final String MONETARY_DISTANCE_COST_RATE_RIDE = "monetaryDistanceCostRateRide";
	private static final String MONETARY_DISTANCE_COST_RATE_BIKE = "monetaryDistanceCostRateBike";
	private static final String MONETARY_DISTANCE_COST_RATE_WALK = "monetaryDistanceCostRateWalk";
	
	// Defaults
	
	private double offsetCar = -1.0;
	private double offsetPt = -2.0;
	private double offsetRide = -1.0;
	private double offsetBike = -0.3;
	private double offsetWalk = -0.0;
	
	private double betaOffsetCar = 1.0;
	private double betaOffsetPt = 1.0;
	private double betaOffsetRide = 1.0;
	private double betaOffsetBike = 1.0;
	private double betaOffsetWalk = 1.0;
	
	private double monetaryDistanceCostRateRide = -0.0;
	private double monetaryDistanceCostRateBike = -0.0;
	private double monetaryDistanceCostRateWalk = -0.0;
	
	
	public BvgScoringFunctionConfigGroup() {
		super(GROUP_NAME);
		log.info("Started...");
		log.warn("SerialVersionUID has to be checked. Current one is " + BvgScoringFunctionConfigGroup.serialVersionUID);
	}
	
	public BvgScoringFunctionConfigGroup(Config config) {
		this();
		addParam(config);
	}
	
	// Setter
	
	private void addParam(Config config){
		this.offsetCar = Double.parseDouble(config.getParam(GROUP_NAME, OFFSET_CAR));
		this.offsetPt = Double.parseDouble(config.getParam(GROUP_NAME, OFFSET_PT));
		this.offsetRide = Double.parseDouble(config.getParam(GROUP_NAME, OFFSET_RIDE));
		this.offsetBike = Double.parseDouble(config.getParam(GROUP_NAME, OFFSET_BIKE));
		this.offsetWalk = Double.parseDouble(config.getParam(GROUP_NAME, OFFSET_WALK));
		
		this.betaOffsetCar = Double.parseDouble(config.getParam(GROUP_NAME, BETA_OFFSET_CAR));
		this.betaOffsetPt = Double.parseDouble(config.getParam(GROUP_NAME, BETA_OFFSET_PT));
		this.betaOffsetRide = Double.parseDouble(config.getParam(GROUP_NAME, BETA_OFFSET_RIDE));
		this.betaOffsetBike = Double.parseDouble(config.getParam(GROUP_NAME, BETA_OFFSET_BIKE));
		this.betaOffsetWalk = Double.parseDouble(config.getParam(GROUP_NAME, BETA_OFFSET_WALK));
		
		this.monetaryDistanceCostRateRide = Double.parseDouble(config.getParam(GROUP_NAME, MONETARY_DISTANCE_COST_RATE_RIDE));
		this.monetaryDistanceCostRateBike = Double.parseDouble(config.getParam(GROUP_NAME, MONETARY_DISTANCE_COST_RATE_BIKE));
		this.monetaryDistanceCostRateWalk = Double.parseDouble(config.getParam(GROUP_NAME, MONETARY_DISTANCE_COST_RATE_WALK));
	}
	
	@Override
	public void addParam(final String key, final String value) {
		
		if (OFFSET_CAR.equals(key)) {
			this.offsetCar = Double.parseDouble(value);
		} else if (OFFSET_PT.equals(key)) {
			this.offsetPt = Double.parseDouble(value);
		} else if (OFFSET_RIDE.equals(key)) {
			this.offsetRide = Double.parseDouble(value);
		} else if (OFFSET_BIKE.equals(key)) {
			this.offsetBike = Double.parseDouble(value);
		} else if (OFFSET_WALK.equals(key)) {
			this.offsetWalk = Double.parseDouble(value);
		} else if (BETA_OFFSET_CAR.equals(key)) {
			this.betaOffsetCar = Double.parseDouble(value);
		} else if (BETA_OFFSET_PT.equals(key)) {
			this.betaOffsetPt = Double.parseDouble(value);
		} else if (BETA_OFFSET_RIDE.equals(key)) {
			this.betaOffsetRide = Double.parseDouble(value);
		} else if (BETA_OFFSET_BIKE.equals(key)) {
			this.betaOffsetBike = Double.parseDouble(value);
		} else if (BETA_OFFSET_WALK.equals(key)) {
			this.betaOffsetWalk = Double.parseDouble(value);
		} else if (MONETARY_DISTANCE_COST_RATE_RIDE.equals(key)){
			this.monetaryDistanceCostRateRide = Double.parseDouble(value);
		} else if (MONETARY_DISTANCE_COST_RATE_BIKE.equals(key)){
			this.monetaryDistanceCostRateBike = Double.parseDouble(value);
		} else if (MONETARY_DISTANCE_COST_RATE_WALK.equals(key)){
			this.monetaryDistanceCostRateWalk = Double.parseDouble(value);
		}
		
	}
	
	// Getter
	
	public double getOffsetCar() {
		return this.offsetCar;
	}

	public double getOffsetPt() {
		return this.offsetPt;
	}

	public double getOffsetRide() {
		return this.offsetRide;
	}

	public double getOffsetBike() {
		return this.offsetBike;
	}

	public double getOffsetWalk() {
		return this.offsetWalk;
	}
	
	public double getBetaOffsetCar() {
		return this.betaOffsetCar;
	}

	public double getBetaOffsetPt() {
		return this.betaOffsetPt;
	}

	public double getBetaOffsetRide() {
		return this.betaOffsetRide;
	}

	public double getBetaOffsetBike() {
		return this.betaOffsetBike;
	}

	public double getBetaOffsetWalk() {
		return this.betaOffsetWalk;
	}

	public double getMonetaryDistanceCostRateRide() {
		return this.monetaryDistanceCostRateRide;
	}

	public double getMonetaryDistanceCostRateBike() {
		return this.monetaryDistanceCostRateBike;
	}

	public double getMonetaryDistanceCostRateWalk() {
		return this.monetaryDistanceCostRateWalk;
	}

	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(OFFSET_CAR, Double.toString(this.offsetCar));
		map.put(OFFSET_PT, Double.toString(this.offsetPt));
		map.put(OFFSET_RIDE, Double.toString(this.offsetRide));
		map.put(OFFSET_BIKE, Double.toString(this.offsetBike));
		map.put(OFFSET_WALK, Double.toString(this.offsetWalk));
		
		map.put(BETA_OFFSET_CAR, Double.toString(this.betaOffsetCar));
		map.put(BETA_OFFSET_PT, Double.toString(this.betaOffsetPt));
		map.put(BETA_OFFSET_RIDE, Double.toString(this.betaOffsetRide));
		map.put(BETA_OFFSET_BIKE, Double.toString(this.betaOffsetBike));
		map.put(BETA_OFFSET_WALK, Double.toString(this.betaOffsetWalk));
		
		map.put(MONETARY_DISTANCE_COST_RATE_RIDE, Double.toString(this.monetaryDistanceCostRateRide));
		map.put(MONETARY_DISTANCE_COST_RATE_BIKE, Double.toString(this.monetaryDistanceCostRateBike));
		map.put(MONETARY_DISTANCE_COST_RATE_WALK, Double.toString(this.monetaryDistanceCostRateWalk));
		
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		String offsetDefaultMessage = "[unit_of_money/leg] money needed in order to start a trip (leg)";
		
		map.put(OFFSET_CAR, offsetDefaultMessage + " in car mode");
		map.put(OFFSET_PT, offsetDefaultMessage + " in pt mode");
		map.put(OFFSET_RIDE, offsetDefaultMessage + " in ride mode");
		map.put(OFFSET_BIKE, offsetDefaultMessage + " in bike mode");
		map.put(OFFSET_WALK, offsetDefaultMessage + " in walk mode");
		
		String betaDefaultMessage = "beta scaling offset";
		
		map.put(BETA_OFFSET_CAR, betaDefaultMessage + " in car mode");
		map.put(BETA_OFFSET_PT, betaDefaultMessage + " in pt mode");
		map.put(BETA_OFFSET_RIDE, betaDefaultMessage + " in ride mode");
		map.put(BETA_OFFSET_BIKE, betaDefaultMessage + " in bike mode");
		map.put(BETA_OFFSET_WALK, betaDefaultMessage + " in walk mode");
		
		String monetaryDefaultMessage = "[unit_of_money/m] conversion of distance into money"; 
		
		map.put(MONETARY_DISTANCE_COST_RATE_RIDE, monetaryDefaultMessage + " in ride mode");
		map.put(MONETARY_DISTANCE_COST_RATE_BIKE, monetaryDefaultMessage + " in bike mode");
		map.put(MONETARY_DISTANCE_COST_RATE_WALK, monetaryDefaultMessage + " in walk mode");		

		return map;
	}
	
}
