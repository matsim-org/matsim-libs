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

import org.apache.logging.log4j.LogManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.testcases.MatsimTestUtils;

public class RoutingConfigGroupTest {
	private final static Logger log = LogManager.getLogger(RoutingConfigGroupTest.class);
	private final static int N_MODE_ROUTING_PARAMS_DEFAULT = 5 ;

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testAddModeParamsTwice() {
		String outdir = utils.getOutputDirectory();
		final String filename = outdir + "config.xml";
		{
			Config config = ConfigUtils.createConfig();
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( N_MODE_ROUTING_PARAMS_DEFAULT, group.getModeRoutingParams().size() );
			group.clearModeRoutingParams();
//			group.setTeleportedModeSpeed( TransportMode.bike, 1. );
//			Assert.assertEquals( 1, group.getModeRoutingParams().size() );
//			group.setTeleportedModeSpeed( TransportMode.bike, 2. );
//			Assert.assertEquals( 2, group.getModeRoutingParams().size() );
//			group.clearModeRoutingParams();
//			Assert.assertEquals( 0, group.getModeRoutingParams().size() );

			ConfigUtils.writeConfig( config, filename );
		}
		{
			Config config = ConfigUtils.loadConfig( filename ) ;
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( 0, group.getModeRoutingParams().size() );
		}
	}

	@Test
	void testClearParamsWriteRead() {
		String outdir = utils.getOutputDirectory();
		final String filename = outdir + "config.xml";
		{
			Config config = ConfigUtils.createConfig();
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( N_MODE_ROUTING_PARAMS_DEFAULT, group.getModeRoutingParams().size() );
			group.clearModeRoutingParams();
			group.setTeleportedModeSpeed( TransportMode.bike, 1. );
			Assertions.assertEquals( 1, group.getModeRoutingParams().size() );
			group.setTeleportedModeSpeed( "abc", 1. );
			Assertions.assertEquals( 2, group.getModeRoutingParams().size() );
			group.clearModeRoutingParams();
			Assertions.assertEquals( 0, group.getModeRoutingParams().size() );

			ConfigUtils.writeConfig( config, filename );
		}
		{
			Config config = ConfigUtils.loadConfig( filename ) ;
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( 0, group.getModeRoutingParams().size() );
		}
	}

	@Test
	void testRemoveParamsWriteRead() {
		String outdir = utils.getOutputDirectory();
		final String filename = outdir + "config.xml";
		{
			Config config = ConfigUtils.createConfig();
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( N_MODE_ROUTING_PARAMS_DEFAULT, group.getModeRoutingParams().size() );
			group.setTeleportedModeSpeed( TransportMode.bike, 1. );
			Assertions.assertEquals( 1, group.getModeRoutingParams().size() );
			group.setTeleportedModeSpeed( "abc", 1. );
			Assertions.assertEquals( 2, group.getModeRoutingParams().size() );
			for( String mode : group.getModeRoutingParams().keySet() ){
				group.removeModeRoutingParams( mode );
			}
			Assertions.assertEquals( 0, group.getModeRoutingParams().size() );

			ConfigUtils.writeConfig( config, filename );
		}
		{
			Config config = ConfigUtils.loadConfig( filename ) ;
			RoutingConfigGroup group = config.routing();
			Assertions.assertEquals( 0, group.getModeRoutingParams().size() );
		}
	}

	@Test
	void testClearDefaults() {
		Config config = ConfigUtils.createConfig(  ) ;
		RoutingConfigGroup group = config.routing() ;
		Assertions.assertEquals( N_MODE_ROUTING_PARAMS_DEFAULT, group.getModeRoutingParams().size() );
		group.setTeleportedModeSpeed( "def", 1. );
		Assertions.assertEquals( 1, group.getModeRoutingParams().size() );
		group.setTeleportedModeSpeed( "abc", 1. );
		Assertions.assertEquals( 2, group.getModeRoutingParams().size() );
		group.clearModeRoutingParams( );
		Assertions.assertEquals( 0, group.getModeRoutingParams().size() );
	}

	@Test
	void test3() {
		Config config = ConfigUtils.createConfig(  ) ;
		RoutingConfigGroup group = config.routing() ;
		group.clearModeRoutingParams();
		group.setClearingDefaultModeRoutingParams( true ); // should be ok
	}

	@Test
	void testInconsistencyBetweenActionAndState() {
		assertThrows(RuntimeException.class, () -> {
			RoutingConfigGroup group = new RoutingConfigGroup() ;
			group.clearModeRoutingParams();
			group.setClearingDefaultModeRoutingParams(false); // should fail
		}); // should fail
	}

	@Test
	void testBackwardsCompatibility() {
		RoutingConfigGroup group = new RoutingConfigGroup();

		// test default
		Assertions.assertEquals(3.0 / 3.6, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON, "different default than expected.");
		try {
			group.addParam("walkSpeedFactor", "1.5");
		}
		catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
			Assertions.assertFalse(e.getMessage().isEmpty(), "Exception-Message should not be empty.");
		}
		Assertions.assertEquals(3.0 / 3.6, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON, "value should not have changed.");
		group.addParam("walkSpeed", "1.5");
		Assertions.assertEquals(1.5, group.getTeleportedModeSpeeds().get(TransportMode.walk), MatsimTestUtils.EPSILON, "value should have changed.");
	}

	@Test
	void testDefaultsAreCleared() {
		RoutingConfigGroup group = new RoutingConfigGroup();
//		group.clearModeRoutingParams();
		group.setTeleportedModeSpeed( "skateboard" , 20 / 3.6 );
		group.setTeleportedModeSpeed( "longboard" , 20 / 3.6 );
		Assertions.assertEquals(
				2,
				group.getModeRoutingParams().size(),
				"unexpected number of modes after adding new mode in "+group.getModeRoutingParams() );
	}

	@Test
	void testIODifferentVersionsV1() {
		// V1 (flat params) format encodes all mode routing params as top-level key-value pairs.
		// It supports per-mode teleportedModeSpeed and teleportedModeFreespeedFactor via the
		// "teleportedModeSpeed_<mode>" and "teleportedModeFreespeedFactor_<mode>" keys, but
		// beelineDistanceFactor only as a single GLOBAL value ("beelineDistanceFactor" key).
		// See RoutingConfigGroup.addParam: there is no "beelineDistanceFactor_<mode>" handler.
		//
		// Circumvention: use a test config that contains only teleportedModeSpeed-based modes
		// that all share the same beelineDistanceFactor, so the single global key faithfully
		// represents all of them. Freespeed-teleported modes are excluded because:
		//   (a) their beelineDistanceFactor is null (irrelevant for freespeed routing), which
		//       would cause toUnderscoredModule to throw when comparing null vs non-null; and
		//   (b) RoutingConfigGroup.setBeelineDistanceFactor (invoked during V1 read-back) pushes
		//       the global value to ALL modes, so a freespeed mode would acquire a spurious
		//       beelineDistanceFactor entry in the read-back group, making assertIdentical fail.
		final RoutingConfigGroup initialGroup = createTestConfigGroupV1();

		final Config configV1 = new Config();
		configV1.addModule(toUnderscoredModule(initialGroup));

		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		new ConfigWriter( configV1 ).writeFileV1( v1path );

		final Config configV1In = ConfigUtils.createConfig();
		new ConfigReader( configV1In ).readFile( v1path );

		assertIdentical("re-read v1", initialGroup, configV1In.routing());
	}

	@Test
	void testIODifferentVersionsV2() {
		// V2 (nested parameterset) format stores beelineDistanceFactor inside each
		// <parameterset type="teleportedModeParams"> element, so per-mode values are
		// preserved exactly. No circumvention needed: the full test config with
		// differing per-mode beelineDistanceFactors and freespeed-teleported modes
		// can be round-tripped without loss.
		final RoutingConfigGroup initialGroup = createTestConfigGroup();

		final Config configOut = new Config();
		configOut.addModule(initialGroup);

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";
		new ConfigWriter( configOut ).writeFileV2( v2path );

		final Config configV2 = ConfigUtils.createConfig();
		new ConfigReader( configV2 ).readFile( v2path );

		assertIdentical("re-read v2", initialGroup, configV2.routing());
	}

	@Test
	void testConsistencyCheckIfNoTeleportedSpeed() {
		assertThrows(RuntimeException.class, () -> {
			final Config config = ConfigUtils.createConfig();

			final TeleportedModeParams params = new TeleportedModeParams( "skateboard" );
			config.routing().addModeRoutingParams(params);
			// (one needs to set one of the teleported speed settings)

			config.checkConsistency();
		});
	}

	@Test
	void testCannotAddSpeedAfterFactor() {
		assertThrows(IllegalStateException.class, () -> {
			final TeleportedModeParams params = new TeleportedModeParams( "overboard" );
			params.setTeleportedModeFreespeedFactor(2.0);
			params.setTeleportedModeSpeed(12.0);
		});
	}

	@Test
	void testCannotAddFactorAfterSpeed() {
		assertThrows(IllegalStateException.class, () -> {
			final TeleportedModeParams params = new TeleportedModeParams( "overboard" );
			params.setTeleportedModeSpeed(12.0);
			params.setTeleportedModeFreespeedFactor(2.0);
		});
	}

	private static void assertIdentical(
			final String msg,
			final RoutingConfigGroup initialGroup,
			final RoutingConfigGroup inputConfigGroup) {
		Assertions.assertEquals(
				initialGroup.getBeelineDistanceFactors(),
				inputConfigGroup.getBeelineDistanceFactors(),
				"unexpected beelineDistanceFactor" ) ;
//				MatsimTestUtils.EPSILON );
		Assertions.assertEquals(
				initialGroup.getNetworkModes(),
				inputConfigGroup.getNetworkModes(),
				"unexpected networkModes" );
		Assertions.assertEquals(
				initialGroup.getTeleportedModeFreespeedFactors(),
				inputConfigGroup.getTeleportedModeFreespeedFactors(),
				"unexpected teleportedModeFreespeedFactors" );
		Assertions.assertEquals(
				initialGroup.getTeleportedModeSpeeds(),
				inputConfigGroup.getTeleportedModeSpeeds(),
				"unexpected teleportedModeSpeeds" );
	}

	// Converts a RoutingConfigGroup into a flat V1-style ConfigGroup (key-value params only).
	// Limitation: V1 format can represent beelineDistanceFactor only as a single global value.
	// The caller is therefore responsible for ensuring that ALL modes in initialGroup share
	// the same (non-null) beelineDistanceFactor. Passing a config that violates this
	// (e.g. modes with differing factors, or freespeed-teleported modes whose
	// getBeelineDistanceFactor() returns null) will throw a RuntimeException.
	private static ConfigGroup toUnderscoredModule(final RoutingConfigGroup initialGroup) {
		final ConfigGroup module = new ConfigGroup( initialGroup.getName() );

		for ( Map.Entry<String, String> e : initialGroup.getParams().entrySet() ) {
			log.info( "add param="+e.getKey() + " with value=" + e.getValue() );
			module.addParam( e.getKey() , e.getValue() );
		}

		for ( TeleportedModeParams settings : initialGroup.getModeRoutingParams().values() ) {
			final String mode = settings.getMode();
			module.addParam( "teleportedModeSpeed_"+mode , ""+settings.getTeleportedModeSpeed() );
			module.addParam( "teleportedModeFreespeedFactor_"+mode , ""+settings.getTeleportedModeFreespeedFactor() );
		}

		Double val = null ;
		boolean first = true ;
		for ( TeleportedModeParams settings : initialGroup.getModeRoutingParams().values() ) {
			if ( first ) {
				first = false ;
				val = settings.getBeelineDistanceFactor() ;
			} else if ( !settings.getBeelineDistanceFactor().equals( val ) ) {
				throw new RuntimeException( "beeline distance factor varies by mode; this cannot be covered by this test" ) ;
			}
		}
		module.addParam( "beelineDistanceFactor", ""+val );

		return module;
	}

	// Full test config used for V2 round-trip testing. Contains both a freespeed-teleported
	// mode (null beelineDistanceFactor, irrelevant for freespeed routing) and a speed-teleported
	// mode with an explicit beelineDistanceFactor, exercising per-mode beeline factor storage.
	private static RoutingConfigGroup createTestConfigGroup() {
		log.info( "creating test config group ... ");

		final RoutingConfigGroup group = new RoutingConfigGroup();

		group.setNetworkModes( Arrays.asList( "electricity" , "water_supply" ) );

		// two modes with only one speed
		TeleportedModeParams params_freespeed = new TeleportedModeParams( "inline skate" );
		params_freespeed.setTeleportedModeFreespeedFactor( 0.1 );
		// Was made illegal: do not test
		// params_freespeed.setBeelineDistanceFactor( 10000000. );
		group.addTeleportedModeParams(params_freespeed);

		TeleportedModeParams params_speed = new TeleportedModeParams( "ice skates" );
		params_speed.setTeleportedModeSpeed(10.);
		params_speed.setBeelineDistanceFactor(10000000.);
		group.addTeleportedModeParams(params_speed);

		// one mode with both speeds
		// Was made illegal: do not test
		//group.setTeleportedModeFreespeedFactor( "overboard" , 100 );
		//group.setTeleportedModeSpeed( "overboard" , 999 );

		log.info( "... done creating test config group.");
		return group;
	}

	// Restricted test config for V1 round-trip testing. Contains only teleportedModeSpeed-based
	// modes that all share the same beelineDistanceFactor. This is required because:
	//   (1) V1 flat format only supports a single global "beelineDistanceFactor" key — there is
	//       no "beelineDistanceFactor_<mode>" handler in RoutingConfigGroup.addParam.
	//   (2) RoutingConfigGroup.setBeelineDistanceFactor (the @Deprecated global setter, invoked
	//       during V1 read-back) pushes its value to ALL modes. Including a freespeed-teleported
	//       mode (whose beelineDistanceFactor is null) would cause it to acquire a spurious
	//       entry in getBeelineDistanceFactors() after read-back, breaking assertIdentical.
	private static RoutingConfigGroup createTestConfigGroupV1() {
		log.info( "creating V1-compatible test config group ... ");

		final RoutingConfigGroup group = new RoutingConfigGroup();

		group.setNetworkModes( Arrays.asList( "electricity" , "water_supply" ) );

		TeleportedModeParams params1 = new TeleportedModeParams( "ice skates" );
		params1.setTeleportedModeSpeed( 10. );
		params1.setBeelineDistanceFactor( 1.5 );
		group.addTeleportedModeParams( params1 );

		TeleportedModeParams params2 = new TeleportedModeParams( "roller blades" );
		params2.setTeleportedModeSpeed( 5. );
		params2.setBeelineDistanceFactor( 1.5 ); // must match params1: V1 only supports one global value
		group.addTeleportedModeParams( params2 );

		log.info( "... done creating V1-compatible test config group.");
		return group;
	}
}
