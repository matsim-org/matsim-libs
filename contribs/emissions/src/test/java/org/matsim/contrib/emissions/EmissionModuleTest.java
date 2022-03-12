package org.matsim.contrib.emissions;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.TestUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

/**
 * Most of the other test implicitly test the EmissionModule as well. Still, I guess it makes sense to have this here
 */
public class EmissionModuleTest {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test(expected = RuntimeException.class)
    public void testWithIncorrectNetwork() {

        var scenarioURL = ExamplesUtils.getTestScenarioURL("emissions-sampleScenario");

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.none);
        emissionConfig.setAverageColdEmissionFactorsFile(IOUtils.extendUrl(scenarioURL, "sample_41_EFA_ColdStart_SubSegm_2020detailed.txt").toString());
        emissionConfig.setAverageWarmEmissionFactorsFile(IOUtils.extendUrl(scenarioURL, "sample_41_EFA_HOT_vehcat_2020average.txt").toString());
        emissionConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);

        var config = ConfigUtils.createConfig(emissionConfig);

        // create a scenario with a random network where every link has an hebefa road type except one link.
        var scenario = ScenarioUtils.createMutableScenario(config);
        var network = TestUtils.createRandomNetwork(1000, 10000, 10000);
        new VspHbefaRoadTypeMapping().addHbefaMappings(network);
        network.getLinks().values().iterator().next().getAttributes().removeAttribute(EmissionUtils.HBEFA_ROAD_TYPE);
        scenario.setNetwork(network);

        var eventsManager = EventsUtils.createEventsManager();


        var module = new AbstractModule() {
            @Override
            public void install() {
                bind(Scenario.class).toInstance(scenario);
                bind(EventsManager.class).toInstance(eventsManager);
                bind(EmissionModule.class);
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);
        // this call should cause an exception when the consistency check in the constructor of the emission module fails
        injector.getInstance(EmissionModule.class);

        fail("Was expecting exception");
    }
}