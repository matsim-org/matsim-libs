package org.matsim.contrib.drt.prebooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.prebooking.PrebookingTestEnvironment.RequestInfo;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogicParams;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogicParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests for config-based prebooking logic installation (issue #4545).
 * Verifies that prebooking logic can be configured per DRT mode via config
 * parameter sets instead of manual programmatic installation.
 *
 * @author Samuel Hoenle (samuelhoenle)
 */
public class PrebookingLogicConfigTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void probabilityBasedLogicParams_defaultValues() {
		ProbabilityBasedPrebookingLogicParams params = new ProbabilityBasedPrebookingLogicParams();
		assertEquals(1.0, params.getProbability(), 1e-9);
		assertEquals(900.0, params.getSubmissionSlack(), 1e-9);
	}

	@Test
	void probabilityBasedLogicParams_setAndGet() {
		ProbabilityBasedPrebookingLogicParams params = new ProbabilityBasedPrebookingLogicParams();
		params.setProbability(0.5);
		params.setSubmissionSlack(1800.0);
		assertEquals(0.5, params.getProbability(), 1e-9);
		assertEquals(1800.0, params.getSubmissionSlack(), 1e-9);
	}

	@Test
	void prebookingParams_probabilityLogicRegistration() {
		PrebookingParams prebookingParams = new PrebookingParams();
		assertFalse(prebookingParams.getLogicParams().isPresent());
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertFalse(prebookingParams.getAttributeBasedLogicParams().isPresent());

		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(0.7);
		logicParams.setSubmissionSlack(600.0);
		prebookingParams.addParameterSet(logicParams);

		assertTrue(prebookingParams.getLogicParams().isPresent());
		assertTrue(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertFalse(prebookingParams.getAttributeBasedLogicParams().isPresent());
		assertEquals(0.7, prebookingParams.getProbabilityBasedLogicParams().get().getProbability(), 1e-9);
		assertEquals(600.0, prebookingParams.getProbabilityBasedLogicParams().get().getSubmissionSlack(), 1e-9);
	}

	@Test
	void prebookingParams_attributeLogicRegistration() {
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());

		assertTrue(prebookingParams.getLogicParams().isPresent());
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertTrue(prebookingParams.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void prebookingParams_oneOfManyConstraint() {
		PrebookingParams prebookingParams = new PrebookingParams();

		ProbabilityBasedPrebookingLogicParams probParams = new ProbabilityBasedPrebookingLogicParams();
		prebookingParams.addParameterSet(probParams);
		assertTrue(prebookingParams.getProbabilityBasedLogicParams().isPresent());

		// Adding another logic type without removing the first should throw
		AttributeBasedPrebookingLogicParams attrParams = new AttributeBasedPrebookingLogicParams();
		assertThrows(IllegalStateException.class, () -> prebookingParams.addParameterSet(attrParams));

		// After removing the first, adding the second should work
		prebookingParams.removeParameterSet(probParams);
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());

		prebookingParams.addParameterSet(attrParams);
		assertFalse(prebookingParams.getProbabilityBasedLogicParams().isPresent());
		assertTrue(prebookingParams.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void configRoundTrip_probabilityBased() {
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setMode("drt");
		multiModeDrtConfig.addParameterSet(drtConfig);

		PrebookingParams prebookingParams = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(0.42);
		logicParams.setSubmissionSlack(1234.0);
		prebookingParams.addParameterSet(logicParams);
		drtConfig.addParameterSet(prebookingParams);

		// Write and read config
		String configFile = utils.getOutputDirectory() + "/test_config.xml";
		ConfigUtils.writeConfig(config, configFile);

		Config readConfig = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup());
		MultiModeDrtConfigGroup readMultiModeDrt = MultiModeDrtConfigGroup.get(readConfig);
		DrtConfigGroup readDrtConfig = readMultiModeDrt.getModalElements().iterator().next();

		assertTrue(readDrtConfig.getPrebookingParams().isPresent());
		PrebookingParams readPrebooking = readDrtConfig.getPrebookingParams().get();
		assertTrue(readPrebooking.getProbabilityBasedLogicParams().isPresent());
		assertFalse(readPrebooking.getAttributeBasedLogicParams().isPresent());

		ProbabilityBasedPrebookingLogicParams readLogic = readPrebooking.getProbabilityBasedLogicParams().get();
		assertEquals(0.42, readLogic.getProbability(), 1e-9);
		assertEquals(1234.0, readLogic.getSubmissionSlack(), 1e-9);
	}

	@Test
	void configRoundTrip_attributeBased() {
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
		config.addModule(multiModeDrtConfig);

		DrtConfigGroup drtConfig = new DrtConfigGroup();
		drtConfig.setMode("drt");
		multiModeDrtConfig.addParameterSet(drtConfig);

		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());
		drtConfig.addParameterSet(prebookingParams);

		String configFile = utils.getOutputDirectory() + "/test_config.xml";
		ConfigUtils.writeConfig(config, configFile);

		Config readConfig = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup());
		MultiModeDrtConfigGroup readMultiModeDrt = MultiModeDrtConfigGroup.get(readConfig);
		DrtConfigGroup readDrtConfig = readMultiModeDrt.getModalElements().iterator().next();

		assertTrue(readDrtConfig.getPrebookingParams().isPresent());
		PrebookingParams readPrebooking = readDrtConfig.getPrebookingParams().get();
		assertFalse(readPrebooking.getProbabilityBasedLogicParams().isPresent());
		assertTrue(readPrebooking.getAttributeBasedLogicParams().isPresent());
	}

	@Test
	void endToEnd_probabilityBasedFromConfig() {
		/*-
		 * Test that probability-based logic configured via config produces the same
		 * result as the manual installation. All trips are prebooked (probability=1.0)
		 * with a submission slack of 900 seconds.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking with probability-based logic via config
		PrebookingParams prebookingParams = new PrebookingParams();
		ProbabilityBasedPrebookingLogicParams logicParams = new ProbabilityBasedPrebookingLogicParams();
		logicParams.setProbability(1.0);
		logicParams.setSubmissionSlack(900.0);
		prebookingParams.addParameterSet(logicParams);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(prebookingParams);

		controller.run();

		// The request should have been prebooked
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		// Submission time = departure - slack = 2000 - 900 = 1100
		assertEquals(1100.0, requestInfo.submissionTime, 1e-3);
		assertFalse(Double.isNaN(requestInfo.pickupTime));
		assertFalse(Double.isNaN(requestInfo.dropoffTime));
	}

	@Test
	void endToEnd_attributeBasedFromConfig() {
		/*-
		 * Test that attribute-based logic configured via config works correctly.
		 * The request has submission time and planned departure time set as attributes.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking with attribute-based logic via config
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.addParameterSet(new AttributeBasedPrebookingLogicParams());

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(prebookingParams);

		controller.run();

		// The request should have been prebooked with submission time from attribute
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfo.submissionTime, 1e-3);
		assertFalse(Double.isNaN(requestInfo.pickupTime));
		assertFalse(Double.isNaN(requestInfo.dropoffTime));
	}

	@Test
	void endToEnd_noLogicConfigured_backwardCompatible() {
		/*-
		 * Test that when no logic parameter set is configured, the prebooking
		 * infrastructure is still set up (for manual installation) but no logic
		 * is auto-installed.
		 */

		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				.addRequest("personA", 0, 0, 5, 5, 2000.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();

		// Install prebooking without any logic (backward-compatible scenario)
		PrebookingTest.installPrebooking(controller, false);

		controller.run();

		// Without any prebooking logic, the request is submitted at departure time
		RequestInfo requestInfo = environment.getRequestInfo().get("personA");
		assertEquals(2000.0, requestInfo.submissionTime, 1e-3);
	}
}
