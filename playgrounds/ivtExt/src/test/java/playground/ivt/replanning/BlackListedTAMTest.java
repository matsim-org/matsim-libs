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
package playground.ivt.replanning;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class BlackListedTAMTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDoesNotCrash() {
		final Config config = utils.loadConfig( "test/scenarios/siouxfalls-2014-reduced/config_default.xml" );
		config.controler().setLastIteration( 1 );
		config.plans().setActivityDurationInterpretation(
				PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		final StrategyConfigGroup.StrategySettings settings = new StrategyConfigGroup.StrategySettings();
		settings.setStrategyName( "BlackListedTimeAllocationMutator" );
		settings.setWeight( 100 );
		config.strategy().addStrategySettings( settings );

		final Controler controler = new Controler( config );
		controler.addOverridingModule( new BlackListedTimeAllocationMutatorStrategyModule() );
		controler.run();
	}
}
