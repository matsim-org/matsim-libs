/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveJointTrips.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * @author thibautd
 */
public class RemoveJointTrips {
	public static void main(final String[] args) {
		Config config = ConfigUtils.loadConfig( args[ 0 ] );
		Scenario scenario = ScenarioUtils.loadScenario( config );

		removeJointTrips( scenario.getPopulation() );

		String outputFile = config.plans().getInputFile();

		if (outputFile.matches(".*.xml.gz")) {
			outputFile = outputFile.substring( 0 , outputFile.length() - 7) + "-wo-jt.xml.gz";
		}
		else {
			outputFile = outputFile.substring( 0 , outputFile.length() - 4) + "-wo-jt.xml";
		}

		(new PopulationWriter(
				scenario.getPopulation(),
				scenario.getNetwork()) ).write( outputFile );
	}

	public static void removeJointTrips(final Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> inPlanElements = plan.getPlanElements();
				List<PlanElement> constructedPlanElements = new ArrayList<PlanElement>();

				// step through plan, and retain only plan elements not in a shared ride.
				// the first access leg is retained.
				boolean inJointTrip = false;
				for (PlanElement element : inPlanElements) {
					if (inJointTrip) {
						if (element instanceof Activity) {
							String type = ((Activity) element).getType();

							if ( !type.matches( JointActingTypes.PICK_UP_REGEXP ) &&
									!type.equals( JointActingTypes.PICK_UP ) &&
									!type.equals( JointActingTypes.DROP_OFF ) ) {
								constructedPlanElements.add( element );
								inJointTrip = false;
							}
						}
					}
					else {
						if (element instanceof Activity) {
							String type = ((Activity) element).getType();

							if ( type.matches( JointActingTypes.PICK_UP_REGEXP ) ||
									type.equals( JointActingTypes.PICK_UP ) ) {
								inJointTrip = true;
							}
							else {
								constructedPlanElements.add( element );
							}
						}
						else {
							constructedPlanElements.add( element );
						}
					}
				}

				// update plan
				inPlanElements.clear();
				inPlanElements.addAll( constructedPlanElements );
			}
		}
	}
}

