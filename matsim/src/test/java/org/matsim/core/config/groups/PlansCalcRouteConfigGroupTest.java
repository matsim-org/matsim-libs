/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.testcases.MatsimTestUtils;

public class PlansCalcRouteConfigGroupTest {

	private final static Logger log = Logger.getLogger(PlansCalcRouteConfigGroupTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testBackwardsCompatibility() {
		PlansCalcRouteConfigGroup group = new PlansCalcRouteConfigGroup();

		// test default
		Assert.assertEquals("different default than expected.", 3.0 / 3.6, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON);
		try {
			group.addParam("walkSpeedFactor", "1.5");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
			Assert.assertFalse("Exception-Message should not be empty.", e.getMessage().isEmpty());
		}
		Assert.assertEquals("value should not have changed.", 3.0 / 3.6, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON);
		group.addParam("walkSpeed", "1.5");
		Assert.assertEquals("value should have changed.", 1.5, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testDefaultsAreCleared() {
		PlansCalcRouteConfigGroup group = new PlansCalcRouteConfigGroup();
		
		Assert.assertEquals(
				"unexpected number of default modes in "+group.getModeRoutingParams(),
				4,
				group.getModeRoutingParams().size() );

		group.setTeleportedModeSpeed( "skateboard" , 20 / 3.6 );
		group.setTeleportedModeSpeed( "longboard" , 20 / 3.6 );
		Assert.assertEquals(
				"unexpected number of modes after adding new mode in "+group.getModeRoutingParams(),
				2,
				group.getModeRoutingParams().size() );
	}

	@Test
	public void testIODifferentVersions() {
			final PlansCalcRouteConfigGroup initialGroup = createTestConfigGroup();

		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		final Config configV1 = new Config();
		configV1.addModule(toUnderscoredModule(initialGroup));

		new ConfigWriter( configV1 ).writeFileV1( v1path );

		final Config configV1In = ConfigUtils.createConfig();
		new MatsimConfigReader( configV1In ).readFile( v1path );

		assertIdentical("re-read v1", initialGroup, configV1In.plansCalcRoute());

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";

		new ConfigWriter( configV1In ).writeFileV2( v2path );

		final Config configV2 = ConfigUtils.createConfig();
		new MatsimConfigReader( configV2 ).readFile( v2path );

		assertIdentical("re-read v2", initialGroup, configV2.plansCalcRoute());
	}

	@Test( expected=RuntimeException.class )
	public void testConsistencyCheckIfNoTeleportedSpeed() {
		final Config config = ConfigUtils.createConfig();

		final ModeRoutingParams params = new ModeRoutingParams( "skateboard" );
		config.plansCalcRoute().addModeRoutingParams( params );

		config.checkConsistency();
	}

	@Test( expected=IllegalStateException.class )
	public void testCannotAddSpeedAfterFactor() {
		final ModeRoutingParams params = new ModeRoutingParams( "overboard" );
		params.setTeleportedModeFreespeedFactor( 2.0 );
		params.setTeleportedModeSpeed( 12.0 );
	}

	@Test( expected=IllegalStateException.class )
	public void testCannotAddFactorAfterSpeed() {
		final ModeRoutingParams params = new ModeRoutingParams( "overboard" );
		params.setTeleportedModeSpeed( 12.0 );
		params.setTeleportedModeFreespeedFactor( 2.0 );
	}

	private static void assertIdentical(
			final String msg,
			final PlansCalcRouteConfigGroup initialGroup,
			final PlansCalcRouteConfigGroup inputConfigGroup) {
		Assert.assertEquals(
				"unexpected beelineDistanceFactor",
//				initialGroup.getBeelineDistanceFactor(),
//				inputConfigGroup.getBeelineDistanceFactor(),
				initialGroup.getBeelineDistanceFactors(),
				inputConfigGroup.getBeelineDistanceFactors() ) ;
//				MatsimTestUtils.EPSILON );
		Assert.assertEquals(
				"unexpected networkModes",
				initialGroup.getNetworkModes(),
				inputConfigGroup.getNetworkModes() );
		Assert.assertEquals(
				"unexpected teleportedModeFreespeedFactors",
				initialGroup.getTeleportedModeFreespeedFactors(),
				inputConfigGroup.getTeleportedModeFreespeedFactors() );
		Assert.assertEquals(
				"unexpected teleportedModeSpeeds",
				initialGroup.getTeleportedModeSpeeds(),
				inputConfigGroup.getTeleportedModeSpeeds() );
	}

	private ConfigGroup toUnderscoredModule(final PlansCalcRouteConfigGroup initialGroup) {
		final ConfigGroup module = new ConfigGroup( initialGroup.getName() );

		for ( Map.Entry<String, String> e : initialGroup.getParams().entrySet() ) {
			log.info( "add param "+e.getKey() );
			module.addParam( e.getKey() , e.getValue() );
		}

		for ( ModeRoutingParams settings : initialGroup.getModeRoutingParams().values() ) {
			final String mode = settings.getMode();
			module.addParam( "teleportedModeSpeed_"+mode , ""+settings.getTeleportedModeSpeed() );
			module.addParam( "teleportedModeFreespeedFactor_"+mode , ""+settings.getTeleportedModeFreespeedFactor() );
		}

		return module;
	}

	private PlansCalcRouteConfigGroup createTestConfigGroup() {
		final PlansCalcRouteConfigGroup group = new PlansCalcRouteConfigGroup();

		group.setBeelineDistanceFactor( 10000000 );
		group.setNetworkModes( Arrays.asList( "electricity" , "water_supply" ) );

		// two modes with only one speed
		group.setTeleportedModeFreespeedFactor( "inline skate" , 0.1 );
		group.setTeleportedModeSpeed( "ice skates" , 10 );

		// one mode with both speeds
		// Was made illegal: do not test
		//group.setTeleportedModeFreespeedFactor( "overboard" , 100 );
		//group.setTeleportedModeSpeed( "overboard" , 999 );

		return group;
	}
}
