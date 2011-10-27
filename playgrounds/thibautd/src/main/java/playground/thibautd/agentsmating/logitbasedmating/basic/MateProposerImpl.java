/* *********************************************************************** *
 * project: org.matsim.*
 * MateProposerImpl.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.List;

import playground.thibautd.agentsmating.logitbasedmating.framework.MateProposer;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;

/**
 * @author thibautd
 */
public class MateProposerImpl implements MateProposer {

	@Override
	public <T extends TripRequest> List<T> proposeMateList(
			final TripRequest trip,
			final List<T> allPossibleMates) {
		// TODO Auto-generated method stub
		return null;
	}
}

