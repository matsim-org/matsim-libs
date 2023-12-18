package org.matsim.contrib.cadyts.car;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.examples.ExamplesUtils;

public class CadytsCarWithPtScenarioIT {

	@Test
	@Disabled
	void testCadytsWithPtVehicles() {
        final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml"));
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setLastIteration(0);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        final Counts<Link> calibrationCounts = new Counts<>();
        final Id<Link> testLink = Id.createLinkId("6_1");
        final Count<Link> count = calibrationCounts.createAndAddCount(testLink, "testStation");
        for (int i=1; i<24; i++) {
            count.createVolume(i, 10.0);
        }
        final Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CadytsCarModule(calibrationCounts));
        AtomicInteger bussesSeenOnLink = new AtomicInteger();
        controler.getEvents().addHandler((LinkLeaveEventHandler) event -> {
            if (event.getLinkId().equals(testLink) && event.getVehicleId().toString().startsWith("bus")) {
                bussesSeenOnLink.incrementAndGet();
            }
        });
        controler.run();
        assertTrue(bussesSeenOnLink.get() > 0, "There's at least one bus on the test link");
        assertTrue(true, "This test runs to the end, meaning cadyts doesn't throw an exception with pt");
    }

}
