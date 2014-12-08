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

package playground.andreas.aas.modules.cellBasedAccessibility.config;

import org.matsim.core.config.ConfigGroup;

public class AccessibilityParameterConfigModule extends ConfigGroup{

	public static final String GROUP_NAME = "accessibilityParameter";
	
	private double accessibilityDestinationSamplingRate;
	
	private boolean useLogitScaleParameterFromMATSim;
	
	private boolean useCarParameterFromMATSim;
	
	private boolean useBikeParameterFromMATSim;
	
	private boolean useWalkParameterFromMATSim;
    
	private boolean useRawSumsWithoutLn;
    
	private double logitScaleParameter;
    
	private double betaCarTravelTime;
    
	private double betaCarTravelTimePower2;
    
	private double betaCarLnTravelTime;
    
	private double betaCarTravelDistance;
    
	private double betaCarTravelDistancePower2;
    
	private double betaCarLnTravelDistance;
    
	private double betaCarTravelCost;
    
	private double betaCarTravelCostPower2;
    
	private double betaCarLnTravelCost;
	
	private double betaBikeTravelTime;
    
	private double betaBikeTravelTimePower2;
    
	private double betaBikeLnTravelTime;
    
	private double betaBikeTravelDistance;
    
	private double betaBikeTravelDistancePower2;
    
	private double betaBikeLnTravelDistance;
    
	private double betaBikeTravelCost;
    
	private double betaBikeTravelCostPower2;
    
	private double betaBikeLnTravelCost;
	
	private double betaWalkTravelTime;
    
	private double betaWalkTravelTimePower2;
    
	private double betaWalkLnTravelTime;
    
	private double betaWalkTravelDistance;
    
	private double betaWalkTravelDistancePower2;
    
	private double betaWalkLnTravelDistance;
    
	private double betaWalkTravelCost;
    
	private double betaWalkTravelCostPower2;
    
	private double betaWalkLnTravelCost;
	
	public AccessibilityParameterConfigModule(String name) {
		super(name);
	}
	
	public double getAccessibilityDestinationSamplingRate(){
		return this.accessibilityDestinationSamplingRate;
	}
	
	public void setAccessibilityDestinationSamplingRate(double sampleRate){
		this.accessibilityDestinationSamplingRate = sampleRate;
	}

    public boolean isUseLogitScaleParameterFromMATSim() {
        return useLogitScaleParameterFromMATSim;
    }

    public void setUseLogitScaleParameterFromMATSim(boolean value) {
        this.useLogitScaleParameterFromMATSim = value;
    }

    public boolean isUseCarParameterFromMATSim() {
        return useCarParameterFromMATSim;
    }

    public void setUseCarParameterFromMATSim(boolean value) {
        this.useCarParameterFromMATSim = value;
    }
    
    public boolean isUseBikeParameterFromMATSim() {
        return useBikeParameterFromMATSim;
    }

    public void setUseBikeParameterFromMATSim(boolean value) {
        this.useBikeParameterFromMATSim = value;
    }
    
    public boolean isUseWalkParameterFromMATSim() {
        return useWalkParameterFromMATSim;
    }

    public void setUseWalkParameterFromMATSim(boolean value) {
        this.useWalkParameterFromMATSim = value;
    }

    public boolean isUseRawSumsWithoutLn() {
        return useRawSumsWithoutLn;
    }

    public void setUseRawSumsWithoutLn(boolean value) {
        this.useRawSumsWithoutLn = value;
    }

    public double getLogitScaleParameter() {
        return logitScaleParameter;
    }

    public void setLogitScaleParameter(double value) {
        this.logitScaleParameter = value;
    }

    public double getBetaCarTravelTime() {
        return betaCarTravelTime;
    }

    public void setBetaCarTravelTime(double value) {
        this.betaCarTravelTime = value;
    }

    public double getBetaCarTravelTimePower2() {
        return betaCarTravelTimePower2;
    }

    public void setBetaCarTravelTimePower2(double value) {
        this.betaCarTravelTimePower2 = value;
    }

    public double getBetaCarLnTravelTime() {
        return betaCarLnTravelTime;
    }

    public void setBetaCarLnTravelTime(double value) {
        this.betaCarLnTravelTime = value;
    }

    public double getBetaCarTravelDistance() {
        return betaCarTravelDistance;
    }

    public void setBetaCarTravelDistance(double value) {
        this.betaCarTravelDistance = value;
    }

    public double getBetaCarTravelDistancePower2() {
        return betaCarTravelDistancePower2;
    }

    public void setBetaCarTravelDistancePower2(double value) {
        this.betaCarTravelDistancePower2 = value;
    }

    public double getBetaCarLnTravelDistance() {
        return betaCarLnTravelDistance;
    }

    public void setBetaCarLnTravelDistance(double value) {
        this.betaCarLnTravelDistance = value;
    }

    public double getBetaCarTravelCost() {
        return betaCarTravelCost;
    }

    public void setBetaCarTravelCost(double value) {
        this.betaCarTravelCost = value;
    }

    public double getBetaCarTravelCostPower2() {
        return betaCarTravelCostPower2;
    }

    public void setBetaCarTravelCostPower2(double value) {
        this.betaCarTravelCostPower2 = value;
    }

    public double getBetaCarLnTravelCost() {
        return betaCarLnTravelCost;
    }

    public void setBetaCarLnTravelCost(double value) {
        this.betaCarLnTravelCost = value;
    }
    
    public double getBetaBikeTravelTime() {
        return betaBikeTravelTime;
    }

    public void setBetaBikeTravelTime(double value) {
        this.betaBikeTravelTime = value;
    }

    public double getBetaBikeTravelTimePower2() {
        return betaBikeTravelTimePower2;
    }

    public void setBetaBikeTravelTimePower2(double value) {
        this.betaBikeTravelTimePower2 = value;
    }

    public double getBetaBikeLnTravelTime() {
        return betaBikeLnTravelTime;
    }

    public void setBetaBikeLnTravelTime(double value) {
        this.betaBikeLnTravelTime = value;
    }

    public double getBetaBikeTravelDistance() {
        return betaBikeTravelDistance;
    }

    public void setBetaBikeTravelDistance(double value) {
        this.betaBikeTravelDistance = value;
    }

    public double getBetaBikeTravelDistancePower2() {
        return betaBikeTravelDistancePower2;
    }

    public void setBetaBikeTravelDistancePower2(double value) {
        this.betaBikeTravelDistancePower2 = value;
    }

    public double getBetaBikeLnTravelDistance() {
        return betaBikeLnTravelDistance;
    }

    public void setBetaBikeLnTravelDistance(double value) {
        this.betaBikeLnTravelDistance = value;
    }

    public double getBetaBikeTravelCost() {
        return betaBikeTravelCost;
    }

    public void setBetaBikeTravelCost(double value) {
        this.betaBikeTravelCost = value;
    }

    public double getBetaBikeTravelCostPower2() {
        return betaBikeTravelCostPower2;
    }

    public void setBetaBikeTravelCostPower2(double value) {
        this.betaBikeTravelCostPower2 = value;
    }

    public double getBetaBikeLnTravelCost() {
        return betaBikeLnTravelCost;
    }

    public void setBetaBikeLnTravelCost(double value) {
        this.betaBikeLnTravelCost = value;
    }

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

    public double getBetaWalkTravelCost() {
        return betaWalkTravelCost;
    }

    public void setBetaWalkTravelCost(double value) {
        this.betaWalkTravelCost = value;
    }

    public double getBetaWalkTravelCostPower2() {
        return betaWalkTravelCostPower2;
    }

    public void setBetaWalkTravelCostPower2(double value) {
        this.betaWalkTravelCostPower2 = value;
    }

    public double getBetaWalkLnTravelCost() {
        return betaWalkLnTravelCost;
    }

    public void setBetaWalkLnTravelCost(double value) {
        this.betaWalkLnTravelCost = value;
    }

}
