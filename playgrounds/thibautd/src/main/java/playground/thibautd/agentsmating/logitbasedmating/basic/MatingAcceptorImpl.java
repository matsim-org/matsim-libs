/* *********************************************************************** *
 * project: org.matsim.*
 * MatingAcceptorImpl.java
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

import playground.thibautd.agentsmating.logitbasedmating.framework.ChoiceModel;
import playground.thibautd.agentsmating.logitbasedmating.framework.MatingAcceptor;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;

/**
 * Default implementation of the {@link MatingAcceptor} interface.
 * It accepts a mating based on the probability that driver and passenger accept it,
 * knowing the other modes alternatives.
 *
 * @author thibautd
 */
public class MatingAcceptorImpl implements MatingAcceptor {
	private final ChoiceModel model;

	public MatingAcceptorImpl(
			final ChoiceModel model) {
		this.model = model;
	}

	@Override
	public boolean acceptMating(
			final TripRequest driver,
			final TripRequest passenger) {
		// TODO Auto-generated method stub
		return false;
	}
}

