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

package playground.thibautd.socnetsim.replanning.selectors.highestweightselection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;

final class KnownBranches {
	private final boolean prune;
	private final List<Set<Id>> branches = new ArrayList<Set<Id>>();

	public KnownBranches(final boolean prune) {
		this.prune = prune;
	}

	public void tagAsExplored(final Set<Id> branch) {
		if (prune) branches.add( branch );
	}

	public boolean isExplored(final Set<Id> branch) {
		return prune && branches.contains( branch );
	}
}
