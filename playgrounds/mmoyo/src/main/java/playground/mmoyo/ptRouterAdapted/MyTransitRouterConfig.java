/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.ptRouterAdapted;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;

public class MyTransitRouterConfig extends TransitRouterConfig {

//	public double searchRadius = 600.0;    							//initial distance for station search around activity location
//	public double extensionRadius = 200.0;  						//extension distance for progressive search 
//	public double beelineWalkConnectionDistance = 300.0;  			//distance 	
//	public double beelineWalkSpeed = 3.0/3.6;  						// presumably, in m/sec.  3.0/3.6 = 3000/3600 = 3km/h.  kai, apr'10
//	public double marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0; 	//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
//	public double marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
//	public double marginalUtilityOfTravelDistanceTransit = -0.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
//	public double costLineSwitch = 60.0 * -this.marginalUtilityOfTravelTimeTransit;	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10

	public MyTransitRouterConfig(PlanCalcScoreConfigGroup pcsConfig, PlansCalcRouteConfigGroup pcrConfig, 
			TransitRouterConfigGroup trConfig, VspExperimentalConfigGroup vspConfig ) {
		super(pcsConfig, pcrConfig, trConfig, vspConfig);
	}
	
	//additional config variables
	public boolean allowDirectWalks = true;					//if a direct walk has a lower cost than the pt connection
	public int minStationsNum= 2; 							//minimal number of stations to find, before start the routing
	public String scenarioName = null;						//name of scenario or parameter set

}
