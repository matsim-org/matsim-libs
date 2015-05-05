/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.herbiespecific;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import herbie.running.config.HerbieConfigGroup;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.herbie.HerbieTransitRouterFactory;
import playground.thibautd.parknride.ParkAndRideUtils;
import playground.thibautd.parknride.scoring.CenteredTimeProportionalPenaltyFactory;
import playground.thibautd.parknride.scoring.ParkingPenaltyFactory;

import javax.inject.Provider;

/**
 * Tests if the custom controler behaves as expected
 * @author thibautd
 */
public class ControlerTest {
	private static final Coord center = RelevantCoordinates.HAUPTBAHNHOF;
	private static final Coord boundaryPoint = RelevantCoordinates.SEEBACH;
	// cost at parkaus hauptbahnof
	private static final double costPerSecondAtCenter = 4.4 / 3600d;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore( "failing after rev 23119: removal of PlanStrategy( Controler ) in StrategyManagerConfigLoader" )
	public void testTransitRouterFactoryType() {
		UglyHerbieMultilegControler controler = initializeControler();

		controler.getConfig().controler().setLastIteration( 0 );
		try {
			controler.run();
		}
		catch (Exception e) {
			// swallow exception arising because of no facilities.
			// XXX DIRTYYYYYYYYY!!!!!
			// TODO create facilities and run completely
		}
		Provider<TransitRouter> f = controler.getTransitRouterFactory();

		assertNotNull(
				"transit router factory is null!?",
				f);

		assertTrue(
				"wrong transit router factory type "+f.getClass(),
				f instanceof HerbieTransitRouterFactory);
	}

	private UglyHerbieMultilegControler initializeControler() {
		Config config = ConfigUtils.createConfig();
		ParkAndRideUtils.setConfigGroup( config );
		config.addModule( new HerbieConfigGroup() );

		String inputDir = utils.getPackageInputDirectory();
		inputDir = inputDir.substring( 0 , inputDir.lastIndexOf( "/" ) );
		inputDir = inputDir.substring( 0 , inputDir.lastIndexOf( "/" ) );
		ConfigUtils.loadConfig( config , inputDir+"/config.xml" );

		config.controler().setOutputDirectory( utils.getOutputDirectory() );

		Scenario scenario = ParkAndRideUtils.loadScenario( config );

		ParkingPenaltyFactory penalty =
			new CenteredTimeProportionalPenaltyFactory(
					center,
					CoordUtils.calcDistance( center , boundaryPoint ),
					costPerSecondAtCenter * config.planCalcScore().getMarginalUtilityOfMoney());

		UglyHerbieMultilegControler c = new UglyHerbieMultilegControler( scenario );
		c.setParkingPenaltyFactory( penalty );

		return c;
	}
}

