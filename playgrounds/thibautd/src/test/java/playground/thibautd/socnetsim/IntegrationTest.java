/* *********************************************************************** *
 * project: org.matsim.*
 * IntegrationTest.java
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
package playground.thibautd.socnetsim;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.security.DigestInputStream;
import java.security.MessageDigest;

import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.run.RunCliquesWithHardCodedStrategies;
import playground.thibautd.socnetsim.run.RunCliquesWithHardCodedStrategies.Weights;

/**
 * Tests that the results are deterministic, by running several runs in numerous settings.
 *
 * Currently (jan. 2012), it shows that when using joint trip mutation with optimization,
 * the results are non-deterministic.
 * @author thibautd
 */
public class IntegrationTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private final class SimulationParameters {
		public Weights weights = new Weights();
		public int nIterations = 10;
		public int nThreads = 4;
		public int agentsMemorySize = 3;

		@Override
		public String toString() {
			return weights+", "+
				nIterations+" iterations, "+
				nThreads+" threads, "+
				agentsMemorySize+" plans per agent";
		}
	}

	@Test
	public void testDeterminismFullRun() throws Exception {
		testDeterminism( new SimulationParameters() );
	}

	@Test
	public void testDeterminismOneThread() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.nThreads = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismNoJointTrips() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.jointTripMutation = 0;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismNoLogit() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.logitSelection = 0;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismNoRemoval() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.agentsMemorySize = 0;
		testDeterminism( params );
	}

	@Test
	@Ignore( "this is too expensive. It fails." )
	public void testDeterminismJointTrips() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.setAllToZero();
		params.weights.jointTripMutation = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismNonOptJointTrips() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.setAllToZero();
		params.weights.jointTripMutation = 1;
		params.weights.jtmOptimizes = false;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismTimeMutationNoRemoval() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.agentsMemorySize = 0;
		params.weights.setAllToZero();
		params.weights.timeMutator = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismModeChoiceNoRemoval() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.agentsMemorySize = 0;
		params.weights.setAllToZero();
		params.weights.modeMutation = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismReRouteNoRemoval() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.agentsMemorySize = 0;
		params.weights.setAllToZero();
		params.weights.reRoute = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismReRouteRemoval() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.setAllToZero();
		params.weights.reRoute = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismReRouteLogit() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.setAllToZero();
		params.weights.reRoute = 1;
		params.weights.logitSelection = 1;
		testDeterminism( params );
	}

	@Test
	public void testDeterminismWithoutInnovation() throws Exception {
		SimulationParameters params = new SimulationParameters();
		params.weights.setAllToZero();
		testDeterminism( params );
	}

	public void testDeterminism(final SimulationParameters params) throws Exception {
		final String configFile = utils.getPackageInputDirectory()+"/config.xml";

		byte[] sha1 = null;

		// Risks leading to unnoticed random results:
		// - too few iterations/agents lead to problematic module combinations not being tested
		// - too few iterations/too large memory leads to plan removal not being executed
		// - too small memory reduces the effect of random plan selection
		// - too small number of threads reduces the effect of concurrency
		for (int i=0; i < 4; i++) {
			// This is normally useless. But still, just to be sure we "start" always
			// in the same state.
			MatsimRandom.reset();
			final Scenario sc = RunCliquesWithHardCodedStrategies.createScenario( configFile );
			sc.getConfig().controler().setOutputDirectory( utils.getOutputDirectory() );
			sc.getConfig().strategy().setMaxAgentPlanMemorySize( params.agentsMemorySize );
			sc.getConfig().controler().setFirstIteration( 0 );
			sc.getConfig().controler().setLastIteration( params.nIterations );
			sc.getConfig().global().setNumberOfThreads( params.nThreads );

			RunCliquesWithHardCodedStrategies.runScenario( sc , params.weights );

			final byte[] newSha1 = calcSha1( utils.getOutputDirectory()+"/output_plans.xml.gz" );
			if ( sha1 != null ) {
				assertTrue(
						"the runs produced different final plans with params "+params,
						MessageDigest.isEqual(
							sha1,
							newSha1 ));
			}
			IOUtils.deleteDirectory( new File( utils.getOutputDirectory() ) );

			if (newSha1 == null) throw new RuntimeException( "got a null checksum!" );
			if (newSha1.length == 0) throw new RuntimeException( "got a zero-length checksum!" );
			sha1 = newSha1;
		}
	}

	private static byte[] calcSha1( final String fileName ) throws Exception {
		final MessageDigest sha1 = MessageDigest.getInstance( "SHA1" );
		DigestInputStream stream =
			new DigestInputStream(
					new BufferedInputStream(
						new FileInputStream(
							fileName ) ),
					sha1);

		while (stream.read() != -1);
		return sha1.digest();
	}
}

