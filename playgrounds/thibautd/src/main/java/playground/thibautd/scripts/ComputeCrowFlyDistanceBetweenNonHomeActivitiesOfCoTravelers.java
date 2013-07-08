/* *********************************************************************** *
 * project: org.matsim.*
 * ComputeCrowFlyDistanceBetweenNonHomeActivitiesOfCoTravelers.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.socnetsim.population.DriverRouteFactory;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.population.PassengerRouteFactory;

/**
 * looks at co travelers with only one joint trip in a 3 acts plan, and computes the crow fly distance
 * between their non home activity which is  not origin or destination of the jt.
 * @author thibautd
 */
public class ComputeCrowFlyDistanceBetweenNonHomeActivitiesOfCoTravelers {
	public static void main(final String[] args) throws IOException {
		final String popFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory().setRouteFactory(
				JointActingTypes.DRIVER,
				new DriverRouteFactory() );
		((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory().setRouteFactory(
				JointActingTypes.PASSENGER,
				new PassengerRouteFactory() );
		new MatsimPopulationReader( scenario ).readFile( popFile );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "type\tdist" );
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			if ( !hasOnlyOnPassengerTrip( p ) ) continue;
			final Activity passengerAct = getNonJointAct( p );
			final Activity driverAct = getDriverAct( p , scenario.getPopulation() );

			writer.newLine();
			writer.write( passengerAct.getType() +"\t"+ CoordUtils.calcDistance( passengerAct.getCoord() , driverAct.getCoord() ) );
		}
		writer.close();
	}

	private static Activity getDriverAct(
			final Person passenger,
			final Population population) {
		final Id driverId = getDriver( passenger );
		final Person driver = population.getPersons().get( driverId );

		final Activity pAct = getNonJointAct( passenger );

		for ( Activity act : TripStructureUtils.getActivities( driver.getSelectedPlan() , JointActingTypes.JOINT_STAGE_ACTS ) ) {
			if ( act.getType().equals( pAct.getType() ) ) return act;
		}
		throw new RuntimeException();
	}

	private static Activity getNonJointAct(final Person passenger) {
		for ( Trip t : TripStructureUtils.getTrips( passenger.getSelectedPlan() , JointActingTypes.JOINT_STAGE_ACTS ) ) {
			if ( t.getLegsOnly().size() == 1 ) {
				// assume no complex trip. should be checked.
				return t.getOriginActivity().getType().equals( "h" ) ?
					t.getDestinationActivity() :
					t.getOriginActivity();
			}
		}
		throw new RuntimeException();
	}

	private static Id getDriver(final Person passenger) {
		for ( PlanElement pe : passenger.getSelectedPlan().getPlanElements() ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
				return ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();
			}
		}
		throw new RuntimeException();
	}

	private static boolean hasOnlyOnPassengerTrip(final Person p) {
		int c = 0;
		for ( PlanElement pe : p.getSelectedPlan().getPlanElements() ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) c++;
		}
		return c == 1;
	}
}

