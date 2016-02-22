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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRouteFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRouteFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

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
				DriverRoute.class,//JointActingTypes.DRIVER,
				new DriverRouteFactory() );
		((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory().setRouteFactory(
				PassengerRoute.class,//JointActingTypes.PASSENGER,
				new PassengerRouteFactory() );
		new MatsimPopulationReader( scenario ).readFile( popFile );

		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		writer.write( "typePassenger\ttypeDriver\tdist" );
		for ( final Person p : scenario.getPopulation().getPersons().values() ) {
			if ( !hasOnlyOnPassengerTrip( p ) ) continue;
			final Activity passengerAct = getJointAct( p );

			final Id driverId = getDriver( p );
			final Person driver = scenario.getPopulation().getPersons().get( driverId );
			final Activity driverAct = getDriverJointAct( p , driver );

			final Activity passengerNonJoint = getOtherNonHome( p , passengerAct );
			final Activity driverNonJoint = getOtherNonHome( driver , driverAct );

			writer.newLine();
			writer.write( passengerNonJoint.getType() +"\t"+ driverNonJoint.getType() +"\t"+ CoordUtils.calcEuclideanDistance( passengerNonJoint.getCoord() , driverNonJoint.getCoord() ) );
		}
		writer.close();
	}

	private static Activity getOtherNonHome(
			final Person p,
			final Activity toExclude) {
		for ( final Activity act : TripStructureUtils.getActivities( p.getSelectedPlan() , JointActingTypes.JOINT_STAGE_ACTS ) ) {
			if ( !act.getType().equals( "h" ) && !act.equals( toExclude ) ) return act;
		}
		throw new RuntimeException();
	}

	private static Activity getDriverJointAct(
			final Person passenger,
			final Person driver) {
		for ( final Trip trip : TripStructureUtils.getTrips( driver.getSelectedPlan() , JointActingTypes.JOINT_STAGE_ACTS ) ) {
			if ( containsPassenger( trip , passenger.getId() ) ) {
				return trip.getOriginActivity().getType().equals( "h" ) ?
					trip.getDestinationActivity() :
					trip.getOriginActivity();
			}
		}
		throw new RuntimeException();
	}

	private static boolean containsPassenger(final Trip trip, final Id id) {
		for ( final Leg l : trip.getLegsOnly() ) {
			final Route route = l.getRoute();
			if ( route instanceof DriverRoute && ((DriverRoute) route).getPassengersIds().contains( id ) ) {
				return true;
			}
		}
		return false;
	}

	private static Activity getJointAct(final Person passenger) {
		for ( final Trip t : TripStructureUtils.getTrips( passenger.getSelectedPlan() , JointActingTypes.JOINT_STAGE_ACTS ) ) {
			if ( isPassengerTrip( t ) ) {
				// assume no complex trip. should be checked.
				return t.getOriginActivity().getType().equals( "h" ) ?
					t.getDestinationActivity() :
					t.getOriginActivity();
			}
		}
		throw new RuntimeException();
	}

	private static boolean isPassengerTrip(final Trip t) {
		for ( final Leg l : t.getLegsOnly() ) {
			if ( l.getMode().equals( JointActingTypes.PASSENGER ) ) return true;
		}
		return false;
	}

	private static Id getDriver(final Person passenger) {
		for ( final PlanElement pe : passenger.getSelectedPlan().getPlanElements() ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
				return ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();
			}
		}
		throw new RuntimeException();
	}

	private static boolean hasOnlyOnPassengerTrip(final Person p) {
		int c = 0;
		for ( final PlanElement pe : p.getSelectedPlan().getPlanElements() ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) c++;
		}
		return c == 1;
	}
}

