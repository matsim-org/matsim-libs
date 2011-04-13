/* *********************************************************************** *
 * project: org.matsim.*
 * CarPassengerLegRouter.java
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
package playground.thibautd.jointtripsoptimizer.router;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.LegRouter;

import playground.thibautd.jointtripsoptimizer.population.JointLeg;

/**
 * {@link LegRouter} designed to set the route of a "passenger" leg, such that
 * it is consistent with the driver's route.
 *
 * More precisely, it just sets the route to null, which makes JointLeg copy the
 * driver's route at the first call of getRoute. In that way, the driver's route
 * is well defined at the time of the copy.
 *
 * @author thibautd
 */
public class CarPassengerLegRouter implements LegRouter {
	@Override
	public double routeLeg(
			final Person person,
			final Leg leg,
			final Activity fromAct,
			final Activity toAct,
			final double depTime) {
		leg.setRoute(null);
		((JointLeg) leg).routeToCopy();
		return 0d;
	}
}

