/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.matsim4opus.config;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.experimental.ReflectiveModule;

public class AccessibilityParameterConfigModule extends ReflectiveModule{
	private static Logger log = Logger.getLogger( AccessibilityParameterConfigModule.class ) ;

	public static final String GROUP_NAME = "accessibilityParameter";
	
	// ===
	
	private double accessibilityDestinationSamplingRate;
	
	private static final String USING_LOGIT_SCALE_PARAMETER_FROM_MATSIM = "usingScaleParameterFromMATSim" ;
	private boolean usingLogitScaleParameterFromMATSim;
	
	private boolean usingCarParameterFromMATSim;
	private boolean usingBikeParameterFromMATSim;
	private boolean usingWalkParameterFromMATSim;
	private boolean usingPtParameterFromMATSim;
    
	private boolean usingRawSumsWithoutLn;
    
	private double logitScaleParameter;
    
	// ===
	
	static final String BETA_CAR_TRAVEL_TIME = "betaCarTravelTime";
	static final String BETA_CAR_TRAVEL_TIME_POWER2 = "betaCarTravelTimePower2";
	static final String BETA_CAR_LN_TRAVEL_TIME = "betaCarLnTravelTime";
	static final String BETA_CAR_TRAVEL_DISTANCE = "betaCarTravelDistance";
	static final String BETA_CAR_TRAVEL_DISTANCE_POWER2 = "betaCarTravelDistancePower2";
	static final String BETA_CAR_LN_TRAVEL_DISTANCE = "betaCarLnTravelDistance";
	static final String BETA_CAR_TRAVEL_COST = "betaCarTravelCost";
	static final String BETA_CAR_TRAVEL_COST_POWER2 = "betaCarTravelCostPower2";
	static final String BETA_CAR_LN_TRAVEL_COST = "betaCarLnTravelCost";
	
	private double betaCarTravelTime;
	private double betaCarTravelTimePower2;
	private double betaCarLnTravelTime;
	private double betaCarTravelDistance;
	private double betaCarTravelDistancePower2;
	private double betaCarLnTravelDistance;
	private double betaCarTravelMonetaryCost;
	private double betaCarTravelMonetaryCostPower2;
	private double betaCarLnTravelMonetaryCost;
	
	// ---
	
	private double betaBikeTravelTime;
	private double betaBikeTravelTimePower2;
	private double betaBikeLnTravelTime;
	private double betaBikeTravelDistance;
	private double betaBikeTravelDistancePower2;
	private double betaBikeLnTravelDistance;
	private double betaBikeTravelMonetaryCost;
	private double betaBikeTravelMonetaryCostPower2;
	private double betaBikeLnTravelMonetrayCost;

	private double betaWalkTravelTime;
	private double betaWalkTravelTimePower2;
	private double betaWalkLnTravelTime;
	private double betaWalkTravelDistance;
	private double betaWalkTravelDistancePower2;
	private double betaWalkLnTravelDistance;
	private double betaWalkTravelMonetaryCost;
	private double betaWalkTravelMonetrayCostPower2;
	private double betaWalkLnTravelMonetrayCost;
	
	private double betaPtTravelTime;
	private double betaPtTravelTimePower2;
	private double betaPtLnTravelTime;
	private double betaPtTravelDistance;
	private double betaPtTravelDistancePower2;
	private double betaPtLnTravelDistance;
	private double betaPtTravelMonetrayCost;
	private double betaPtTravelMonetrayCostPower2;
	private double betaPtLnTravelMonetrayCost;

	// parameter names in matsim4urbansimParameter module
	public static final String TIME_OF_DAY = "timeOfDay";

	static final String BETA_BIKE_TRAVEL_TIME = "betaBikeTravelTime";
	static final String BETA_BIKE_TRAVEL_TIME_POWER2 = "betaBikeTravelTimePower2";
	static final String BETA_BIKE_LN_TRAVEL_TIME = "betaBikeLnTravelTime";
	static final String BETA_BIKE_TRAVEL_DISTANCE = "betaBikeTravelDistance";
	static final String BETA_BIKE_TRAVEL_DISTANCE_POWER2 = "betaBikeTravelDistancePower2";
	static final String BETA_BIKE_LN_TRAVEL_DISTANCE = "betaBikeLnTravelDistance";
	static final String BETA_BIKE_TRAVEL_COST = "betaBikeTravelCost";
	static final String BETA_BIKE_TRAVEL_COST_POWER2 = "betaBikeTravelCostPower2";
	static final String BETA_BIKE_LN_TRAVEL_COST = "betaBikeLnTravelCost";
	
	public AccessibilityParameterConfigModule() {
		super(GROUP_NAME);
		// yyyyyy this class feels quite dangerous to me; one can have inconsistent entries between the Map and the typed values. kai, apr'13 
	}
	
	@Override
	public Map<String,String> getComments() {
		Map<String,String> map = new TreeMap<String,String>() ;
		
		// map.put(key,comment) ;
		
		return map ;
	}
	
	public double getAccessibilityDestinationSamplingRate(){
		return this.accessibilityDestinationSamplingRate;
	}
	
	public void setAccessibilityDestinationSamplingRate(double sampleRate){
		this.accessibilityDestinationSamplingRate = sampleRate;
	}

    public boolean usingLogitScaleParameterFromMATSim() {
        return usingLogitScaleParameterFromMATSim;
    }

    public void setUsingLogitScaleParameterFromMATSim(boolean value) {
        this.usingLogitScaleParameterFromMATSim = value;
    }

    public boolean usingCarParameterFromMATSim() {
        return usingCarParameterFromMATSim;
    }

    public void setUsingCarParameterFromMATSim(boolean value) {
        this.usingCarParameterFromMATSim = value;
    }
    
    public boolean usingBikeParameterFromMATSim() {
        return usingBikeParameterFromMATSim;
    }

    public void setUsingBikeParameterFromMATSim(boolean value) {
        this.usingBikeParameterFromMATSim = value;
    }
    
    public boolean usingWalkParameterFromMATSim() {
        return usingWalkParameterFromMATSim;
    }

    public void setUsingWalkParameterFromMATSim(boolean value) {
        this.usingWalkParameterFromMATSim = value;
    }
    
    public boolean usingPtParameterFromMATSim() {
        return usingPtParameterFromMATSim;
    }

    public void setUsingPtParameterFromMATSim(boolean value) {
        this.usingPtParameterFromMATSim = value;
    }

    public boolean usingRawSumsWithoutLn() {
        return usingRawSumsWithoutLn;
    }

    public void setUsingRawSumsWithoutLn(boolean value) {
        this.usingRawSumsWithoutLn = value;
    }

    public double getLogitScaleParameter() {
        return logitScaleParameter;
    }

    public void setLogitScaleParameter(double value) {
        this.logitScaleParameter = value;
    }
    // ===
    @StringGetter(BETA_CAR_TRAVEL_TIME)
    public double getBetaCarTravelTime() {
        return betaCarTravelTime;
    }
    @StringSetter(BETA_CAR_TRAVEL_TIME)
    public void setBetaCarTravelTime(double value) {
        this.betaCarTravelTime = value;
    }
    @StringGetter(BETA_CAR_TRAVEL_TIME_POWER2)
    public double getBetaCarTravelTimePower2() {
        return betaCarTravelTimePower2;
    }
    @StringSetter(BETA_CAR_TRAVEL_TIME_POWER2)
    public void setBetaCarTravelTimePower2(double value) {
        this.betaCarTravelTimePower2 = value;
    }
    @StringGetter(BETA_CAR_LN_TRAVEL_TIME)
    public double getBetaCarLnTravelTime() {
        return betaCarLnTravelTime;
    }
    @StringSetter(BETA_CAR_LN_TRAVEL_TIME)
    public void setBetaCarLnTravelTime(double value) {
        this.betaCarLnTravelTime = value;
    }
    @StringGetter(BETA_CAR_TRAVEL_DISTANCE)
    public double getBetaCarTravelDistance() {
        return betaCarTravelDistance;
    }
    @StringSetter(BETA_CAR_TRAVEL_DISTANCE)
    public void setBetaCarTravelDistance(double value) {
        this.betaCarTravelDistance = value;
    }
    @StringGetter(BETA_CAR_TRAVEL_DISTANCE_POWER2)
    public double getBetaCarTravelDistancePower2() {
        return betaCarTravelDistancePower2;
    }
    @StringSetter(BETA_CAR_TRAVEL_DISTANCE_POWER2)
    public void setBetaCarTravelDistancePower2(double value) {
        this.betaCarTravelDistancePower2 = value;
    }
    @StringGetter(BETA_CAR_LN_TRAVEL_DISTANCE)
    public double getBetaCarLnTravelDistance() {
        return betaCarLnTravelDistance;
    }
    @StringSetter(BETA_CAR_LN_TRAVEL_DISTANCE)
    public void setBetaCarLnTravelDistance(double value) {
        this.betaCarLnTravelDistance = value;
    }
    @StringGetter(BETA_CAR_TRAVEL_COST)
    public double getBetaCarTravelMonetaryCost() {
        return betaCarTravelMonetaryCost;
    }
    @StringSetter(BETA_CAR_TRAVEL_COST)
    public void setBetaCarTravelMonetaryCost(double value) {
        this.betaCarTravelMonetaryCost = value;
    }
    @StringGetter(BETA_CAR_TRAVEL_COST_POWER2)
    public double getBetaCarTravelMonetaryCostPower2() {
        return betaCarTravelMonetaryCostPower2;
    }
    @StringSetter(BETA_CAR_TRAVEL_COST_POWER2)
    public void setBetaCarTravelMonetaryCostPower2(double value) {
        this.betaCarTravelMonetaryCostPower2 = value;
    }
    @StringGetter(BETA_CAR_LN_TRAVEL_COST)
    public double getBetaCarLnTravelMonetaryCost() {
        return betaCarLnTravelMonetaryCost;
    }
    @StringSetter(BETA_CAR_LN_TRAVEL_COST)
    public void setBetaCarLnTravelMonetaryCost(double value) {
        this.betaCarLnTravelMonetaryCost = value;
    }
    // ===
    @StringGetter(BETA_BIKE_TRAVEL_TIME)
    public double getBetaBikeTravelTime() {
        return betaBikeTravelTime;
    }
    @StringSetter(BETA_BIKE_TRAVEL_TIME)
    public void setBetaBikeTravelTime(double value) {
        this.betaBikeTravelTime = value;
    }
    @StringGetter(BETA_BIKE_TRAVEL_TIME_POWER2)
    public double getBetaBikeTravelTimePower2() {
        return betaBikeTravelTimePower2;
    }
    @StringGetter(BETA_BIKE_TRAVEL_TIME_POWER2)
    public void setBetaBikeTravelTimePower2(double value) {
        this.betaBikeTravelTimePower2 = value;
    }
    @StringGetter(BETA_BIKE_LN_TRAVEL_TIME)
    public double getBetaBikeLnTravelTime() {
        return betaBikeLnTravelTime;
    }
    @StringSetter(BETA_BIKE_LN_TRAVEL_TIME)
    public void setBetaBikeLnTravelTime(double value) {
        this.betaBikeLnTravelTime = value;
    }
    @StringGetter(BETA_BIKE_LN_TRAVEL_DISTANCE)
    public double getBetaBikeTravelDistance() {
        return betaBikeTravelDistance;
    }
    @StringSetter(BETA_BIKE_TRAVEL_DISTANCE)
    public void setBetaBikeTravelDistance(double value) {
        this.betaBikeTravelDistance = value;
    }
    @StringGetter(BETA_BIKE_TRAVEL_DISTANCE_POWER2)
    public double getBetaBikeTravelDistancePower2() {
        return betaBikeTravelDistancePower2;
    }
    @StringSetter(BETA_BIKE_TRAVEL_DISTANCE_POWER2)
    public void setBetaBikeTravelDistancePower2(double value) {
        this.betaBikeTravelDistancePower2 = value;
    }
    @StringGetter(BETA_BIKE_LN_TRAVEL_DISTANCE)
    public double getBetaBikeLnTravelDistance() {
        return betaBikeLnTravelDistance;
    }
    @StringSetter(BETA_BIKE_LN_TRAVEL_DISTANCE)
    public void setBetaBikeLnTravelDistance(double value) {
        this.betaBikeLnTravelDistance = value;
    }
    @StringGetter(BETA_BIKE_TRAVEL_COST)
    public double getBetaBikeTravelMonetaryCost() {
        return betaBikeTravelMonetaryCost;
    }
    @StringSetter(BETA_BIKE_TRAVEL_COST)
    public void setBetaBikeTravelMonetaryCost(double value) {
        this.betaBikeTravelMonetaryCost = value;
    }
    @StringGetter(BETA_BIKE_TRAVEL_COST_POWER2)
    public double getBetaBikeTravelMonetaryCostPower2() {
        return betaBikeTravelMonetaryCostPower2;
    }
    @StringSetter(BETA_BIKE_TRAVEL_COST_POWER2)
    public void setBetaBikeTravelMonetaryCostPower2(double value) {
        this.betaBikeTravelMonetaryCostPower2 = value;
    }
    @StringGetter(BETA_BIKE_LN_TRAVEL_COST)
    public double getBetaBikeLnTravelMonetaryCost() {
        return betaBikeLnTravelMonetrayCost;
    }
    @StringSetter(BETA_BIKE_LN_TRAVEL_COST)
    public void setBetaBikeLnTravelMonetaryCost(double value) {
        this.betaBikeLnTravelMonetrayCost = value;
    }
    // ===
    public double getBetaWalkTravelTime() {
        return betaWalkTravelTime;
    }

    public void setBetaWalkTravelTime(double value) {
        this.betaWalkTravelTime = value;
    }

    public double getBetaWalkTravelTimePower2() {
        return betaWalkTravelTimePower2;
    }

    public void setBetaWalkTravelTimePower2(double value) {
        this.betaWalkTravelTimePower2 = value;
    }

    public double getBetaWalkLnTravelTime() {
        return betaWalkLnTravelTime;
    }

    public void setBetaWalkLnTravelTime(double value) {
        this.betaWalkLnTravelTime = value;
    }

    public double getBetaWalkTravelDistance() {
        return betaWalkTravelDistance;
    }

    public void setBetaWalkTravelDistance(double value) {
        this.betaWalkTravelDistance = value;
    }

    public double getBetaWalkTravelDistancePower2() {
        return betaWalkTravelDistancePower2;
    }

    public void setBetaWalkTravelDistancePower2(double value) {
        this.betaWalkTravelDistancePower2 = value;
    }

    public double getBetaWalkLnTravelDistance() {
        return betaWalkLnTravelDistance;
    }

    public void setBetaWalkLnTravelDistance(double value) {
        this.betaWalkLnTravelDistance = value;
    }

    public double getBetaWalkTravelMonetaryCost() {
        return betaWalkTravelMonetaryCost;
    }

    public void setBetaWalkTravelMonetaryCost(double value) {
        this.betaWalkTravelMonetaryCost = value;
    }

    public double getBetaWalkTravelMonetaryCostPower2() {
        return betaWalkTravelMonetrayCostPower2;
    }

    public void setBetaWalkTravelMonetaryCostPower2(double value) {
        this.betaWalkTravelMonetrayCostPower2 = value;
    }

    public double getBetaWalkLnTravelMonetaryCost() {
        return betaWalkLnTravelMonetrayCost;
    }

    public void setBetaWalkLnTravelMonetaryCost(double value) {
        this.betaWalkLnTravelMonetrayCost = value;
    }
    
    public double getBetaPtTravelTime() {
        return betaPtTravelTime;
    }

    public void setBetaPtTravelTime(double value) {
        this.betaPtTravelTime = value;
    }

    public double getBetaPtTravelTimePower2() {
        return betaPtTravelTimePower2;
    }

    public void setBetaPtTravelTimePower2(double value) {
        this.betaPtTravelTimePower2 = value;
    }

    public double getBetaPtLnTravelTime() {
        return betaPtLnTravelTime;
    }

    public void setBetaPtLnTravelTime(double value) {
        this.betaPtLnTravelTime = value;
    }

    public double getBetaPtTravelDistance() {
        return betaPtTravelDistance;
    }

    public void setBetaPtTravelDistance(double value) {
        this.betaPtTravelDistance = value;
    }

    public double getBetaPtTravelDistancePower2() {
        return betaPtTravelDistancePower2;
    }

    public void setBetaPtTravelDistancePower2(double value) {
        this.betaPtTravelDistancePower2 = value;
    }

    public double getBetaPtLnTravelDistance() {
        return betaPtLnTravelDistance;
    }

    public void setBetaPtLnTravelDistance(double value) {
        this.betaPtLnTravelDistance = value;
    }

    public double getBetaPtTravelMonetaryCost() {
        return betaPtTravelMonetrayCost;
    }

    public void setBetaPtTravelMonetaryCost(double value) {
        this.betaPtTravelMonetrayCost = value;
    }

    public double getBetaPtTravelMonetaryCostPower2() {
        return betaPtTravelMonetrayCostPower2;
    }

    public void setBetaPtTravelMonetaryCostPower2(double value) {
        this.betaPtTravelMonetrayCostPower2 = value;
    }

    public double getBetaPtLnTravelMonetaryCost() {
        return betaPtLnTravelMonetrayCost;
    }

    public void setBetaPtLnTravelMonetaryCost(double value) {
        this.betaPtLnTravelMonetrayCost = value;
    }
    
}
