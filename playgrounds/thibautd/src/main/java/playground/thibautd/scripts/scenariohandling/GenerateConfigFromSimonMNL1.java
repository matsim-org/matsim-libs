/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateConfigFromSimonMNL1.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

/**
 * @author thibautd
 */
public class GenerateConfigFromSimonMNL1 {
	public static void main(final String[] args) {
		final double alpha = Double.parseDouble( args[ 0 ] );
		final String outConf = args[ 1 ];

		final double utilPerf = 6;
		final PlanCalcScoreConfigGroup group = new PlanCalcScoreConfigGroup();

		final double conv = 60; // alpha and conversion to hourly utility

		/* walk */ {
			final ModeParams pars = group.getOrCreateModeParams( TransportMode.walk );
			pars.setConstant( alpha * 1.129 );
			pars.setMarginalUtilityOfTraveling( conv * alpha * -0.045 + utilPerf );
		}

		/* transit walk */ {
			final ModeParams pars = group.getOrCreateModeParams( TransportMode.transit_walk );
			pars.setConstant( alpha * 1.129 );
			pars.setMarginalUtilityOfTraveling( conv * alpha * -0.045 + utilPerf );
		}

		/* car */ {
			final ModeParams pars = group.getOrCreateModeParams( TransportMode.car );
			pars.setConstant( alpha * 1.547 );
			pars.setMarginalUtilityOfTraveling( conv * alpha * -0.052 + utilPerf );
		}

		/* bike */ {
			final ModeParams pars = group.getOrCreateModeParams( TransportMode.bike );
			pars.setConstant( alpha * 0 );
			pars.setMarginalUtilityOfTraveling( conv * alpha * -0.063 + utilPerf );
		}

		/* pt */ {
			final ModeParams pars = group.getOrCreateModeParams( TransportMode.pt );
			pars.setConstant( alpha * 0.614 );
			pars.setMarginalUtilityOfTraveling( conv * alpha * -0.013 + utilPerf );
		}

		group.setUtilityOfLineSwitch( alpha * -0.361 );

		final Config config = new Config();
		config.addModule( group );
		new ConfigWriter( config ).write( outConf );
	}
}

