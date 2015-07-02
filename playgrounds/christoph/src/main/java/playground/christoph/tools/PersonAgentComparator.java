/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PersonAgentComparator.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.christoph.tools;

import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.framework.MobsimAgent;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two {@link MobsimAgent}s according to their Id.
 *
 * @author cdobler
 */
public class PersonAgentComparator implements Comparator<MobsimAgent>, Serializable, MatsimComparator {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(MobsimAgent agent1, MobsimAgent agent2) {
		return agent1.getId().compareTo(agent2.getId());
	}

}
