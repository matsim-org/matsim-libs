package org.matsim.contrib.drt.extension.shifts;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.extension.shifts.config.DrtShiftParams;
import org.matsim.contrib.drt.extension.shifts.config.DrtWithShiftsConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class ReadConfigTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(DrtWithShiftsConfigGroup::new),
				new DvrpConfigGroup(), new OTFVisConfigGroup());

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup modalElement : multiModeDrtConfigGroup.getModalElements()) {
			DrtShiftParams drtShiftParams = ((DrtWithShiftsConfigGroup) modalElement).getDrtShiftParams();
			drtShiftParams.setAllowInFieldChangeover(false);
		}
	}
}
