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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.estimatedsampling;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.ArentzePopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.TRBModelConfigGroup;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.DeterministicPart;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.ErrorTerm;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.TieUtility.GumbelErrorTerm;

/**
 * @author thibautd
 */
public class ContinuousAgeArentzeTieUtilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(ContinuousAgeArentzeTieUtilityModule.class);
	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );
		// binding automatic if module has a @Provides method
		// bind(DeterministicPart.class);
		bind(ErrorTerm.class).to(GumbelErrorTerm.class);
		log.debug( "Configuring "+getClass().getSimpleName()+": DONE" );
	}

	@Provides
	private DeterministicPart createDeterministicPart( ArentzePopulation population , EstimatedSamplingModelConfigGroup pars ) {
		return ( ego, alter ) -> {
			final int ageDifference =
				Math.abs(
					population.getAgeCategory( ego ) -
					population.getAgeCategory( alter ) );

			final double distance =
					CoordUtils.calcEuclideanDistance(
							population.getCoord(ego),
							population.getCoord(alter));

			return pars.getB_logDist() * EstimatedSamplingModelConfigGroup.transform( pars.getDistanceTransformation() , distance ) +
					+ pars.getB_sameGender() * dummy( population.isMale( ego ) == population.isMale( alter ) )
					+ pars.getB_ageDiff() *  EstimatedSamplingModelConfigGroup.transform( pars.getAgeTransformation() , ageDifference );
		};
	}

	private static double dummy(final boolean b) {
		return b ? 1 : 0;
	}
}
