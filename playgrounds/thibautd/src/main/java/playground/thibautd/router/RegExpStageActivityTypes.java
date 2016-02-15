/* *********************************************************************** *
 * project: org.matsim.*
 * RegExpStageActivityChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router;

import org.matsim.core.router.StageActivityTypes;

import java.util.ArrayList;
import java.util.Collection;


/**
 * A {@link StageActivityTypes} using regular expressions. For example,
 * PT-related stages activities could be all activities which type match
 * <tt>"pt_.*"</tt>.
 *
 * @author thibautd
 */
public class RegExpStageActivityTypes implements StageActivityTypes {
	private final Collection<String> typesRegExps = new ArrayList<String>();

	/**
	 * Initialises an instance with a given list of regularExpresions
	 * @param typesRegExps a Collection containing the regular expressions to consider as stage types patterns
	 */
	public RegExpStageActivityTypes(final Collection<String> typesRegExps) {
		this.typesRegExps.addAll( typesRegExps );
	}

	@Override
	public boolean isStageActivity(final String activityType) {
		for (String regExp : typesRegExps) {
			if (activityType.matches( regExp )) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean equals(final Object other) {
		if ( other == null ) return false;
		if (other.getClass().equals( this.getClass() )) {
			return typesRegExps.equals( ((RegExpStageActivityTypes) other).typesRegExps );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return typesRegExps.hashCode();
	}
}

