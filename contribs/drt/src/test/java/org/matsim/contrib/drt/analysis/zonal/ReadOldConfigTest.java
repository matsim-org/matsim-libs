package org.matsim.contrib.drt.analysis.zonal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;

/**
 * @author nkuehnel / MOIA
 */
public class ReadOldConfigTest {

    @Test
    public void test() {
        URL context = ExamplesUtils.getTestScenarioURL("kelheim");
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config-with-drt_old.xml"));
        Assertions.assertThatNoException().isThrownBy(() -> ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class));
        Assertions.assertThatNoException().isThrownBy(() -> ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class));
    }
}
