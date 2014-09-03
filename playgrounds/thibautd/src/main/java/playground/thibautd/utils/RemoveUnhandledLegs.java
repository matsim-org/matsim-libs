/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveUnhandledLegs.java
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
package playground.thibautd.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * Simple utility, parsing a plan file and setting mode of all
 * legs which mode does not pertains to {car,pt,walk,bike} to
 * pt.
 *
 * @author thibautd
 */
public class RemoveUnhandledLegs {
	private static final Collection<String> VALID_MODES =
		Arrays.asList( new String[]{
			TransportMode.car,
			TransportMode.pt,
			TransportMode.walk,
			TransportMode.bike,
		} );

	public static void main( final String[] args) {
		String configFile = args[ 0 ];
		Config config = ConfigUtils.loadConfig( configFile );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			for ( Plan plan : person.getPlans() ) {
				for ( PlanElement pe : plan.getPlanElements() ) {
					if (pe instanceof Leg &&
							!VALID_MODES.contains( ((Leg) pe).getMode() ) ) {
						((Leg) pe).setMode( TransportMode.pt );
					}
				}
			}
		}

		PopulationWriter popWriter = new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork()
        );

		String popFile = config.plans().getInputFile();
		String outputFile =
			popFile.matches(".*.xml.gz") ?
			popFile.substring( 0 , popFile.length() - 7) + "-no-unhandled-leg.xml.gz" :
			popFile.substring( 0 , popFile.length() - 4) + "-no-unhandled-leg.xml";

		popWriter.write( outputFile );
	}
}

