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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.DeterministicPart;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.ErrorTerm;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.GumbelErrorTerm;

/**
 * @author thibautd
 */
public class ArentzeTieUtilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(ArentzeTieUtilityModule.class);
	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );
		bind(DeterministicPart.class).toProvider(
				new Provider<DeterministicPart>() {
					@Inject ArentzePopulation population;
					@Inject TRBModelConfigGroup pars;

					@Override
					public DeterministicPart get() {
						return
								new DeterministicPart() {
									@Override
									public double calcDeterministicPart(
											final int ego,
											final int alter ) {
										final int ageClassDifference =
										Math.abs(
											population.getAgeCategory( ego ) -
											population.getAgeCategory( alter ) );

										// increase distance by 1 (normally meter) to avoid linking with all agents
										// living in the same place.
										// TODO: test sensitivity of the results to this
										return pars.getB_logDist() * Math.log( CoordUtils.calcEuclideanDistance(population.getCoord(ego), population.getCoord(alter)) + 1 )
												+ pars.getB_sameGender() * dummy( population.isMale( ego ) == population.isMale( alter ) )
												+ pars.getB_ageDiff0() * dummy( ageClassDifference == 0 )
												+ pars.getB_ageDiff2() * dummy( ageClassDifference == 2 )
												+ pars.getB_ageDiff3() * dummy( ageClassDifference == 3 )
												+ pars.getB_ageDiff4() * dummy( ageClassDifference == 4 );
									}
								};
					}
				});
		bind(ErrorTerm.class).to(GumbelErrorTerm.class);
		log.debug( "Configuring "+getClass().getSimpleName()+": DONE" );
	}

	private static double dummy(final boolean b) {
		return b ? 1 : 0;
	}
}
