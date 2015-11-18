/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.maxess.nestedlogitaccessibility;

/**
 * @author thibautd
 */
public class NestedLogitModel {
	private final double mu;
	private final Utility utility;
	private final ChoiceSetIdentifier choiceSetIdentifier;

	public NestedLogitModel(
			final double mu,
			final Utility utility,
			final ChoiceSetIdentifier choiceSetIdentifier ) {
		this.mu = mu;
		this.utility = utility;
		this.choiceSetIdentifier = choiceSetIdentifier;
	}

	public ChoiceSetIdentifier getChoiceSetIdentifier() {
		return choiceSetIdentifier;
	}

	public double getMu() {
		return mu;
	}

	public Utility getUtility() {
		return utility;
	}
}
