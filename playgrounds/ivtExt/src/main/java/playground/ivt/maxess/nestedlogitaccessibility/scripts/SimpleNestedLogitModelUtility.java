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
package playground.ivt.maxess.nestedlogitaccessibility.scripts;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;

import java.util.List;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModelUtility implements Utility<ModeNests> {
	private static final double ASC_CAR = 0;
	private static final double BETA_TT_CAR = -0.276;

	private static final double ASC_PT = 0.0644;
	private static final double BETA_TT_PT = -0.508;
	private static final double BETA_TT_PT_GA = 0.158;
	private static final double BETA_TT_PT_HT = 0.0653;
	private static final double BETA_TT_PT_LOCAL = 0.169;

	private static final double ASC_BIKE = 1.85;
	private static final double BETA_TT_BIKE = -0.235;
	private static final double BETA_LICENSE_BIKE = -0.614;

	private static final double ASC_WALK = 6.86;
	private static final double BETA_TT_WALK = -0.917;

	private final ObjectAttributes personAttributes;

	@Inject
	public SimpleNestedLogitModelUtility( final Population population ) {
		this( population.getPersonAttributes() );
	}

	public SimpleNestedLogitModelUtility( final ObjectAttributes personAttributes ) {
		this.personAttributes = personAttributes;
	}

	@Override
	public double calcUtility( final Person p, final Alternative<ModeNests> a ) {
		final double logTT = Math.log( 1 + getTravelTime( a ) );
		switch ( a.getNestId() ) {
			case car:
				return ASC_CAR +
						BETA_TT_CAR * logTT;
			case pt:
				double hasGA = hasGa( p ) ? 1 : 0;
				double hasHT = hasHT( p ) ? 1 : 0;
				double hasLocal = hasLocalAbo( p ) ? 1 : 0;

				return ASC_PT +
						BETA_TT_PT * logTT +
						BETA_TT_PT_GA * hasGA * logTT +
						BETA_TT_PT_HT * hasHT * logTT +
						BETA_TT_PT_LOCAL * hasLocal * logTT;
			case bike:
				final double hasLicense = hasLicense( p ) ? 1 : 0;
				return ASC_BIKE +
						BETA_TT_BIKE * logTT +
						BETA_LICENSE_BIKE * hasLicense;
			case walk:
				return ASC_WALK +
						BETA_TT_WALK * logTT;
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
