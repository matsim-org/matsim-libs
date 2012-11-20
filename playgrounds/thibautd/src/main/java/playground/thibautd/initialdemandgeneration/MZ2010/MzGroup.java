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
package playground.thibautd.initialdemandgeneration.MZ2010;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Container representing an homogeneous group, from which random elements can be drawn
 * @author thibautd
 */
class MzGroup {
	private boolean isModifiable = true;
	private double totalWeight = 0;

	private final List< Tuple<Double , Person> > values =
			new ArrayList< Tuple<Double , Person> >();
	private final Random random = new Random( 1993467 );

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	private final int ageMin;
	private final int ageMax;
	private final Gender gender;
	private final boolean isEmployed;
	private final boolean isInEducation;
	private final boolean hasDrivingLicense;
	private final boolean hasCarAvail;

	public MzGroup(
			final int ageMin,
			final int ageMax,
			final Gender gender,
			final boolean isEmployed,
			final boolean isInEducation,
			final boolean hasDrivingLicense,
			final boolean hasCarAvail) {
		this.ageMin = ageMin;
		this.ageMax = ageMax;
		this.gender = gender;
		this.isEmployed = isEmployed;
		this.isInEducation = isInEducation;
		this.hasDrivingLicense =hasDrivingLicense;
		this.hasCarAvail = hasCarAvail;
	}

	// /////////////////////////////////////////////////////////////////////////
	// group construction methods
	// /////////////////////////////////////////////////////////////////////////
	boolean add( final ObjectAttributes atts, final Person person ) {
		if ( !isModifiable ) throw new IllegalStateException( "persons cannot be added after the first draw" );

		if ( !contains( atts , person ) ) return false;

		double weight = (Double) atts.getAttribute(
				person.getId().toString(),
				SimplifyObjectAttributes.WEIGHT );
		totalWeight += weight;

		values.add( new Tuple< Double , Person >(
					weight,
					person) );

		return true;
	}

	public boolean contains(
			final ObjectAttributes atts,
			final Person person) {
		String id = person.getId().toString();

		double age = (Double) atts.getAttribute( id , SimplifyObjectAttributes.AGE );
		if ( age < ageMin || age >= ageMax ) return false;
		
		return gender.equals( atts.getAttribute( id , SimplifyObjectAttributes.GENDER ) ) &&
			atts.getAttribute( id , SimplifyObjectAttributes.IS_EMPLOYED ).equals( isEmployed ) &&
			atts.getAttribute( id , SimplifyObjectAttributes.IS_IN_EDUCATION ).equals( isInEducation ) &&
			atts.getAttribute( id , SimplifyObjectAttributes.LICENSE ).equals( hasDrivingLicense ) &&
			atts.getAttribute( id , SimplifyObjectAttributes.CAR_AVAIL ).equals( hasCarAvail );
	}

	// /////////////////////////////////////////////////////////////////////////
	// get
	// /////////////////////////////////////////////////////////////////////////
	public Person getRandomWeightedPerson() {
		isModifiable = false;

		if (values.size() == 0) throw new RuntimeException( "try to draw from empty group "+this );

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

	@Override
	public String toString() {
		return "{age=["+ageMin+", "+ageMax+"]; gender="+gender+
			"; employement="+isEmployed+"; education="+isInEducation+
			"; drivingLicense="+hasDrivingLicense+"; carAvail="+hasCarAvail+"}";
	}
}

