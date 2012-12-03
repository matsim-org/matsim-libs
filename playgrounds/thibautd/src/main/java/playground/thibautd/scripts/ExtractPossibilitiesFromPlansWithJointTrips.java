/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractPossibilitiesFromPlansWithJointTrips.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.cliquessim.population.Clique;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Od;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilities.Possibility;
import playground.thibautd.cliquessim.population.jointtrippossibilities.JointTripPossibilitiesXMLWriter;
import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class ExtractPossibilitiesFromPlansWithJointTrips {
	private static final Logger log =
		Logger.getLogger(ExtractPossibilitiesFromPlansWithJointTrips.class);

	public static void main(final String[] args) {
		log.warn( "this converter assumes the PU and DO for the passenger are at its origin and destination!" );
		String confFile = args[ 0 ];
		String outFile = args[ 1 ];
		Scenario sc = JointControlerUtils.createScenario( confFile );

		JointTripPossibilities poss = new JointTripPossibilities( "from "+sc.getPopulation().getName() );

		Counter counter = new Counter( "analysing clique # " );
		for (Clique cl : JointControlerUtils.getCliques( sc ).getCliques().values()) {
			counter.incCounter();
			JointPlan plan = (JointPlan) cl.getSelectedPlan();
			TripCount tripCount = new TripCount();

			for (Map.Entry<Id, Plan> entry : plan.getIndividualPlans().entrySet() ) {
				Id driver = entry.getKey();
				Activity origin = null;
				DriverRoute driverRoute = null;
				for (PlanElement driverEl : entry.getValue().getPlanElements()) {
					if (driverEl instanceof Activity) {
						String type = ((Activity) driverEl).getType();
						if (type.equals( JointActingTypes.PICK_UP ) ||
								type.equals( JointActingTypes.DROP_OFF )) {
							continue;
						}
						else if (driverRoute != null) {
							// we are at the end of a joint trip
							Od driverOd = Od.create( origin.getLinkId() , ((Activity) driverEl).getLinkId() );

							// find corresponding passengers
							passengerLoop:
							for (Id passenger : driverRoute.getPassengersIds()) {
								tripCount.incCount( driver , passenger );
								int tripToFind = tripCount.getCount( driver , passenger );
								int currentTrip = 0;
								for (PlanElement passengerEl : plan.getIndividualPlans().get( passenger ).getPlanElements()) {
									if (passengerEl instanceof Leg && ((Leg) passengerEl).getMode().equals( JointActingTypes.PASSENGER )) {
										PassengerRoute pr = (PassengerRoute) ((Leg) passengerEl).getRoute();

										if (pr.getDriverId().equals( driver )) {
											currentTrip++;
											if (currentTrip == tripToFind) {
												poss.add( new Possibility(
															driver,
															driverOd,
															passenger,
															// XXX: not necessarily the case
															Od.create( pr.getStartLinkId() , pr.getEndLinkId() )) );
												continue passengerLoop;
											}
										}
									}
								}
							}
							driverRoute = null;
						}
						origin = (Activity) driverEl;
					}
					else if (((Leg) driverEl).getMode().equals( JointActingTypes.DRIVER )) {
						driverRoute = (DriverRoute) ((Leg) driverEl).getRoute();
					}
				}
			}
		}
		counter.printCounter();

		(new JointTripPossibilitiesXMLWriter( poss )).write( outFile );
	}

	private static class TripCount {
		private final Map<Id, Map<Id, Integer> > counts = new HashMap<Id, Map<Id, Integer> >();

		public int getCount(final Id d, final Id p) {
			Map<Id, Integer> m = counts.get( d );

			if (m == null) {
				m = new HashMap<Id, Integer>();
				counts.put( d , m );
			}

			Integer count = m.get( p );

			if (count == null) {
				count = 0;
				m.put( p , count );
			}

			return count;
		}

		public void incCount(final Id d, final Id p) {
			Map<Id, Integer> m = counts.get( d );

			if (m == null) {
				m = new HashMap<Id, Integer>();
				counts.put( d , m );
			}

			Integer count = m.get( p );

			if (count == null) {
				count = 0;
			}

			m.put( p , count + 1 );
		}
	}
}

