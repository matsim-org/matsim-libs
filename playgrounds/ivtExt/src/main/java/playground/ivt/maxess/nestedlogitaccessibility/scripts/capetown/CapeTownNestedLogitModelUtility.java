/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.capetown;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.households.Household;
import org.matsim.households.Households;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class CapeTownNestedLogitModelUtility implements Utility<CapeTownModeNests> {
	private final CapeTownNestedLogitModelConfigGroup pars;

	private final Households households;
	private final Map<Id<Person>, Id<Household>> person2household = new HashMap<>();

	@Inject
	public CapeTownNestedLogitModelUtility(
			final Scenario scenario ) {
		this( (CapeTownNestedLogitModelConfigGroup) scenario.getConfig().getModule( CapeTownNestedLogitModelConfigGroup.GROUP_NAME ),
				scenario.getHouseholds() );
	}

	public CapeTownNestedLogitModelUtility(
			final CapeTownNestedLogitModelConfigGroup configGroup,
			final Households households ) {
		this.pars = configGroup;
		this.households = households;
		for ( Household hh : households.getHouseholds().values() ) {
			for ( Id<Person> personId : hh.getMemberIds() ) {
				person2household.put( personId , hh.getId() );
			}
		}
	}

	@Override
	public double calcUtility( final Person p, final Alternative<CapeTownModeNests> a ) {
		final double logTT = Math.log( 1 + getTravelTime( a ) );
		switch ( a.getNestId() ) {
			case car:
				return pars.getAscCar() +
						pars.getBetaTtCar() * logTT +
						pars.getBetaNCarsPerPerson() * getNCarsPerPerson( p );
			case pt:
				return pars.getAscPt() +
						pars.getBetaTtPt() * logTT;
			case walk:
				return pars.getAscWalk() +
						pars.getBetaTtWalk() * logTT;
			case ride:
				return pars.getAscRide() +
						pars.getBetaTtRide() * logTT;
			case taxi:
				return pars.getAscTaxi() +
						pars.getBetaTtTaxi() * logTT;
			default:
				throw new RuntimeException( "unknown nest "+a.getNestId() );
		}
	}

	private double getNCarsPerPerson( final Person p ) {
		final double hhSize = getHouseholdInteger( p , "householdSize" );
		final double nCars = getHouseholdInteger( p , "numberOfHouseholdCarsOwned" );
		final double nMotos = getHouseholdInteger( p , "numberOfHouseholdMotorcyclesOwned" );

		return (nCars + nMotos) / hhSize;
	}

	private Integer getHouseholdInteger( Person decisionMaker , String att ) {
		final Id<Household> hh = person2household.get( decisionMaker.getId() );
		if ( hh == null ) throw new IllegalStateException( "no household ID for person "+decisionMaker.getId() );
		return (Integer) households.getHouseholdAttributes().getAttribute( hh.toString() , att );
	}


	private double getTravelTime( Alternative<CapeTownModeNests> a ) {
		final List<? extends PlanElement> trip = a.getAlternative().getTrip();

		double tt = 0;

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg )  {
				tt += ( (Leg) pe ).getTravelTime();
			}
		}

		return tt;
	}
}
