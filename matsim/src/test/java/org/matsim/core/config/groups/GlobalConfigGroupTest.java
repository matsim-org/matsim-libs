package org.matsim.core.config.groups;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.testcases.MatsimTestUtils;

class GlobalConfigGroupTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void persistsBaseCasePathName() {
		Config config = ConfigUtils.createConfig();
		config.global().setBaseCasePathName("../base-case");
		String file = utils.getOutputDirectory() + "/config.xml";

		new ConfigWriter(config).write(file);

		Config loaded = ConfigUtils.loadConfig(file);
		Assertions.assertThat(loaded.global().getBaseCasePathName()).isEqualTo("../base-case");
	}
}
