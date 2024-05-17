package org.matsim.contrib.drt.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
		DrtOptimizationConstraintsParams params = drtConfig.getDefaultDrtOptimizationConstraintsParam();

		Assertions.assertEquals(DrtOptimizationConstraintsParams.DEFAULT_PARAMS_NAME, params.name);
		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}

	@Test
	void testMultipleDrtOptimizationConstraintsParams() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// add second DrtOptimizationConstraintsParams
		DrtOptimizationConstraintsParams params = new DrtOptimizationConstraintsParams();
		params.name = "test";
		drtConfig.addParameterSet(params);

		Assertions.assertEquals(2, drtConfig.getDrtOptimizationConstraintsParams().size());
		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}

	@Test
	void testNoDuplicateDrtDrtOptimizationConstraintsParams() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// add second DrtOptimizationConstraintsParams with same name
		DrtOptimizationConstraintsParams params = new DrtOptimizationConstraintsParams();
		params.name = DrtOptimizationConstraintsParams.DEFAULT_PARAMS_NAME;
		drtConfig.addParameterSet(params);

		Assertions.assertEquals(2, drtConfig.getDrtOptimizationConstraintsParams().size());
		Assertions.assertThrows(VerifyException.class, () -> drtConfig.checkConsistency(config));
	}
}
