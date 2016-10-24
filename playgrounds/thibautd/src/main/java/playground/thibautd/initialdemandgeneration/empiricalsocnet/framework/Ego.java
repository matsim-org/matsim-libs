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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class Ego {
	private final Person person;
	private final int degree;
	private final Set<Ego> alters = new HashSet<>();

	// for special requirements
	private final Object additionalInformation;

	public Ego( final Person person, final int degree ) {
		this( person , degree , null );
	}

	public Ego( final Person person, final int degree, final Object additionalInformation ) {
		this.person = person;
		this.degree = degree;
		this.additionalInformation = additionalInformation;
	}

	public Id<Person> getId() {
		return getPerson().getId();
	}

	public Person getPerson() {
		return person;
	}

	public int getDegree() {
		return degree;
	}

	public int getFreeStubs() {
		return degree - alters.size();
	}

	public Set<Ego> getAlters() {
		return alters;
	}

	public Object getAdditionalInformation() {
		return additionalInformation;
	}
}
