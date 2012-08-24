/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vehicles.Vehicle;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.ConfigurationModule;

/**
 * @author thomas
 *
 */
public class FreeSpeedTravelTimeAndDistanceBasedTravelDisutility implements TravelDisutility {
	
	protected final TravelTime timeCalculator;
	
	// accessibility parameter
	private boolean useRawSum	= false;
	private double logitScaleParameter;
	private double inverseOfLogitScaleParameter;
	private double betaCarTT;		// in MATSim this is [utils/h]: cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr() 
	private double betaCarTTPower;
	private double betaCarLnTT;
	private double betaCarTD;		// in MATSim this is [utils/money * money/meter] = [utils/meter]: cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar()
	private double betaCarTDPower;
	private double betaCarLnTD;
	private double betaCarTC;		// in MATSim this is [utils/money]: cnScoringGroup.getMarginalUtilityOfMoney()
	private double betaCarTCPower;
	private double betaCarLnTC;
	
	protected double bikeSpeedMeterPerHour = -1;
	protected double walkSpeedMeterPerHour = -1;
	
	private static int wrnCnt = 0 ;
	
	public FreeSpeedTravelTimeAndDistanceBasedTravelDisutility(final TravelTime timeCalculator, ScenarioImpl scenario) {
		
		// how to get the origin location here to calculate cost to network??
		// When using Dj as opportunity weight for cost calculation, nodes without opportunities get zero, is this a problem???
		
		AccessibilityParameterConfigModule module = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		this.useRawSum				= module.isUseRawSumsWithoutLn();
		this.logitScaleParameter 	= module.getLogitScaleParameter();
		this.inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
		this.walkSpeedMeterPerHour 	= scenario.getConfig().plansCalcRoute().getWalkSpeed() * 3600.;
		this.bikeSpeedMeterPerHour 	= 15000.;
		this.betaCarTT 	   			= module.getBetaCarTravelTime();
		this.betaCarTTPower			= module.getBetaCarTravelTimePower2();
		this.betaCarLnTT			= module.getBetaCarLnTravelTime();
		this.betaCarTD				= module.getBetaCarTravelDistance();
		this.betaCarTDPower			= module.getBetaCarTravelDistancePower2();
		this.betaCarLnTD			= module.getBetaCarLnTravelDistance();
		this.betaCarTC				= module.getBetaCarTravelCost();
		this.betaCarTCPower			= module.getBetaCarTravelCostPower2();
		this.betaCarLnTC			= module.getBetaCarLnTravelCost();
		
		this.timeCalculator = timeCalculator;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		
//		if (this.marginalCostOfDistance == 0.0) {
//			return travelTime * this.marginalCostOfTime;
//		}
		// commenting this out since we think it is not (no longer?) necessary.  kai/benjamin, jun'11
		
		return this.marginalCostOfTime * travelTime + this.marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {

//		if (this.marginalCostOfDistance == 0.0) {
//			return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
//		}
		// commenting this out since we think it is not (no longer?) necessary.  kai/benjamin, jun'11

		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime
		+ this.marginalCostOfDistance * link.getLength();
	}

}
