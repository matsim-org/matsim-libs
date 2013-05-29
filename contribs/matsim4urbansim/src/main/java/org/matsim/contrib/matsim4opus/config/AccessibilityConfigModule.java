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

public class AccessibilityConfigModule extends ReflectiveModule{
	private static Logger log = Logger.getLogger( AccessibilityConfigModule.class ) ;

	public static final String GROUP_NAME = "accessibility";
	
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
    
	private double betaCarTravelTime;
	private double betaCarTravelTimePower2;
	private double betaCarLnTravelTime;
	private double betaCarTravelDistance;
	private double betaCarTravelDistancePower2;
	private double betaCarLnTravelDistance;
	private double betaCarTravelMonetaryCost;
	private double betaCarTravelMonetaryCostPower2;
	private double betaCarLnTravelMonetaryCost;
	
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

	public AccessibilityConfigModule() {
		super(GROUP_NAME);
		// yyyyyy this class feels quite dangerous to me; one can have inconsistent entries between the Map and the typed values. kai, apr'13 
	}
	
	@Override
	public Map<String,String> getComments() {
		Map<String,String> map = new TreeMap<String,String>() ;
		
		map.put("betaBikeLnMonetaryTravelCost","bicycle parameters") ;
		map.put("betaCarLnMonetaryTravelCost","car parameters") ;
		
		return map ;
	}
	
	// NOTE: It seems ok to have the string constants immediately here since having them separately really does not help
	// keeping the code compact
	
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
    // only betas below this line
    @StringGetter("betaCarTravelTime")
    public double getBetaCarTravelTime() {
        return betaCarTravelTime;
    }
    @StringSetter("betaCarTravelTime")
    public void setBetaCarTravelTime(double value) {
        this.betaCarTravelTime = value;
    }
    @StringGetter("betaCarTravelTimePower2")
    public double getBetaCarTravelTimePower2() {
        return betaCarTravelTimePower2;
    }
    @StringSetter("betaCarTravelTimePower2")
    public void setBetaCarTravelTimePower2(double value) {
        this.betaCarTravelTimePower2 = value;
    }
    @StringGetter("betaCarLnTravelTime")
    public double getBetaCarLnTravelTime() {
        return betaCarLnTravelTime;
    }
    @StringSetter("betaCarLnTravelTime")
    public void setBetaCarLnTravelTime(double value) {
        this.betaCarLnTravelTime = value;
    }
    @StringGetter("betaCarTravelDistance")
    public double getBetaCarTravelDistance() {
        return betaCarTravelDistance;
    }
    @StringSetter("betaCarTravelDistance")
    public void setBetaCarTravelDistance(double value) {
        this.betaCarTravelDistance = value;
    }
    @StringGetter("betaCarTravelDistancePower2")
    public double getBetaCarTravelDistancePower2() {
        return betaCarTravelDistancePower2;
    }
    @StringSetter("betaCarTravelDistancePower2")
    public void setBetaCarTravelDistancePower2(double value) {
        this.betaCarTravelDistancePower2 = value;
    }
    @StringGetter("betaCarLnTravelDistance")
    public double getBetaCarLnTravelDistance() {
        return betaCarLnTravelDistance;
    }
    @StringSetter("betaCarLnTravelDistance")
    public void setBetaCarLnTravelDistance(double value) {
        this.betaCarLnTravelDistance = value;
    }
    @StringGetter("betaCarMonetaryTravelCost")
    public double getBetaCarTravelMonetaryCost() {
        return betaCarTravelMonetaryCost;
    }
    @StringSetter("betaCarMonetaryTravelCost")
    public void setBetaCarTravelMonetaryCost(double value) {
        this.betaCarTravelMonetaryCost = value;
    }
    @StringGetter("betaCarMonetaryTravelCostPower2")
    public double getBetaCarTravelMonetaryCostPower2() {
        return betaCarTravelMonetaryCostPower2;
    }
    @StringSetter("betaCarMonetaryTravelCostPower2")
    public void setBetaCarTravelMonetaryCostPower2(double value) {
        this.betaCarTravelMonetaryCostPower2 = value;
    }
    @StringGetter("betaCarLnMonetaryTravelCost")
    public double getBetaCarLnTravelMonetaryCost() {
        return betaCarLnTravelMonetaryCost;
    }
    @StringSetter("betaCarLnMonetaryTravelCost")
    public void setBetaCarLnTravelMonetaryCost(double value) {
        this.betaCarLnTravelMonetaryCost = value;
    }
    // ===
    @StringGetter("betaBikeTravelTime")
    public double getBetaBikeTravelTime() {
        return betaBikeTravelTime;
    }
    @StringSetter("betaBikeTravelTime")
    public void setBetaBikeTravelTime(double value) {
        this.betaBikeTravelTime = value;
    }
    @StringGetter("betaBikeTravelTimePower2")
    public double getBetaBikeTravelTimePower2() {
        return betaBikeTravelTimePower2;
    }
    @StringSetter("betaBikeTravelTimePower2")
    public void setBetaBikeTravelTimePower2(double value) {
        this.betaBikeTravelTimePower2 = value;
    }
    @StringGetter("betaBikeLnTravelTime")
    public double getBetaBikeLnTravelTime() {
        return betaBikeLnTravelTime;
    }
    @StringSetter("betaBikeLnTravelTime")
    public void setBetaBikeLnTravelTime(double value) {
        this.betaBikeLnTravelTime = value;
    }
    @StringGetter("betaBikeTravelDistance")
    public double getBetaBikeTravelDistance() {
        return betaBikeTravelDistance;
    }
    @StringSetter("betaBikeTravelDistance")
    public void setBetaBikeTravelDistance(double value) {
        this.betaBikeTravelDistance = value;
    }
    @StringGetter("betaBikeTravelDistancePower2")
    public double getBetaBikeTravelDistancePower2() {
        return betaBikeTravelDistancePower2;
    }
    @StringSetter("betaBikeTravelDistancePower2")
    public void setBetaBikeTravelDistancePower2(double value) {
        this.betaBikeTravelDistancePower2 = value;
    }
    @StringGetter("betaBikeLnTravelDistance")
    public double getBetaBikeLnTravelDistance() {
        return betaBikeLnTravelDistance;
    }
    @StringSetter("betaBikeLnTravelDistance")
    public void setBetaBikeLnTravelDistance(double value) {
        this.betaBikeLnTravelDistance = value;
    }
    @StringGetter("betaBikeMonetaryTravelCost")
    public double getBetaBikeTravelMonetaryCost() {
        return betaBikeTravelMonetaryCost;
    }
    @StringSetter("betaBikeMonetaryTravelCost")
    public void setBetaBikeTravelMonetaryCost(double value) {
        this.betaBikeTravelMonetaryCost = value;
    }
    @StringGetter("betaBikeMonetaryTravelCostPower2")
    public double getBetaBikeTravelMonetaryCostPower2() {
        return betaBikeTravelMonetaryCostPower2;
    }
    @StringSetter("betaBikeMonetaryTravelCostPower2")
    public void setBetaBikeTravelMonetaryCostPower2(double value) {
        this.betaBikeTravelMonetaryCostPower2 = value;
    }
    @StringGetter("betaBikeLnMonetaryTravelCost")
    public double getBetaBikeLnTravelMonetaryCost() {
        return betaBikeLnTravelMonetrayCost;
    }
    @StringSetter("betaBikeLnMonetaryTravelCost")
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
    // ---
    @StringGetter( "betaPtTravelTime" )
    public double getBetaPtTravelTime() {
        return betaPtTravelTime;
    }
    @StringSetter( "betaPtTravelTime" )
    public void setBetaPtTravelTime(double value) {
        this.betaPtTravelTime = value;
    }
    @StringGetter( "betaPtTravelTimePower2" )
    public double getBetaPtTravelTimePower2() {
        return betaPtTravelTimePower2;
    }
    @StringSetter( "betaPtTravelTimePower2" )
    public void setBetaPtTravelTimePower2(double value) {
        this.betaPtTravelTimePower2 = value;
    }
    @StringGetter( "betaPtLnTravelTime" )
    public double getBetaPtLnTravelTime() {
        return betaPtLnTravelTime;
    }
    @StringSetter( "betaPtLnTravelTime" )
    public void setBetaPtLnTravelTime(double value) {
        this.betaPtLnTravelTime = value;
    }
    @StringGetter( "betaPtTravelDistance" )
    public double getBetaPtTravelDistance() {
        return betaPtTravelDistance;
    }
    @StringSetter( "betaPtTravelDistance" )
    public void setBetaPtTravelDistance(double value) {
        this.betaPtTravelDistance = value;
    }
    @StringGetter( "betaPtTravelDistancePower2" )
    public double getBetaPtTravelDistancePower2() {
        return betaPtTravelDistancePower2;
    }
    @StringSetter( "betaPtTravelDistancePower2" )
    public void setBetaPtTravelDistancePower2(double value) {
        this.betaPtTravelDistancePower2 = value;
    }
    @StringGetter( "betaPtLnTravelDistance" )
    public double getBetaPtLnTravelDistance() {
        return betaPtLnTravelDistance;
    }
    @StringSetter( "betaPtLnTravelDistance" )
    public void setBetaPtLnTravelDistance(double value) {
        this.betaPtLnTravelDistance = value;
    }
    @StringGetter( "betaPtTravelCost" )
    public double getBetaPtTravelMonetaryCost() {
        return betaPtTravelMonetrayCost;
    }
    @StringSetter( "betaPtTravelCost" )
    public void setBetaPtTravelMonetaryCost(double value) {
        this.betaPtTravelMonetrayCost = value;
    }
    @StringGetter( "betaPtTravelCostPower2" )
    public double getBetaPtTravelMonetaryCostPower2() {
        return betaPtTravelMonetrayCostPower2;
    }
    @StringSetter( "betaPtTravelCostPower2" )
    public void setBetaPtTravelMonetaryCostPower2(double value) {
        this.betaPtTravelMonetrayCostPower2 = value;
    }
    @StringGetter( "betaPtLnTravelCost" )
    public double getBetaPtLnTravelMonetaryCost() {
        return betaPtLnTravelMonetrayCost;
    }
    @StringSetter( "betaPtLnTravelCost" )
    public void setBetaPtLnTravelMonetaryCost(double value) {
        this.betaPtLnTravelMonetrayCost = value;
    }
    
}
