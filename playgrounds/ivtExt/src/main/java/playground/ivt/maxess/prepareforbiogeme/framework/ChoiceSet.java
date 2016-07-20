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
package playground.ivt.maxess.prepareforbiogeme.framework;

import org.matsim.api.core.v01.population.Person;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author thibautd
 */
public class ChoiceSet<T> {
	private final Person decisionMaker;
	private final String chosen;
	private final Map<String, T> namedAlternatives = new TreeMap<>();

	public ChoiceSet(
			final Person decisionMaker,
			final String chosen,
			final Map<String, T> namedAlternatives) {
		this.chosen = chosen;
		this.decisionMaker = decisionMaker;
		this.namedAlternatives.putAll( namedAlternatives );
	}

	public Person getDecisionMaker() {
		return decisionMaker;
	}

	public T getChosenAlternative() {
		return namedAlternatives.get( chosen );
	}

	public String getChosenName() {
		return chosen;
	}

	public Map<String, T> getNamedAlternatives() {
		return namedAlternatives;
	}

	public T getAlternative(final String name) {
		return namedAlternatives.get( name );
	}
}
