/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

/**
 * Design decisions:<ul>
 * <li>At this point, only those variables that are needed elsewhere (in particular in the scoring) are set from the
 * config.  All other variables are considered internal computational variables.  kai, feb'11
 * </ul>
 *
 */
public class TransitRouterConfig implements MatsimParameters {

	/**
	 * The distance in meters in which stop facilities should be searched for
	 * around the start and end coordinate.
	 */
	private double searchRadius = 1000.0;

	/**
	 * If no stop facility is found around start or end coordinate (see
	 * {@link #searchRadius}), the nearest stop location is searched for
	 * and the distance from start/end coordinate to this location is
	 * extended by the given amount.<br />
	 * If only one stop facility is found within {@link #searchRadius},
	 * the radius is also extended in the hope to find more stop
	 * facilities (e.g. in the opposite direction of the already found
	 * stop).
	 */
	private double extensionRadius = 200.0;

	/**
	 * The distance in meters that agents can walk to get from one stop to
	 * another stop of a nearby transit line.
	 * <p/>
	 * Is this really needed?  If the marg utl of walk is correctly set, this should come out automagically.
	 * kai, feb'11
	 * This value is used to generate the walk connections between stop facilities. If they are used,
	 * depends on the scoring/cost calculation. But when they are missing, they cannot be used at all.
	 * mrieser, mar'11
	 */
	private double beelineWalkConnectionDistance = 100.0;

	/**
	 * The minimum time needed for a transfer is calculated based on the distance and the beeline walk speed
	 * between two stop facilities. Due to passengers probably not being able to immediately (=in the same
	 * second) leave a transit vehicle, or a vehicle being delayed by a small amount of time, an additional
	 * "savety" time can be added to transfers when searching for connecting trips. This could help to find
	 * "better" transfer connections such as they can indeed be realized by the simulation. This value only
	 * affects the routing process, not the simulation itself.
	 */
	private double additionalTransferTime = 0.0;

	private double beelineWalkSpeed; // meter / second

	private double marginalUtilityOfTravelTimeWalk_utl_s;

	private double marginalUtilityOfTravelTimeTransit_utl_s;

	private double marginalUtilityOfWaitingPt_utl_s;

	private double marginalUtilityOfTravelDistanceWalk_utl_m;
	
	private double marginalUtilityOfTravelDistanceTransit_utl_m;

	private double utilityOfLineSwitch_utl;

	public TransitRouterConfig(final Config config) {
		this(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	}
	
	public TransitRouterConfig(final PlanCalcScoreConfigGroup pcsConfig, final PlansCalcRouteConfigGroup pcrConfig, 
			final TransitRouterConfigGroup trConfig, final VspExperimentalConfigGroup vspConfig ) 
	{
		pcsConfig.setLocked(); pcrConfig.setLocked() ; trConfig.setLocked() ; vspConfig.setLocked() ;
		
		// walk:
		this.beelineWalkSpeed = pcrConfig.getTeleportedModeSpeeds().get(TransportMode.walk)
				/ pcrConfig.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor() ;

		this.marginalUtilityOfTravelTimeWalk_utl_s = pcsConfig.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() /3600.0 - pcsConfig.getPerforming_utils_hr()/3600. ;
		
		this.marginalUtilityOfTravelDistanceWalk_utl_m = pcsConfig.getMarginalUtilityOfMoney() * pcsConfig.getModes().get(TransportMode.walk).getMonetaryDistanceRate();
		
		// pt:
		this.marginalUtilityOfTravelTimeTransit_utl_s = pcsConfig.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() /3600.0 - pcsConfig.getPerforming_utils_hr()/3600. ;

		this.marginalUtilityOfTravelDistanceTransit_utl_m = pcsConfig.getMarginalUtilityOfMoney() * pcsConfig.getModes().get(TransportMode.pt).getMonetaryDistanceRate();

		this.marginalUtilityOfWaitingPt_utl_s = pcsConfig.getMarginalUtlOfWaitingPt_utils_hr() / 3600.0 - pcsConfig.getPerforming_utils_hr()/3600. ;

		this.utilityOfLineSwitch_utl = pcsConfig.getUtilityOfLineSwitch();

		// router:
		this.setSearchRadius(trConfig.getSearchRadius());
		this.setExtensionRadius(trConfig.getExtensionRadius());
		this.setBeelineWalkConnectionDistance(trConfig.getMaxBeelineWalkConnectionDistance());
		this.setAdditionalTransferTime(trConfig.getAdditionalTransferTime());
	}

	public void setUtilityOfLineSwitch_utl(final double utilityOfLineSwitch_utl_sec) {
		this.utilityOfLineSwitch_utl = utilityOfLineSwitch_utl_sec;
	}

	/**
	 * The additional utility to be added when an agent switches lines.  Normally negative
	 * <p/>
	 * The "_utl" can go as soon as we are confident that there are no more utilities in "Eu".  kai, feb'11
	 */
	public double getUtilityOfLineSwitch_utl() {
		return this.utilityOfLineSwitch_utl;
	}

	public void setMarginalUtilityOfTravelTimeWalk_utl_s(final double marginalUtilityOfTravelTimeWalk_utl_s) {
		this.marginalUtilityOfTravelTimeWalk_utl_s = marginalUtilityOfTravelTimeWalk_utl_s;
	}

	public double getMarginalUtilityOfTravelTimeWalk_utl_s() {
		return this.marginalUtilityOfTravelTimeWalk_utl_s;
	}

	public void setMarginalUtilityOfTravelTimePt_utl_s(final double marginalUtilityOfTravelTimeTransit_utl_s) {
		this.marginalUtilityOfTravelTimeTransit_utl_s = marginalUtilityOfTravelTimeTransit_utl_s;
	}
	
	public void setMarginalUtilityOfTravelDistanceWalk_utl_m(final double marginalUtilityOfTravelDistanceWalk_utl_m) {
		this.marginalUtilityOfTravelDistanceWalk_utl_m = marginalUtilityOfTravelDistanceWalk_utl_m;
	}
	
	/**
	 * @return the marginal utility of travel time by public transit.
	 */
	public double getMarginalUtilityOfTravelTimePt_utl_s() {
		return this.marginalUtilityOfTravelTimeTransit_utl_s;
	}

	public void setMarginalUtilityOfTravelDistancePt_utl_m(final double marginalUtilityOfTravelDistanceTransit_utl_m) {
		this.marginalUtilityOfTravelDistanceTransit_utl_m = marginalUtilityOfTravelDistanceTransit_utl_m;
	}

	public double getMarginalUtilityOfWaitingPt_utl_s() {
		return this.marginalUtilityOfWaitingPt_utl_s;
	}

	public void setMarginalUtilityOfWaitingPt_utl_s(final double effectiveMarginalUtilityOfWaiting_utl_s) {
		this.marginalUtilityOfWaitingPt_utl_s = effectiveMarginalUtilityOfWaiting_utl_s;
	}

	/**
	 * in the config, this is distanceCostRate * margUtlOfMoney.  For the router, the conversion to
	 * utils seems ok.  kai, feb'11
	 */
	public double getMarginalUtilityOfTravelDistancePt_utl_m() {
		return this.marginalUtilityOfTravelDistanceTransit_utl_m;
	}

	public double getMarginalUtilityOfTravelDistanceWalk_utl_m() {
		return this.marginalUtilityOfTravelDistanceWalk_utl_m;
	}
	
	public void setBeelineWalkSpeed(final double beelineWalkSpeed) {
		this.beelineWalkSpeed = beelineWalkSpeed;
	}

	/**
	 * Walking speed of agents on transfer links, beeline distance.
	 */
	public double getBeelineWalkSpeed() {
		return this.beelineWalkSpeed;
	}

	public double getSearchRadius() {
		return searchRadius;
	}

	public void setSearchRadius(double searchRadius) {
		this.searchRadius = searchRadius;
	}

	public double getExtensionRadius() {
		return extensionRadius;
	}

	public void setExtensionRadius(double extensionRadius) {
		this.extensionRadius = extensionRadius;
	}

	public double getBeelineWalkConnectionDistance() {
		return beelineWalkConnectionDistance;
	}

	public void setBeelineWalkConnectionDistance(double beelineWalkConnectionDistance) {
		this.beelineWalkConnectionDistance = beelineWalkConnectionDistance;
	}

	public double getAdditionalTransferTime() {
		return additionalTransferTime;
	}

	public void setAdditionalTransferTime(double additionalTransferTime) {
		this.additionalTransferTime = additionalTransferTime;
	}

}
