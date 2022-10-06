/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyConfigGroupTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Test for {@link StrategyConfigGroup}.
 *
 * @author mrieser
 */
public class StrategyConfigGroupTest {

	private static final Logger log = LogManager.getLogger(StrategyConfigGroupTest.class);
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Tests that only the known param-names are accepted, and no others.
	 *
	 * @author mrieser
	 */
	@Test
	public void testParamNames() {
		StrategyConfigGroup configGroup = new StrategyConfigGroup();
		configGroup.addParam("maxAgentPlanMemorySize", "3");
		configGroup.addParam("Module_1", "ReRoute");
		configGroup.addParam("ModuleProbability_1", "0.5");
		configGroup.addParam("ModuleDisableAfterIteration_1", "20");
		try {
			configGroup.addParam("ModuleWrong_1", "should fail");
			fail("Expected to get an IllegalArgumentException, but got none.");
		} catch (IllegalArgumentException e) {
			log.info("Catched IllegalArgumentException, as expected: "
					+ e.getMessage());
		}
		assertEquals("unexpected number of strategy settings", 1, configGroup
				.getStrategySettings().size());
	}

	/**
	 * Tests that inconsistent configuration states are recognized, like
	 * missing settings or wrong enumeration.
	 *
	 * @author mrieser
	 */
	@Test
	public void testCheckConsistency() {
		// start with a simple configuration with exactly one module defined
		StrategyConfigGroup configGroup = new StrategyConfigGroup();
		configGroup.addParam("maxAgentPlanMemorySize", "3");
		configGroup.addParam("Module_1", "ReRoute");
		configGroup.addParam("ModuleProbability_1", "0.5");
		configGroup.addParam("ModuleDisableAfterIteration_1", "20");
		configGroup.checkConsistency(ConfigUtils.createConfig());

		// add a 2nd module
		configGroup.addParam("Module_2", "TimeAllocationMutator");
		configGroup.addParam("ModuleProbability_2", "0.4");
		configGroup.checkConsistency(ConfigUtils.createConfig());

		// add a 3rd module, but inconsistent with the enumeration
		// --- no restrictions on indices anymore. td, sep'14
		//configGroup.addParam("Module_4", "SelectRandom");
		//configGroup.addParam("ModuleProbability_4", "0.3");
		//try {
		//	configGroup.checkConsistency();
		//	fail("Expected to fail consistency check with missing Module_3, but did not fail.");
		//} catch (RuntimeException e) {
		//	log.info("Catched RuntimeException, as expected: " + e.getMessage());
		//}

		// fix the configuration by adding the missing module
		configGroup.addParam("Module_3", "KeepLastSelected");
		configGroup.addParam("ModuleProbability_3", "0.2");
		configGroup.checkConsistency(ConfigUtils.createConfig());

		// break the configuration by adding an incomplete module
		configGroup.addParam("Module_5", "SelectBest");
		try {
			configGroup.checkConsistency(ConfigUtils.createConfig());
			fail("Expected to fail consistency check with incomplete Module_5, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}

		// fix Module_5
		configGroup.addParam("ModuleProbability_5", "0.0");
		configGroup.checkConsistency(ConfigUtils.createConfig());

		// add another incomplete module
		configGroup.addParam("ModuleProbability_6", "0.1");
		try {
			configGroup.checkConsistency(ConfigUtils.createConfig());
			fail("Expected to fail consistency check with incomplete Module_6, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}

		// fix Module_6
		configGroup.addParam("Module_6", "SelectExpBeta");
		configGroup.checkConsistency(ConfigUtils.createConfig());

		// add forbidden Module_0
		// --- no restrictions on indices anymore. td, sep'14
		//configGroup.addParam("Module_0", "ChangeExpBeta");
		//configGroup.addParam("ModuleProbability_0", "0.6");
		//try {
		//	configGroup.checkConsistency();
		//	fail("Expected to fail consistency check with Module_0, but did not fail.");
		//} catch (RuntimeException e) {
		//	log.info("Catched RuntimeException, as expected: " + e.getMessage());
		//}
	}

	@Test
	public void testIOWithFormatChange() {
		final StrategyConfigGroup initialGroup = createTestConfigGroup();

		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		final Config configV1 = new Config();
		configV1.addModule(toUnderscoredModule(initialGroup));

		new ConfigWriter( configV1 ).writeFileV1( v1path );

		final Config configV1In = ConfigUtils.createConfig();
		new ConfigReader( configV1In ).readFile( v1path );

		assertIdentical("re-read v1", initialGroup, configV1In.strategy());

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";

		new ConfigWriter( configV1In ).writeFileV2( v2path );

		final Config configV2 = ConfigUtils.createConfig();
		new ConfigReader( configV2 ).readFile( v2path );

		assertIdentical("re-read v2", initialGroup, configV2.strategy());
	}

	private void assertIdentical(
			final String msg,
			final StrategyConfigGroup initialGroup,
			final StrategyConfigGroup inputConfigGroup) {
		assertEquals(
				"wrong config template for "+msg,
				initialGroup.getExternalExeConfigTemplate(),
				inputConfigGroup.getExternalExeConfigTemplate() );

		assertEquals(
				"wrong ExternalExeTimeOut for "+msg,
				initialGroup.getExternalExeTimeOut(),
				inputConfigGroup.getExternalExeTimeOut() );

		assertEquals(
				"wrong ExternalExeTmpFileRootDir for "+msg,
				initialGroup.getExternalExeTmpFileRootDir(),
				inputConfigGroup.getExternalExeTmpFileRootDir() );

		assertEquals(
				"wrong FractionOfIterationsToDisableInnovation for "+msg,
				initialGroup.getFractionOfIterationsToDisableInnovation(),
				inputConfigGroup.getFractionOfIterationsToDisableInnovation(),
				MatsimTestUtils.EPSILON );

		assertEquals(
				"wrong MaxAgentPlanMemorySize for "+msg,
				initialGroup.getMaxAgentPlanMemorySize(),
				inputConfigGroup.getMaxAgentPlanMemorySize() );

		assertEquals(
				"wrong PlanSelectorForRemoval for "+msg,
				initialGroup.getPlanSelectorForRemoval(),
				inputConfigGroup.getPlanSelectorForRemoval() );

		assertEquals(
				"wrong number of StrategySettings for "+msg,
				initialGroup.getStrategySettings().size(),
				inputConfigGroup.getStrategySettings().size() );
	}

	private ConfigGroup toUnderscoredModule(final StrategyConfigGroup initialGroup) {
		// yyyy is this method/this execution path still necessary?  Maybe we need to be able to read config v1, but certainly
		// we don't need to WRITE it, do we?  kai/mz, nov'15
		
		final ConfigGroup module = new ConfigGroup( initialGroup.getName() );

		for ( Map.Entry<String, String> e : initialGroup.getParams().entrySet() ) {
			log.info( "add param "+e.getKey() );
			module.addParam( e.getKey() , e.getValue() );
		}

		for ( StrategySettings settings : initialGroup.getStrategySettings() ) {
			final Id<StrategySettings> id = settings.getId();
			module.addParam( "Module_"+id , settings.getStrategyName() );
			module.addParam( "ModuleProbability_"+id , ""+settings.getWeight() );
			module.addParam( "ModuleDisableAfterIteration_"+id , ""+settings.getDisableAfter() );
			module.addParam( "ModuleExePath_"+id , settings.getExePath() );
			module.addParam( "ModuleSubpopulation_"+id , settings.getSubpopulation() );
		}

		return module;
	}

	private StrategyConfigGroup createTestConfigGroup() {
		final StrategyConfigGroup group = new StrategyConfigGroup();
		group.setExternalExeConfigTemplate( "bwark" );
		group.setExternalExeTimeOut( 999 );
		group.setExternalExeTmpFileRootDir( "some/random/location" );
		group.setFractionOfIterationsToDisableInnovation( 8 );
		group.setMaxAgentPlanMemorySize( 999999 );
		group.setPlanSelectorForRemoval( "SelectSomeArbitraryPlan" );

		/* scope of settings: minimal */ {
			final StrategySettings settings = new StrategySettings();
			settings.setStrategyName( "MyModule" );
			settings.setWeight( 10. );
			group.addStrategySettings( settings );
		}

		/* scope of settings: all options */ {
			final StrategySettings settings = new StrategySettings();
			settings.setStrategyName( "YourModule" );
			settings.setWeight( 0 );
			settings.setDisableAfter( 10 );
			settings.setExePath( "path/to/nowhere/" );
			settings.setSubpopulation( "sushi_eaters" );
			group.addStrategySettings( settings );
		}

		return group;
	}
}
