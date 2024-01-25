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
	void testIODifferentVersions()
	{
		final RoutingConfigGroup initialGroup = createTestConfigGroup();

		log.info( "constructing new config ...");
		final Config configV1 = new Config();
		log.info("... done constructing new config.");
		log.info("adding undescored info ...");
		configV1.addModule(toUnderscoredModule(initialGroup));
		log.info("... done adding underscored info.") ;

		log.info( "writing to file ...");
		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		new ConfigWriter( configV1 ).writeFileV1( v1path );
		log.info( "... done writing to file.");

		log.info( "creating new config ...");
		final Config configV1In = ConfigUtils.createConfig();
		log.info( "... done creating new config.");
		log.info( "read file into new config ...");
		new ConfigReader( configV1In ).readFile( v1path );
		log.info("... done reading file into new config.") ;

		log.info( "asserting ...");
		assertIdentical("re-read v1", initialGroup, configV1In.routing());
		log.info( "... done asserting.") ;

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";

		new ConfigWriter( configV1In ).writeFileV2( v2path );

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

	private static RoutingConfigGroup createTestConfigGroup() {
		log.info( "creating test config group ... ");

		final RoutingConfigGroup group = new RoutingConfigGroup();

		group.setNetworkModes( Arrays.asList( "electricity" , "water_supply" ) );

		// two modes with only one speed
		group.setTeleportedModeFreespeedFactor( "inline skate" , 0.1 );
//		group.getModeRoutingParams().get("inline skate").setBeelineDistanceFactor( 10000000. );

		group.setTeleportedModeSpeed( "ice skates" , 10 );
//		group.getModeRoutingParams().get("ice skates").setBeelineDistanceFactor( 10000000. );

		group.setBeelineDistanceFactor( 10000000 );

		// one mode with both speeds
		// Was made illegal: do not test
		//group.setTeleportedModeFreespeedFactor( "overboard" , 100 );
		//group.setTeleportedModeSpeed( "overboard" , 999 );

		log.info( "... done creating test config group.");
		return group;
	}
}
