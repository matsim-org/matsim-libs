/* *********************************************************************** *
 * project: org.matsim.*
 * MzGroupsModule.java
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
package playground.thibautd.initialdemandgeneration.microcensusdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Module;
import org.matsim.core.population.PersonImpl;

/**
 * Allows to import stratification imformation from config file
 * @author thibautd
 */
public class MzGroupsModule extends Module {
	private static final long serialVersionUID = 1L;

	public static final String NAME = "MzGroups";

	private boolean isModifiable = true;

	// field names
	public static final String AGE_THRESHOLD_REGEXP = "ageThreshold.*";
	public static final String STRATIFY_BY_GENDER = "stratifyByGender";
	public static final String STRATIFY_BY_LICENCE = "stratifyByLicense";
	public static final String STRATIFY_BY_WORK = "stratifyByWork";
	public static final String STRATIFY_BY_EDUC = "stratifyByEduc";

	// values
	private boolean stratifyByGender = true;
	private boolean stratifyByLicence = true;
	private boolean stratifyByWork = true;
	private boolean stratifyByEduc = true;
	private final AgeThresholds thresholds = new AgeThresholds();

	// "group container" related fields
	private Map<GroupId, MzGroup> groups = new HashMap<GroupId, MzGroup>();

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public MzGroupsModule() {
		super( NAME );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Module interface
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void addParam(final String name , final String value) {
		if (!isModifiable) throw new IllegalStateException( "parameters cannot be changed anymore" );

		if (name.matches( AGE_THRESHOLD_REGEXP )) {
			thresholds.addThreshold( value );
		}
		else if ( name.equals( STRATIFY_BY_GENDER ) ) {
			stratifyByGender = Boolean.getBoolean( value );
		}
		else if ( name.equals( STRATIFY_BY_LICENCE ) ) {
			stratifyByLicence = Boolean.getBoolean( value );
		}
		else if ( name.equals( STRATIFY_BY_WORK ) ) {
				stratifyByWork = Boolean.getBoolean( value );
		}
		else if ( name.equals( STRATIFY_BY_EDUC ) ) {
				stratifyByEduc = Boolean.getBoolean( value );
		}
	}

	//TODO: implement the superclass getters
	
	// /////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////
	public String getDescription() {
		return "{MzGroupsModule : "+
			"ages : "+thresholds+
			"; strat. by gender: "+stratifyByGender+
			"; strat. by licence: "+stratifyByLicence+
			"; strat. by work: "+stratifyByWork+
			"; strat. by educ: "+stratifyByEduc+"}";
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public MzGroup getGroup(
				final int age,
				final String gender,
				final String licence,
				final boolean work,
				final boolean educ) {
		GroupId id = new GroupId( age , gender , licence , work , educ );
		MzGroup group = groups.get( id );

		if (group == null) {
			group = new MzGroup( id );
			groups.put( id , group );
		}

		return group;
	}

	public MzGroup getGroup(
			final PersonImpl person) {
		return getGroup(
				person.getAge(),
				person.getSex(),
				person.getLicense(),
				person.isEmployed(),
				(person.getDesires() != null &&
				 person.getDesires().getActivityDuration("e") > 0));
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested classes
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Associated a group id to a set of individual characteristic.
	 */
	private class GroupId implements Id {
		private final int ageCategory;
		private final String gender;
		private final String licence;
		private final boolean work;
		private final boolean educ;

		public GroupId(
				final int age,
				final String gender,
				final String licence,
				final boolean work,
				final boolean educ) {
			this.ageCategory = thresholds.getCategoryIndex( age );
			this.gender = gender;
			this.licence = licence;
			this.work = work;
			this.educ = educ;
		}

		@Override
		public String toString() {
			return "[group: age="+ageCategory+
				( stratifyByGender ? "; gender="+gender : "" )+
				( stratifyByLicence ? "; licence="+licence : "" )+
				( stratifyByWork ? "; work="+work : "" )+
				( stratifyByEduc ? "; educ="+educ : "" )+
				"]";
		}

		@Override
		public int hashCode() {
			return ageCategory +
				( stratifyByGender && gender.equals( "m" ) ? 1000 : 0 ) +
				( stratifyByLicence && licence.equals( "yes" ) ? 10000 : 0 ) +
				( stratifyByWork && work ? 100000 : 0 ) +
				( stratifyByEduc && educ ? 1000000 : 0 );
		}

		@Override
		public boolean equals( final Object o) {
			if ( o instanceof GroupId ) {
				GroupId other = (GroupId) o;
				return ageCategory == other.ageCategory &&
					( !stratifyByGender || gender.equals( other.gender ) ) &&
					( !stratifyByLicence || licence.equals( other.licence ) ) &&
					( !stratifyByWork || work == other.work ) &&
					( !stratifyByEduc || educ == other.educ );
			}
			return false;
		}

		@Override
		public int compareTo(final Id other) {
			return hashCode() - other.hashCode();
		}
	}
}

class AgeThresholds {
	private final List<Integer> thresholds = new ArrayList<Integer>();
	private boolean isDefault = true;
	private boolean isLocked = false;

	public AgeThresholds() {
		thresholds.add( 0 );
		thresholds.add( 7 );
		thresholds.add( 15 );
		thresholds.add( 18 );
		thresholds.add( 66 );
	}

	public void clear() {
		if (isLocked) throw new IllegalStateException( "age thresholds are not modifiable anymore" );
		thresholds.clear();
		thresholds.add( 0 );
	}

	public void addThreshold( final String value ) {
		if (isLocked) throw new IllegalStateException( "age thresholds are not modifiable anymore" );
		if (isDefault) clear();

		isDefault = false;
		thresholds.add( Integer.parseInt( value ) );
		Collections.sort( thresholds );
	}

	public int getCategoryIndex( final int age ) {
		// starting to acces values locks the possibility to modify them
		isLocked = true;
		int i = 0;

		for (int threshold : thresholds) {
			if (age >= threshold) {
				return i;
			}
			i++;
		}

		throw new RuntimeException( "could not find age category for age "+age );
	}

	@Override
	public String toString() {
		return "[ageThresholds: "+thresholds.toString()+"]";
	}
}


