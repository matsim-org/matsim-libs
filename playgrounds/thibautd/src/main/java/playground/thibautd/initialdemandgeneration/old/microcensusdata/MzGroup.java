/* *********************************************************************** *
 * project: org.matsim.*
 * MzGroup.java
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
package playground.thibautd.initialdemandgeneration.old.microcensusdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

/**
 * Container representing an homogeneous group, from which random elements can be drawn
 * @author thibautd
 */
class MzGroup {
	private boolean isModifiable = true;
	private double totalWeight = 0;

	private final Id id;
	private final List< Tuple<Double , Person> > values =
			new ArrayList< Tuple<Double , Person> >();
	private final Random random = new Random( 1993467 );

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	MzGroup( final Id id ) {
		this.id = id;
	}

	// /////////////////////////////////////////////////////////////////////////
	// group construction methods
	// /////////////////////////////////////////////////////////////////////////
	void add( final Person person ) {
		if (!isModifiable) throw new IllegalStateException( "persons cannot be added after the first draw" );

		double weight = person.getSelectedPlan().getScore();
		totalWeight += weight;

		values.add( new Tuple< Double , Person >(
					weight,
					person) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// get
	// /////////////////////////////////////////////////////////////////////////
	public Person getRandomWeightedPerson() {
		isModifiable = false;

		if (values.size() == 0) throw new RuntimeException( "try to draw from empty group "+id );

		double choice = random.nextDouble();
		double cumul = 1E-7;

		for (Tuple< Double , Person > person : values) {
			cumul += person.getFirst() / totalWeight;
			if (choice < cumul) return person.getSecond();
		}

		throw new RuntimeException( "problem while drawing a random person" );
	}

	public int size() {
		return values.size();
	}
}

