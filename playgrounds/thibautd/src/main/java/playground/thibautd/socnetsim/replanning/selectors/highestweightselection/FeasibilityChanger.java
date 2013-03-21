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


final class FeasibilityChanger {
	private final List<PlanRecord> changedRecords = new ArrayList<PlanRecord>();

	public void markInfeasible( final PlanRecord r ) {
		if ( r.isStillFeasible ) changedRecords.add( r );
		r.isStillFeasible = false;
	}

	public void resetFeasibilities() {
		for ( PlanRecord r : changedRecords ) r.isStillFeasible = true;
		changedRecords.clear();
	}
}
