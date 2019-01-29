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

package org.matsim.contrib.socnetsim.framework.replanning.selectors.whoisthebossselector;

import java.util.ArrayList;
import java.util.List;


final class FeasibilityChanger {
	private final boolean changeTo;
	private final List<PlanRecord> changedRecords = new ArrayList<PlanRecord>();

	public FeasibilityChanger() {
		this( false );
	}

	public FeasibilityChanger(final boolean changeTo) {
		this.changeTo = changeTo;
	}

	public void changeIfNecessary( final PlanRecord r ) {
		if ( r.isStillFeasible != changeTo ) {
			changedRecords.add( r );
			r.isStillFeasible = changeTo;
		}
	}

	public void resetFeasibilities() {
		for ( PlanRecord r : changedRecords ) {
			r.isStillFeasible = !changeTo;
		}
		changedRecords.clear();
	}
}
