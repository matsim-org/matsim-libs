/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceModel.java
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;
import java.util.Map;

/**
 * Abstraction of a choice model.
 *
 * @author thibautd
 */
public interface ChoiceModel {
	/**
	 * Draws an alternative based on choice probabilities.
	 *
	 * @return the choosen alternative
	 */
	public Alternative performChoice(DecisionMaker decisionMaker, List<Alternative> alternatives);

	/**
	 * Computes the choice probabilities
	 *
	 * @return a map associating alternatives with their probabilities (the sum
	 * must be 1)
	 */
	public Map<Alternative, Double> getChoiceProbabilities();

	public DecisionMakerFactory getDecisionMakerFactory();
	public ChoiceSetFactory getChoiceSetFactory();
	public double getSystematicUtility(DecisionMaker decisionMaker, Alternative alternative);
}

