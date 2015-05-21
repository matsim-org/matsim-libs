/* *********************************************************************** *
 * project: org.matsim.*
 * DriverPassengerODCoordinatesExtractor.java
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
package playground.thibautd.analysis.coordinatesextrators;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;

/**
 * @author thibautd
 */
public class DriverPassengerODCoordinatesExtractor {
	private static final String PU_REGEXP = "p.*";
	
	private final List<Coord> passengerOrigins = new ArrayList<Coord>();
	private final List<Coord> passengerDestinations = new ArrayList<Coord>();
	private final List<Coord> driverOrigins = new ArrayList<Coord>();
	private final List<Coord> driverDestinations = new ArrayList<Coord>();
	
	public DriverPassengerODCoordinatesExtractor(
			//final Scenario scenario) {
			final Population population) {
		//Population population = scenario.getPopulation();

		for (Person person : population.getPersons().values()) {
			PlanElement[] elements = person.getSelectedPlan().getPlanElements().toArray(new PlanElement[0]);
			if (elements.length <= 3) continue;

			List<Coord> currentList;
			Activity relevantActivity;
			for (int i = 2; i < elements.length; i += 2) {
				currentList = null;
				relevantActivity = null;

				if ( ((Activity) elements[i]).getType().matches( PU_REGEXP ) ) {
					if ( ((Leg) elements[i + 1]).getMode().equals( JointActingTypes.PASSENGER ) ) {
						currentList = passengerOrigins;
					}
					else {
						currentList = driverOrigins;
					}
					relevantActivity = (Activity) elements[i - 2];
				}
				else if ( ((Activity) elements[i]).getType().equals( JointActingTypes.INTERACTION ) ) {
					if ( ((Leg) elements[i - 1]).getMode().equals( JointActingTypes.PASSENGER ) ) {
						currentList = passengerDestinations;
					}
					else {
						currentList = driverDestinations;
					}
					relevantActivity = (Activity) elements[i + 2];
				}

				if ( currentList != null ) {
					currentList.add( relevantActivity.getCoord() );
				}
			}
		}
	}

	public void writeCoords(final String prefix) {
		CoordinatesWriter.write(passengerOrigins, prefix+"passengerOrigins.xy");
		CoordinatesWriter.write(driverOrigins, prefix+"driverOrigins.xy");
		CoordinatesWriter.write(passengerDestinations, prefix+"passengerDestinations.xy");
		CoordinatesWriter.write(driverDestinations, prefix+"driverDestinations.xy");
	}

	public static void main(final String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.loadScenario(
				 config );

		DriverPassengerODCoordinatesExtractor extractor =
			new DriverPassengerODCoordinatesExtractor(scenario.getPopulation());
		extractor.writeCoords( config.controler().getOutputDirectory());
	}
}

