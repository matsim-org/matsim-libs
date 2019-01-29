package org.matsim.contrib.cadyts.car;

import org.junit.Ignore;
import org.junit.Test;
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

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

public class CadytsCarWithPtScenarioIT {

    @Test @Ignore
    public void testCadytsWithPtVehicles() {
        final Config config = ConfigUtils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml"));
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setLastIteration(0);
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
        assumeThat("There's at least one bus on the test link", bussesSeenOnLink.get(), is(greaterThan(0)));
        assertTrue("This test runs to the end, meaning cadyts doesn't throw an exception with pt", true);
    }

}
