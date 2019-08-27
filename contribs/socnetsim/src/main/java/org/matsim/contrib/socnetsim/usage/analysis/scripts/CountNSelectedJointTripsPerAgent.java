/* *********************************************************************** *
 * project: org.matsim.*
 * CountNSelectedJointTripsPerAgent.java
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
package org.matsim.contrib.socnetsim.usage.analysis.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;

/**
 * @author thibautd
 */
public class CountNSelectedJointTripsPerAgent {
	public static void main(final String[] args) {
		final String popFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new PopulationReader( sc ).readFile( popFile );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );

		try {
			writer.write( "id\tnPassenger\tnDriver\tnJoint" );

			for (Person p : sc.getPopulation().getPersons().values()) {
				final List<Trip> trips =
					TripStructureUtils.getTrips(p.getSelectedPlan());

				int countPassenger = 0;
				int countDriver = 0;
				for (Trip trip : trips) {
					final boolean isPassenger = containsMode( trip , JointActingTypes.PASSENGER );
					final boolean isDriver = containsMode( trip , JointActingTypes.DRIVER );
					assert !(isPassenger && isDriver);
					if (isPassenger) countPassenger++;
					if (isDriver) countDriver++;
				}

				writer.newLine();
				writer.write( p.getId()+"\t"+
						countPassenger+"\t"+
						countDriver+"\t"+
						(countPassenger + countDriver) );
			}

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static boolean containsMode(
			final Trip trip,
			final String mode) {
		for (Leg leg : trip.getLegsOnly()) {
			if (leg.getMode().equals( mode )) return true;
		}
		return false;
	}
}

