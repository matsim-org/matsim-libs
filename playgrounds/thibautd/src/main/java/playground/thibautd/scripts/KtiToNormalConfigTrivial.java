/* *********************************************************************** *
 * project: org.matsim.*
 * KtiToNormalConfigTrivial.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.thibautd.utils.MyConfigUtils;

/**
 * Uses the trivial meaning of KTI params (NOT the one resulting from the code),
 * with the idea that the strange things in the code may have been introduced by refactorings
 * posterior to the calibration.
 * @author thibautd
 */
public class KtiToNormalConfigTrivial {
	public static void main(final String[] args) {
		final String inputConfigFile = args[ 0 ];
		final String outputConfigFile = args[ 1 ];

		final Config inputConfig = ConfigUtils.createConfig();
		final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		inputConfig.addModule( ktiConfigGroup );
		ConfigUtils.loadConfig( inputConfig , inputConfigFile );

		final Config outputConfig = new Config();
		final PlanCalcScoreConfigGroup planCalcScore = new PlanCalcScoreConfigGroup();
		outputConfig.addModule( planCalcScore );
		MyConfigUtils.transmitParams( inputConfig.planCalcScore() , planCalcScore );
		planCalcScore.getModes().get(TransportMode.car).setConstant(ktiConfigGroup.getConstCar());
		planCalcScore.getModes().get(TransportMode.bike).setConstant(ktiConfigGroup.getConstBike());
		planCalcScore.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(ktiConfigGroup.getTravelingBike());
		// TODO: check units (per km or per m)
		double monetaryDistanceRatePt = -( ktiConfigGroup.getDistanceCostPtNoTravelCard() / 1000d ) /
        planCalcScore.getMarginalUtilityOfMoney();
		planCalcScore.getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
		double monetaryDistanceRateCar = -( ktiConfigGroup.getDistanceCostCar() / 1000d ) /
        planCalcScore.getMarginalUtilityOfMoney();
		planCalcScore.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		final KtiLikeScoringConfigGroup ktiLikeConfigGroup = new KtiLikeScoringConfigGroup();
		outputConfig.addModule( ktiLikeConfigGroup );
		ktiLikeConfigGroup.setTravelCardRatio( ktiConfigGroup.getDistanceCostPtUnknownTravelCard() / ktiConfigGroup.getDistanceCostPtNoTravelCard() );

		new ConfigWriter( outputConfig ).write( outputConfigFile );
	}
}


