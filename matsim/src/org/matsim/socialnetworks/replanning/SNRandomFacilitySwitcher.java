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

import org.apache.log4j.Logger;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.socialnetworks.io.PajekWriter;
/**
 * The social network replanning StrategyModule is not multi-threaded because each
 * agent could refer to and alter other agent objects in a random manner.
 *  
 * @author jhackney
 *
 */

public class SNRandomFacilitySwitcher extends MultithreadedModuleA {
	private final static Logger log = Logger.getLogger(SNRandomFacilitySwitcher.class);
	private NetworkLayer network=null;
	private TravelCost tcost=null;
	private TravelTime ttime=null;
	/** 
	 * TODO [JH] this is hard-coded here but has to match the standard facility types
	 * in the facilities object. Need to make this change in the SNControllers, too.
	 */
	private String[] factypes={"home","work","shop","education","leisure"};
	
    public SNRandomFacilitySwitcher(NetworkLayer network, TravelCost tcost, TravelTime ttime) {

		log.info("initializing SNRandomFacilitySwitcher");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    }

    public PlanAlgorithm getPlanAlgoInstance() {
//	return new SNSecLocShortest(factypes, network, tcost, ttime);
	return new SNSecLocRandom(factypes, network, tcost, ttime);
    }


}
