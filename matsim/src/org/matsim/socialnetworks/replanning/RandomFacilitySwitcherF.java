/* *********************************************************************** *
 * project: org.matsim.*
 * SNFacilitySwitcher.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.socialnetworks.replanning;

import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
/**
 * A sample location choice replanning StrategyModule. The facility
 * (the place an activity takes place) is exchanged randomly for another
 * facility chosen randomly from all available facilities.
 * This is only performed for a given type(s) of activity.
 *  
 * @author jhackney
 *
 */

public class RandomFacilitySwitcherF extends MultithreadedModuleA {
	private NetworkLayer network=null;
	private TravelCost tcost=null;
	private TravelTime ttime=null;
	private Facilities facs=null;
	/** 
	 * TODO [JH] Activity types are hard-coded here but have to match the
	 * standard facility types in the facilities object as well as plans object.
	 * Need to make this change in the SNControllers, too.
	 */
	private String[] factypes={"home","work","shop","education","leisure"};
	
    public RandomFacilitySwitcherF(NetworkLayer network, TravelCost tcost, TravelTime ttime, Facilities facs) {

		System.out.println("initializing RandomFacilitySwitcher");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    	this.facs = facs;
    }

    public PlanAlgorithm getPlanAlgoInstance() {
//	return new SNSecLocShortest(factypes, network, tcost, ttime);
	return new RandomChangeLocationF(factypes, network, tcost, ttime, facs);
    }


}

