package org.matsim.contrib.drt.optimizer.servicequality;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DrtServiceQualityProbeParamsTest {

	@Test
	void serviceQualityProbeParamsRoundTrip() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drt = new DrtConfigGroup();
		DrtServiceQualityProbeParams params = new DrtServiceQualityProbeParams();
		params.setWriteServiceQualityProbes(true);
		params.setServiceQualityProbeTimes("28800, 32400");
		params.setServiceQualityProbeSpatialResolution(DrtServiceQualityProbeParams.SpatialResolution.ZONE_TO_ZONE);
		params.setServiceQualityProbeZoneCellSize(5000.);
		drt.addParameterSet(params);
		config.addModule(drt);

		ConfigUtils.writeConfig(config, "target/service-quality-probe-config.xml");
		Config loaded = ConfigUtils.loadConfig("target/service-quality-probe-config.xml", new DrtConfigGroup());
		DrtServiceQualityProbeParams loadedParams = ConfigUtils.addOrGetModule(loaded, DrtConfigGroup.class)
			.getDrtServiceQualityProbeParams().orElseThrow();

		assertEquals("28800, 32400", loadedParams.getServiceQualityProbeTimes());
		assertEquals(DrtServiceQualityProbeParams.SpatialResolution.ZONE_TO_ZONE, loadedParams.getServiceQualityProbeSpatialResolution());
		assertEquals(5000., loadedParams.getServiceQualityProbeZoneCellSize());
	}

	@Test
	void repeatedSelectiveSearchIsRejectedWhenProbeIsEnabled() {
		Config config = ConfigUtils.createConfig();
		DrtConfigGroup drt = new DrtConfigGroup();
		DrtServiceQualityProbeParams probe = new DrtServiceQualityProbeParams();
		probe.setWriteServiceQualityProbes(true);
		probe.setServiceQualityProbeTimes("28800");
		drt.addParameterSet(probe);
		drt.addParameterSet(new RepeatedSelectiveInsertionSearchParams());
		config.addModule(drt);

		assertThrows(RuntimeException.class, config::checkConsistency);
	}
}
