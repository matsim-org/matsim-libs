package org.matsim.simwrapper;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;


public class SimWrapperConfigGroupTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void config() {

		Config config = ConfigUtils.createConfig();
		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		sw.defaultParams().sampleSize = 0.5;

		SimWrapperConfigGroup.ContextParams p = sw.get("new");
		p.set("dynamic", "value");

		Assertions.assertThat(p.sampleSize)
			.isEqualTo(0.5);

		String path = utils.getOutputDirectory() + "/config.xml";

		ConfigUtils.writeConfig(config, path);

		Config loaded = ConfigUtils.loadConfig(path);
		sw = ConfigUtils.addOrGetModule(loaded, SimWrapperConfigGroup.class);

		Assertions.assertThat(sw.get("new").getOrDefault("dynamic", null))
			.isEqualTo("value");

	}
}
