/* *********************************************************************** *
 * project: org.matsim.*
 * KtiToSimiliKtiConfig.java
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
 * @author thibautd
 */
public class KtiToSimiliKtiConfig {
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
		// XXX KTI defines a constant for car in the config group,
		// BUT IT IS NOT USED IN THE SCORING FUNCTION!!!!!!!
		//planCalcScore.setConstantCar( ktiConfigGroup.getConstCar() );
		planCalcScore.getModes().get(TransportMode.car).setConstant((double) 0);
		planCalcScore.getModes().get(TransportMode.bike).setConstant(ktiConfigGroup.getConstBike());
		planCalcScore.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(ktiConfigGroup.getTravelingBike());
		planCalcScore.getModes().get(TransportMode.pt).setMonetaryDistanceRate((planCalcScore.getModes().get(TransportMode.pt).getMonetaryDistanceRate() * planCalcScore.getMarginalUtilityOfMoney() * ktiConfigGroup.getDistanceCostPtNoTravelCard() / 1000d ) /
				planCalcScore.getMarginalUtilityOfMoney());
		planCalcScore.getModes().get(TransportMode.car).setMonetaryDistanceRate((planCalcScore.getModes().get(TransportMode.car).getMonetaryDistanceRate() * planCalcScore.getMarginalUtilityOfMoney() * ktiConfigGroup.getDistanceCostCar() / 1000d) /
				planCalcScore.getMarginalUtilityOfMoney());

		final KtiLikeScoringConfigGroup ktiLikeConfigGroup = new KtiLikeScoringConfigGroup();
		outputConfig.addModule( ktiLikeConfigGroup );
		ktiLikeConfigGroup.setTravelCardRatio( ktiConfigGroup.getDistanceCostPtUnknownTravelCard() / ktiConfigGroup.getDistanceCostPtNoTravelCard() );

		new ConfigWriter( outputConfig ).write( outputConfigFile );
	}
}

