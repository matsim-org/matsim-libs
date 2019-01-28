/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.examples.random;

import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.core.gbl.MatsimRandom;

import com.google.common.collect.Iterators;

public class RandomDynAgentLogic implements DynAgentLogic {
	private final Network network;

	private DynAgent agent;

	public RandomDynAgentLogic(Network network) {
		this.network = network;
	}

	@Override
	public DynActivity computeInitialActivity(DynAgent agent) {
		this.agent = agent;
		return new RandomDynActivity(0);
	}

	@Override
	public DynAgent getDynAgent() {
		return agent;
	}

	@Override
	public DynAction computeNextAction(DynAction oldAction, double now) {
		// I am tired, I want to stop being simulated (2% chance)
		if (MatsimRandom.getRandom().nextInt(50) == 0) {
			return new IdleDynActivity("Infinite laziness :-)", Double.POSITIVE_INFINITY);
		}

		// Do I want to stay or drive? (50-50 choice)
		if (MatsimRandom.getRandom().nextBoolean()) {
			return new RandomDynActivity(now);
		} else {
			return new RandomDynLeg(agent.getCurrentLinkId(), network);
		}
	}

	static <E> E chooseRandomElement(Set<E> set) {
		int randomIndex = MatsimRandom.getRandom().nextInt(set.size());
		return Iterators.get(set.iterator(), randomIndex);
	}
}
