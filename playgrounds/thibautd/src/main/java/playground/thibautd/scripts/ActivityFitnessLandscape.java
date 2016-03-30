/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityFitnessLandscape.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class ActivityFitnessLandscape {
	private static final String LONG_TYPE = "long_act";
	private static final String SHORT_TYPE = "short_act";

	private static final double LONG_DUR = 14 * 3600;
	private static final double SHORT_DUR = 10 * 3600;

	public static void main(final String[] args) throws IOException {
		final String outfile = args[ 0 ];
		final BufferedWriter writer = IOUtils.getBufferedWriter( outfile );
		writer.write( "long_act_dur\tscore" );

		final PlanCalcScoreConfigGroup config = new PlanCalcScoreConfigGroup();

		final ActivityParams longParams = new ActivityParams( LONG_TYPE );
		longParams.setTypicalDuration( LONG_DUR );

		final ActivityParams shortParams = new ActivityParams( SHORT_TYPE );
		shortParams.setTypicalDuration( SHORT_DUR );

		config.addActivityParams( longParams );
		config.addActivityParams( shortParams );

		CharyparNagelActivityScoring testee =
			new CharyparNagelActivityScoring(
					new CharyparNagelScoringParameters.Builder(config, config.getScoringParameters(null), new ScenarioConfigGroup()).build());

		final Activity shortAct = new ActivityImpl( SHORT_TYPE , Id.create( 1 , Link.class ) );
		final Activity longAct = new ActivityImpl( LONG_TYPE , Id.create( 1 , Link.class ) );

		double now = 0;
		now += SHORT_DUR;
		testee.endActivity( now , shortAct );
		testee.startActivity( now , longAct );
		now += LONG_DUR;
		testee.endActivity( now , longAct );
		testee.startActivity( now , shortAct );

		testee.finish();

		for ( double shortTime = 0; shortTime < 24 * 3600; shortTime += 360 ) {
			if ( Math.abs( shortTime - SHORT_DUR ) < 1 ) continue;
			testee = new CharyparNagelActivityScoring(
					new CharyparNagelScoringParameters.Builder(config, config.getScoringParameters(null), new ScenarioConfigGroup()).build());

			final double longTime = 24 * 3600 - shortTime;
			now = shortTime;
			testee.endActivity( now , shortAct );
			testee.startActivity( now , longAct );
			now += longTime;
			testee.endActivity( now , longAct );
			testee.startActivity( now , shortAct );

			testee.finish();
			final double score = testee.getScore();
			writer.newLine();
			writer.write( (longTime / 3600.) +"\t"+ score );
		}	
		writer.close();
	}
}

