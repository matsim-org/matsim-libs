/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemoveUnhandledModes.java
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
package playground.thibautd.initialdemandgeneration.old.modules;

import java.util.Arrays;
import java.util.Collection;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PersonAlgorithm;

/**
 * Sets the mode of legs with a mode not in {car,pt,walk,bike}
 * to pt.
 * @author thibautd
 */
public class PersonRemoveUnhandledModes implements PersonAlgorithm {
	private static final Collection<String> VALID_MODES =
		Arrays.asList( new String[]{
			TransportMode.car,
			TransportMode.pt,
			TransportMode.walk,
			TransportMode.bike,
		} );

	@Override
	public void run(final Person person) {
		for ( Plan plan : person.getPlans() ) {
			for ( PlanElement pe : plan.getPlanElements() ) {
				if (pe instanceof Leg &&
						!VALID_MODES.contains( ((Leg) pe).getMode() ) ) {
					((Leg) pe).setMode( TransportMode.pt );
				}
			}
		}	
	}
}

