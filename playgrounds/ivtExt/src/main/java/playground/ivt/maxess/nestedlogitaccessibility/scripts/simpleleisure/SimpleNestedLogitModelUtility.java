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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.simpleleisure;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.ModeNests;

import java.util.List;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModelUtility implements Utility<ModeNests> {
	private final ObjectAttributes personAttributes;
	private final SimpleNestedLogitUtilityConfigGroup pars;

	@Inject
	public SimpleNestedLogitModelUtility(
			final Config config,
			final Population population ) {
		this( (SimpleNestedLogitUtilityConfigGroup) config.getModule( SimpleNestedLogitUtilityConfigGroup.GROUP_NAME ),
				population.getPersonAttributes() );
	}

	public SimpleNestedLogitModelUtility(
			final SimpleNestedLogitUtilityConfigGroup configGroup,
			final ObjectAttributes personAttributes ) {
		this.personAttributes = personAttributes;
		this.pars = configGroup;
	}

	@Override
	public double calcUtility( final Person p, final Alternative<ModeNests> a ) {
		final double logTT = Math.log( 1 + getTravelTime( a ) );
		switch ( a.getNestId() ) {
			case car:
				return pars.getAscCar() +
						pars.getBetaTtCar() * logTT;
			case pt:
				double hasGA = hasGa( p ) ? 1 : 0;
				double hasHT = hasHT( p ) ? 1 : 0;
				double hasLocal = hasLocalAbo( p ) ? 1 : 0;

				return pars.getAscPt() +
						pars.getBetaTtPt() * logTT +
						pars.getBetaTtPtGa() * hasGA * logTT +
						pars.getBetaTtPtHt() * hasHT * logTT +
						pars.getBetaTtPtLocal() * hasLocal * logTT;
			case bike:
				final double hasLicense = hasLicense( p ) ? 1 : 0;
				return pars.getAscBike() +
						pars.getBetaTtBike() * logTT +
						pars.getBetaLicenseBike() * hasLicense;
			case walk:
				return pars.getAscWalk() +
						pars.getBetaTtWalk() * logTT;
			default:
				throw new RuntimeException( "unknown nest "+a.getNestId() );
		}
	}

	private boolean hasLicense( Person p ) {
		final String avail = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"driving licence" );
		return avail.equals( "yes" );
	}

	private boolean hasLocalAbo( Person p ) {
		final String avail = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"abonnement: Verbund" );
		return avail.equals( "yes" );
	}

	private boolean hasHT( Person p ) {
		final String avail = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"abonnement: Halbtax" );
		return avail.equals( "yes" );
	}

	private boolean hasGa( Person p ) {
		final String availFirst = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"abonnement: GA first class" );
		final String availSecond = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"abonnement: GA second class" );
		return availFirst.equals( "yes" ) || availSecond.equals( "yes" );
	}

	private double getTravelTime( Alternative<ModeNests> a ) {
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
