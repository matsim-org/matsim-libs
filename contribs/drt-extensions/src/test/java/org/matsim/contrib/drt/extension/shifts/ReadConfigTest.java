package org.matsim.contrib.drt.extension.shifts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.extension.shifts.config.DrtShiftParams;
import org.matsim.contrib.drt.extension.shifts.config.DrtWithShiftsConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class ReadConfigTest extends MatsimTestCase {

	private static final String TESTXMLOUTPUT  = "config.xml";

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(DrtWithShiftsConfigGroup::new),
				new DvrpConfigGroup(), new OTFVisConfigGroup());

		MultiModeDrtConfigGroup multiModeDrtConfigGroupOut = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup modalElement : multiModeDrtConfigGroupOut.getModalElements()) {
			ConfigGroup parameterSet = modalElement.createParameterSet(DrtShiftParams.SET_NAME);
			modalElement.addParameterSet(parameterSet);
			DrtShiftParams drtShiftParams = ((DrtWithShiftsConfigGroup) modalElement).getDrtShiftParams();
			drtShiftParams.setChangeoverDuration(42);
		}

		String outfilename = this.getOutputDirectory() + TESTXMLOUTPUT;

		new ConfigWriter(config).write(outfilename);

		Config configIn = ConfigUtils.loadConfig(outfilename, new MultiModeDrtConfigGroup(DrtWithShiftsConfigGroup::new),
				new DvrpConfigGroup(), new OTFVisConfigGroup());
		MultiModeDrtConfigGroup multiModeDrtConfigGroupIn = MultiModeDrtConfigGroup.get(configIn);
		for (DrtConfigGroup modalElement : multiModeDrtConfigGroupIn.getModalElements()) {
			DrtShiftParams drtShiftParams = ((DrtWithShiftsConfigGroup) modalElement).getDrtShiftParams();
			Assert.assertEquals(42, drtShiftParams.getChangeoverDuration(), 0);
		}
	}
}
