package org.matsim.contrib.drt.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import com.google.common.base.VerifyException;

class DrtConfigGroupTest {

	@Test
	void testHasDefaultDrtOptimizationConstraintsParam() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// get DrtOptimizationConstraintsParams
		DrtOptimizationConstraintsSet params = drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();

		Assertions.assertEquals(DrtOptimizationConstraintsParams.defaultConstraintSet, params.name);
		Assertions.assertThrows(VerifyException.class, () -> drtConfig.checkConsistency(config));
		drtConfig.stopDuration = 0;
		params.maxWaitTime = drtConfig.stopDuration;
		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}

	@Test
	void testMultipleDrtOptimizationConstraintsParams() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// add second DrtOptimizationConstraintsParams
		DrtOptimizationConstraintsSet params = new DefaultDrtOptimizationConstraintsSet();
		params.name = "test";

		DrtOptimizationConstraintsParams optimizationConstraintsParams = drtConfig.addOrGetDrtOptimizationConstraintsParams();
		optimizationConstraintsParams.addParameterSet(params);

		//default not yet present
		Assertions.assertEquals(1, optimizationConstraintsParams.getDrtOptimizationConstraintsSets().size());

		DrtOptimizationConstraintsSet defaultConstraints = optimizationConstraintsParams.addOrGetDefaultDrtOptimizationConstraintsSet();
		Assertions.assertEquals(2, optimizationConstraintsParams.getDrtOptimizationConstraintsSets().size());

		drtConfig.stopDuration = 0;
		params.maxWaitTime = drtConfig.stopDuration;
		defaultConstraints.maxWaitTime = drtConfig.stopDuration;

		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}

	@Test
	void testNoDuplicateDrtDrtOptimizationConstraintsParams() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// add second DrtOptimizationConstraintsParams with same name
		DrtOptimizationConstraintsSet params = new DefaultDrtOptimizationConstraintsSet();
		params.name = DrtOptimizationConstraintsSet.DEFAULT_PARAMS_NAME;

		DrtOptimizationConstraintsParams optimizationConstraintsParams = drtConfig.addOrGetDrtOptimizationConstraintsParams();
		optimizationConstraintsParams.addOrGetDefaultDrtOptimizationConstraintsSet();
		optimizationConstraintsParams.addParameterSet(params);

		Assertions.assertEquals(2, optimizationConstraintsParams.getDrtOptimizationConstraintsSets().size());
		Assertions.assertThrows(VerifyException.class, () -> drtConfig.checkConsistency(config));
	}
}
