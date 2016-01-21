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
package playground.ivt.router.lazyschedulebasedmatrix;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class ScheduleBasedMatrixInjectionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInstanciation() {
		final Config config = utils.loadConfig( "test/scenarios/pt-simple/config.xml" );

		config.controler().setLastIteration( 0 );

		final Controler controler = new Controler( config );
		controler.addOverridingModule( new LazyScheduleBasedMatrixModule() );

		// to get the injector created
		// this also routes in prepare for sim, so it at least tests for crashes
		// (but not for correctness)
		controler.run();

		Assert.assertEquals(
				"unexpected class for PT router",
				LazyScheduleBasedMatrixRoutingModule.class,
				controler.getTripRouterProvider().get().getRoutingModule( TransportMode.pt ).getClass() );
	}
}
