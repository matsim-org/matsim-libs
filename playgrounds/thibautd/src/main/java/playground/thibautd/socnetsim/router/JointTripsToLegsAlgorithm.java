/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsToLegsAlgorithm.java
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
package playground.thibautd.socnetsim.router;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.utils.RoutingUtils;

/**
 * Similar to TripsToLegsAlgorithm, but remembers joint routes
 * @author thibautd
 */
public class JointTripsToLegsAlgorithm implements PlanAlgorithm {
	private final TripRouter router;

	public JointTripsToLegsAlgorithm(final TripRouter router) {
		this.router = router;
	}

	@Override
	public void run(final Plan plan) {
		List<OrigLegDest> jointInfo = getJointInfo( plan );
		List<PlanElement> structure =
				RoutingUtils.tripsToLegs(
						plan,
						router.getStageActivityTypes(),
						new MainModeIdentifierImpl());
		reinsertJointInfo( jointInfo , structure );
		plan.getPlanElements().clear();
		plan.getPlanElements().addAll( structure );
	}

	private static List<OrigLegDest> getJointInfo(final Plan plan) {
		List<OrigLegDest> info = new ArrayList<OrigLegDest>();

		Iterator<PlanElement> pes = plan.getPlanElements().iterator();
		PlanElement last = null;
		while (pes.hasNext()) {
			PlanElement curr = pes.next();

			if (curr instanceof Leg &&
					JointActingTypes.JOINT_MODES.contains(
						((Leg) curr).getMode() ) ) {
				Leg l = (Leg) curr;
				curr = pes.next();
				info.add( new OrigLegDest( last , l , curr ) );
			}

			last = curr;
		}

		return info;
	}

	private static void reinsertJointInfo(
			final List<OrigLegDest> jointInfo,
			final List<PlanElement> structure) {
	 	for (OrigLegDest old : jointInfo) {
			List<PlanElement> t = getTrip( old , structure );
			if (t.size() != 1) throw new RuntimeException( t+" has unexpected size" );
			t.clear();
			t.add( old.leg );
		}
	}

	private static List<PlanElement> getTrip(
			final OrigLegDest old,
			final List<PlanElement> structure) {
		int o = -1;
		int i = 0;
		for (PlanElement pe : structure) {
			if (pe == old.origin) {
				o = i;
			}
			if (pe == old.destination) {
				return structure.subList( o + 1 , i );
			}
			i++;
		}
		throw new RuntimeException( old+" could not be found in "+structure );
	}

	private static class OrigLegDest {
		public final Activity origin;
		public final Leg leg;
		public final Activity destination;

		public OrigLegDest(
				final PlanElement origin,
				final Leg leg,
				final PlanElement destination) {
			this.origin = (Activity) origin;
			this.leg = leg;
			this.destination = (Activity) destination;
		}

		@Override
		public String toString() {
			return "[Orig="+origin+"; Leg="+leg+"; Dest="+destination+"]";
		}
	}
}

