/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.thibautd.socnetsim.sharedvehicles;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class VehicleAllocationConsistencyChecker implements IterationEndsListener, IterationStartsListener {
	private final static Logger log = Logger.getLogger( VehicleAllocationConsistencyChecker.class );

	private boolean gotError = false;

	private final Population population;
	private final JointPlans jointPlans;

	@Inject
	public VehicleAllocationConsistencyChecker( final Scenario sc ) {
		this.population = sc.getPopulation();
		this.jointPlans = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		log.info( "Checking consistency of vehicle allocation" );
		final Set<Id> knownVehicles = new HashSet<Id>();
		final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();

		boolean hadNull = false;
		boolean hadNonNull = false;
		for ( Person person : population.getPersons().values() ) {
			final Plan plan = person.getSelectedPlan();
			final JointPlan jp = jointPlans.getJointPlan( plan );

			final Set<Id> vehsOfPlan = 
				 jp != null && knownJointPlans.add( jp ) ?
					SharedVehicleUtils.getVehiclesInJointPlan(
							jp,
							SharedVehicleUtils.DEFAULT_VEHICULAR_MODES) :
					(jp == null ?
						SharedVehicleUtils.getVehiclesInPlan(
								plan,
								SharedVehicleUtils.DEFAULT_VEHICULAR_MODES) :
						Collections.<Id>emptySet());

			for ( Id v : vehsOfPlan ) {
				if ( v == null ) {
					if ( hadNonNull ) {
						log.error( "got null and non-null vehicles" );
						gotError = true;
					}

					hadNull = true;
				}
				else {
					if ( hadNull ) {
						log.error( "got null and non-null vehicles" );
						gotError = true;
					}
					if ( !knownVehicles.add( v ) ) {
						log.error( "inconsistent allocation of vehicle "+v+" (found in several distinct joint plans)" );
						gotError = true;
					}
					hadNonNull = true;
				}
			}
		}
	}

	@Override
	public void notifyIterationStarts( IterationStartsEvent event ) {
		if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
	}
}
