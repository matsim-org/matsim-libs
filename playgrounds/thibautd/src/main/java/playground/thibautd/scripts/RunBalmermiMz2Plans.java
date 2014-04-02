/* *********************************************************************** *
 * project: org.matsim.*
 * RunBalmermiMz2Plans.java
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
//package playground.thibautd.scripts;
//
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.api.core.v01.population.PopulationWriter;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.misc.Time;
//
//import playground.balmermi.mz.PlansCreateFromMZ;
//
///**
// * Just a script with more handy command line args (mb uses a config)
// * @author thibautd
// */
//public class RunBalmermiMz2Plans {
//	public static void main(final String[] args) throws Exception {
//		final String inputWegekettenFile = args[ 0 ];
//		final String outputPopulationFile = args[ 1 ];
//		final int firstDow = Integer.parseInt( args[ 2 ] );
//		final int lastDow = Integer.parseInt( args[ 3 ] );
//
//		final PlansCreateFromMZ algo =
//			new PlansCreateFromMZ(
//					inputWegekettenFile,
//					null, // output *wegeketten* file, unused
//					firstDow,
//					lastDow);
//
//		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
//		algo.run( sc.getPopulation() );
//
//		final StringBuilder name = new StringBuilder( "playground.thibautd.scripts.RunBalmermiMz2Plans" );
//		for ( String arg : args ) name.append( " "+arg );
//
//		sc.getPopulation().setName( name.toString() );
//
//		removeRoutes( sc.getPopulation() );
//		removeMaxDurs( sc.getPopulation() );
//		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPopulationFile );
//	}
//
//	private static void removeMaxDurs(final Population population) {
//		// when no start time, leads to invalid numbers which crash at import
//		for ( Person person : population.getPersons().values() ) {
//			for ( Plan plan : person.getPlans() ) {
//				for ( PlanElement pe : plan.getPlanElements() ) {
//					if ( pe instanceof Activity ) ((Activity) pe).setMaximumDuration( Time.UNDEFINED_TIME );
//				}
//			}
//		}
//	}
//
//	private static void removeRoutes(final Population population) {
//		// causes crash at writing because of no links
//		for ( Person person : population.getPersons().values() ) {
//			for ( Plan plan : person.getPlans() ) {
//				for ( PlanElement pe : plan.getPlanElements() ) {
//					if ( pe instanceof Leg ) ((Leg) pe).setRoute( null );
//				}
//			}
//		}
//	}
//}
//
