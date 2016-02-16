/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkedPopulationDilutionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.transformation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author thibautd
 */
public class SocialNetworkedPopulationDilutionUtils {
	private static final Logger log =
		Logger.getLogger(SocialNetworkedPopulationDilutionUtils.class);

	public static enum DilutionType {allContacts, areaOnly, leisureOnly};

	public static void dilute(
			final DilutionType dilutionType,
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		switch ( dilutionType ) {
		case allContacts:
			dilute( scenario , center , radius );
			break;
		case areaOnly:
			diluteAreaOnly( scenario , center , radius );
			break;
		case leisureOnly:
			diluteLeisureOnly( scenario , center , radius );
			break;
		default:
			throw new IllegalArgumentException( dilutionType.toString() );
		}
	}

	/**
	 * Dilutes the population of the scenario, by retaining only agents passing
	 * by the area defined by center and radius, as well as their social contacts.
	 * TODO: try also only retaining social contacts of agents living or working in the zone
	 * (not agents passing by).
	 */
	public static void dilute(
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		log.info( "Start dilution with center "+center+" and radius "+radius );
		final Set<Id<Person>> personsToKeep = new HashSet<>();
		fillSetWithIntersectingPersons(
				personsToKeep,
				scenario,
				center,
				radius );
		fillSetWithAltersOfSet(
				personsToKeep,
				scenario );
		final Collection<Id<Person>> pruned =
			prunePopulation(
				scenario,
				personsToKeep );
		pruneSocialNetwork( pruned , scenario );
		log.info( "Finished dilution." );
	}

	/**
	 * Dilutes the population of the scenario, by retaining only agents passing
	 * by the area defined by center and radius, as well as their social contacts.
	 * For social contacts not part of the "dilution", they are only kept if they,
	 * as well as the ego, have a leisure activity.
	 */
	public static void diluteLeisureOnly(
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		log.info( "Start dilution with center "+center+" and radius "+radius );
		final Set<Id<Person>> personsToKeep = new HashSet<>();
		fillSetWithIntersectingPersons(
				personsToKeep,
				scenario,
				center,
				radius );
		fillSetWithLeisureAltersOfSet(
				personsToKeep,
				scenario );
		final Collection<Id<Person>> pruned =
			prunePopulation(
				scenario,
				personsToKeep );
		pruneSocialNetwork( pruned , scenario );
		log.info( "Finished dilution." );
	}

	/**
	 * The "normal" dilution (only intersecting agents are kept), also taking care
	 * of pruning the social network.
	 */
	public static void diluteAreaOnly(
			final Scenario scenario,
			final Coord center,
			final double radius ) {
		log.info( "Start dilution with center "+center+" and radius "+radius );
		final Set<Id<Person>> personsToKeep = new HashSet<>();
		fillSetWithIntersectingPersons(
				personsToKeep,
				scenario,
				center,
				radius );
		final Collection<Id<Person>> pruned =
			prunePopulation(
				scenario,
				personsToKeep );
		pruneSocialNetwork( pruned , scenario );
		log.info( "Finished dilution." );
	}

	private static void pruneSocialNetwork(
			final Collection<Id<Person>> toPrune,
			final Scenario scenario) {
		final SocialNetworkImpl sn = (SocialNetworkImpl)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		
		log.info( "Pruning of the social network begins." );
		log.info( sn.getEgos().size()+" egos." );

		for ( Id id : toPrune ) sn.removeEgo( id );

		log.info( "Pruning of the social network finished." );
		log.info( sn.getEgos().size()+" egos remaining." );
	}

	private static Collection<Id<Person>> prunePopulation(
			final Scenario scenario,
			final Set<Id<Person>> personsToKeep) {
		final Population population = scenario.getPopulation();
		log.info( "Actual pruning of the population begins." );
		log.info( "Population size: "+population.getPersons().size() );
		log.info( "Remaining persons to keep: "+personsToKeep.size() );

		// XXX this is not guaranteed to remain feasible...
		final Iterator<Id<Person>> popit = population.getPersons().keySet().iterator();
		final Collection<Id<Person>> pruned = new ArrayList<>();
		while ( popit.hasNext() ) {
			final Id<Person> curr = popit.next();
			if ( !personsToKeep.remove( curr ) ) {
				popit.remove();
				pruned.add( curr );
			}
		}

		log.info( "Actual pruning of the population finished." );
		log.info( "Population size: "+population.getPersons().size() );
		log.info( "Pruned: "+pruned.size() );
		log.info( "Remaining persons to keep: "+personsToKeep.size() );

		return pruned;
	}

	private static void fillSetWithAltersOfSet(
			final Set<Id<Person>> personsToKeep,
			final Scenario scenario) {
		log.info( "Search for alters of identified persons" ); 

		final SocialNetwork sn = (SocialNetwork)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		if ( !sn.isReflective() ) throw new IllegalArgumentException( "results undefined with unreflexive network." );

		final Collection<Id<Person>> alters = new ArrayList<>();
		for ( Id<Person> ego : personsToKeep ) alters.addAll( sn.getAlters( ego ) );

		personsToKeep.addAll( alters );

		log.info( "Finished search for alters of identified persons" ); 
		log.info( personsToKeep.size()+" agents identified in total over "+scenario.getPopulation().getPersons().size() );
	}

	private static void fillSetWithLeisureAltersOfSet(
			final Set<Id<Person>> personsToKeep,
			final Scenario scenario) {
		log.info( "Search for LEISURE alters of identified persons" ); 

		final SocialNetwork sn = (SocialNetwork)
			scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		if ( !sn.isReflective() ) throw new IllegalArgumentException( "results undefined with unreflexive network." );

		final Set<Id<Person>> withLeisure = identifyAgentsWithLeisure( scenario );

		final Collection<Id<Person>> alters = new ArrayList<>();
		for ( Id<Person> ego : personsToKeep ) {
			if ( !withLeisure.contains( ego ) ) continue; // only consider ties potentially activated

			for ( Id<Person> alter : sn.getAlters( ego ) ) {
				if ( !withLeisure.contains( alter ) ) continue; // only consider ties potentially activated
				alters.add( alter );
			}
		}

		personsToKeep.addAll( alters );

		log.info( "Finished search for alters of identified persons" ); 
		log.info( personsToKeep.size()+" agents identified in total over "+scenario.getPopulation().getPersons().size() );
	}

	private static Set<Id<Person>> identifyAgentsWithLeisure(final Scenario scenario) {
		final Set<Id<Person>> agents = new HashSet<>();
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			assert plan != null : person.getId();

			for ( Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE ) ) {
				// TODO: less hardcoded
				if ( act.getType().equals( "leisure" ) ) {
					agents.add( person.getId() );
					break;
				}
			}
		}

		return agents;
	}

	private static void fillSetWithIntersectingPersons(
			final Set<Id<Person>> personsToKeep,
			final Scenario scenario,
			final Coord center,
			final double radius) {
		log.info( "Search for intersecting persons" );
		log.warn( "Only using crowfly intersection (not network routes)" );

		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			if ( accept( p , center , radius ) ) personsToKeep.add( p.getId() );
		}

		log.info( "Finished search for intersecting persons" );
		log.info( personsToKeep.size()+" agents identified over "+scenario.getPopulation().getPersons().size() );
	}

	private static boolean accept(
			final Person p,
			final Coord center,
			final double radius) {
		Coord lastCoord = null;

		for ( Activity activity : TripStructureUtils.getActivities( p.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE ) ) {
			if ( activity.getCoord() == null ) throw new NullPointerException( "no coord for activity "+activity+" for person "+p );

			if ( CoordUtils.calcEuclideanDistance( center , activity.getCoord() ) < radius ) return true;

			if ( lastCoord != null &&
					CoordUtils.distancePointLinesegment(
						lastCoord , activity.getCoord(),
						center ) < radius ) return true;
			lastCoord = activity.getCoord();
		}

		return false;
	}
}


