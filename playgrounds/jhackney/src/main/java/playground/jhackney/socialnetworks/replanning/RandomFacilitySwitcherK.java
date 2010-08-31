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

package playground.jhackney.socialnetworks.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.SocNetConfigGroup;

/**
 * A sample location choice replanning StrategyModule. The facility
 * (the place an activity takes place) is exchanged randomly for another
 * facility chosen randomly from facilities in agent's knowledge.
 * This is only performed for a given type(s) of activity.
 * @author jhackney
 *
 */

public class RandomFacilitySwitcherK extends AbstractMultithreadedModule {
	private final static Logger log = Logger.getLogger(RandomFacilitySwitcherK.class);
	private Network network=null;
	private PersonalizableTravelCost tcost=null;
	private PersonalizableTravelTime ttime=null;
	/**
	 * TODO [JH] Activity types are hard-coded here but have to match the
	 * standard facility types in the facilities object as well as plans object.
	 * Need to make this change in the SNControllers, too.
	 */
	private String[] factypes={"home","work","shop","education","leisure"};
	private Knowledges knowledges;
	private final SocNetConfigGroup snConfig;

    public RandomFacilitySwitcherK(Config config, Network network, PersonalizableTravelCost tcost, PersonalizableTravelTime ttime, Knowledges kn) {
    	super(config.global());
		log.info("initializing SNRandomFacilitySwitcher");
    	this.network=network;
    	this.tcost = tcost;
    	this.ttime = ttime;
    	this.knowledges = kn;
    	this.snConfig = (SocNetConfigGroup) config.getModule(SocNetConfigGroup.GROUP_NAME);
    }

    @Override
		public PlanAlgorithm getPlanAlgoInstance() {
//	return new SNSecLocShortest(factypes, network, tcost, ttime);
	return new RandomChangeLocationK(factypes, network, tcost, ttime, this.knowledges, this.snConfig);
    }


}
