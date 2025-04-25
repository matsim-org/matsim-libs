package org.matsim.contrib.drt.run;

import com.google.common.base.VerifyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class DrtConfigGroupTest {

	@Test
	void testHasDefaultDrtOptimizationConstraintsParam() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// get DrtOptimizationConstraintsParams
		DrtOptimizationConstraintsSet params = drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();

		Assertions.assertEquals(DrtOptimizationConstraintsSet.DEFAULT_PARAMS_NAME, params.getConstraintSetName());
		Assertions.assertThrows(VerifyException.class, () -> drtConfig.checkConsistency(config));
		drtConfig.setStopDuration(0);
		params.setMaxWaitTime(drtConfig.getStopDuration());
		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}

	@Test
	void testMultipleDrtOptimizationConstraintsParams() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drtConfig = new DrtConfigGroup();
		config.addModule(drtConfig);

		// add second DrtOptimizationConstraintsParams
		DrtOptimizationConstraintsSet params = new DrtOptimizationConstraintsSetImpl();
		params.setConstraintSetName("test");

		DrtOptimizationConstraintsParams optimizationConstraintsParams = drtConfig.addOrGetDrtOptimizationConstraintsParams();
		optimizationConstraintsParams.addParameterSet(params);

		//default not yet present
		Assertions.assertEquals(1, optimizationConstraintsParams.getDrtOptimizationConstraintsSets().size());

		DrtOptimizationConstraintsSet defaultConstraints = optimizationConstraintsParams.addOrGetDefaultDrtOptimizationConstraintsSet();
		Assertions.assertEquals(2, optimizationConstraintsParams.getDrtOptimizationConstraintsSets().size());

		drtConfig.setStopDuration(0);
		params.setMaxWaitTime(drtConfig.getStopDuration());
		defaultConstraints.setMaxWaitTime(drtConfig.getStopDuration());

		Assertions.assertDoesNotThrow(() -> drtConfig.checkConsistency(config));
	}
}
