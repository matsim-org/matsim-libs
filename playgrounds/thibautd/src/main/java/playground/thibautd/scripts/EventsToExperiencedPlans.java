/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToExperiencedPlans.java
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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ExperiencedPlansModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import playground.thibautd.utils.EventsToPlans;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author thibautd
 */
public class EventsToExperiencedPlans {
	public static void main(final String[] args) {
		Config config = ConfigUtils.createConfig();
		final Scenario inputSc = ScenarioUtils.createScenario(config);

		com.google.inject.Injector injector = Injector.createInjector(config,
				new ScenarioByInstanceModule(inputSc),
				new ExperiencedPlansModule(),
				new CharyparNagelScoringFunctionModule(),
				new EventsManagerModule(),
				new ReplayEvents.Module());

		final String eventsFile = args[ 0 ];
		final String inPopFile = args[ 1 ];
		final String outputPlansFile = args[ 2 ];

		new PopulationReader( inputSc ).readFile( inPopFile );

		final EventsToPlans eventsToPlans =
			new EventsToPlans(
					id -> inputSc.getPopulation().getPersons().containsKey( id ) );

		EventsToActivities eventsToActivities = injector.getInstance(EventsToActivities.class);
		eventsToActivities.addActivityHandler(eventsToPlans);
		EventsToLegs eventsToLegs = injector.getInstance(EventsToLegs.class);
		eventsToLegs.addLegHandler(eventsToPlans);

		injector.getInstance(ReplayEvents.class).playEventsFile( eventsFile, 1);

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		for ( final Plan plan : eventsToPlans.getPlans().values() ) {
			plan.getPerson().addPlan( plan );
			transmitCoordinates( inputSc.getPopulation().getPersons().get( plan.getPerson().getId() ) , plan );
			sc.getPopulation().addPerson( plan.getPerson() );
		}

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPlansFile );
	}

	private static void transmitCoordinates(
			final Person person,
			final Plan plan) {
		final Collection<Activity> originalActivities = TripStructureUtils.getActivities( person.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
		final Collection<Activity> newActivities = TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE );

		assert newActivities.size() <= originalActivities.size();

		final Iterator<Activity> origIterator = originalActivities.iterator();
		final Iterator<Activity> newIterator = newActivities.iterator();

		while ( newIterator.hasNext() ) {
			final Activity origAct = origIterator.next();
			final Activity newAct = newIterator.next();

			newAct.setCoord( origAct.getCoord() );
		}
	}
}

