/* *********************************************************************** *
 * project: org.matsim.*
 * UnsynchronizeHouseholdMembers.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;

import playground.thibautd.socnetsim.usage.JointScenarioUtils;

/**
 * @author thibautd
 */
public class UnsynchronizeHouseholdMembers {
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];
		final String outPlansFile = args[ 1 ];
		final Config config = JointScenarioUtils.loadConfig( configFile );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		for ( Household hh : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values() ) {
			final int nMorning = hh.getMemberIds().size() / 2;
			int i=0;
			for ( Id member : hh.getMemberIds() ) {
				for ( Plan p : scenario.getPopulation().getPersons().get( member ).getPlans() ) {
					setTimes( p , i < nMorning );
				}
				i++;
			}
		}

		new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( outPlansFile );
	}

	private static void setTimes(
			final Plan p,
			final boolean isMorning) {
		double now = isMorning ? 3600 : 13 * 3600;
		for ( Activity act : TripStructureUtils.getActivities( p , EmptyStageActivityTypes.INSTANCE ) ) {
			if ( act.getEndTime() == Time.UNDEFINED_TIME ) continue;
			act.setEndTime( now );
			now += 3600;
		}
	}
}

