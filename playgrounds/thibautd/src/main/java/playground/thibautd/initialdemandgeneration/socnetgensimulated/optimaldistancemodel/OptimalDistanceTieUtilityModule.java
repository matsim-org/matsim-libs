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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.optimaldistancemodel;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.ArentzePopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.DeterministicPart;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.ErrorTerm;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.GumbelErrorTerm;

/**
 * @author thibautd
 */
public class OptimalDistanceTieUtilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(OptimalDistanceTieUtilityModule.class);

	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );
		bind(DeterministicPart.class).toProvider(
				new Provider<DeterministicPart>() {
					@Inject
					ArentzePopulation population;

					@Inject
					OptimalDistanceConfigGroup pars;

					@Override
					public DeterministicPart get() {
						return
								new DeterministicPart() {
									@Override
									public double calcDeterministicPart(
											final int ego,
											final int alter ) {
										final double distance =
												CoordUtils.calcEuclideanDistance(
														population.getCoord(ego),
														population.getCoord(alter) );

										return distance > pars.getMinDistance_m() && distance < pars.getMaxDistance_m() ?
												pars.getUtilityOfMatch() :
												0;
									}
								};
					}
				});
		bind(ErrorTerm.class).to(GumbelErrorTerm.class);
		log.debug( "Configuring "+getClass().getSimpleName()+": DONE" );
	}
}
