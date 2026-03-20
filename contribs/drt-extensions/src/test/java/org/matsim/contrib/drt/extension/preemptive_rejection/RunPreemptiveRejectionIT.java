package org.matsim.contrib.drt.extension.preemptive_rejection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import scala.util.Random;

public class RunPreemptiveRejectionIT {
    @RegisterExtension
    public final MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    void testPreemptiveRejection() {
        URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

        Config config = ConfigUtils.loadConfig(configUrl,
                new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new), new DvrpConfigGroup(),
                new OTFVisConfigGroup());

        config.controller()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setOutputDirectory(utils.getOutputDirectory());
        config.controller().setLastIteration(0);

        DrtConfigGroup drtConfig = (DrtWithExtensionsConfigGroup) DrtConfigGroup.getSingleModeDrtConfig(config);
        drtConfig.setVehiclesFile("vehicles-5-cap-2.xml");

        PreemptiveRejectionParams preemptiveParams = new PreemptiveRejectionParams();
        drtConfig.addParameterSet(preemptiveParams);

        String path = RunPreemptiveRejectionIT.class.getResource("example.json").toString();
        preemptiveParams.setInputPath(path);

        Controler controller = DrtControlerCreator.createControler(config, false);
        controller.addOverridingModule(new PreemptiveRejectionModule());

        Random random = new Random(0);
        for (Person person : controller.getScenario().getPopulation().getPersons().values()) {
            String bookingClass = random.nextDouble() < 0.5 ? "business" : "leisure";
            PreemptiveRejectionOptimizer.setBookingClass(person, bookingClass);
        }

        AtomicInteger leisureRejections = new AtomicInteger();
        AtomicInteger businessRejections = new AtomicInteger();

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(new PassengerRequestRejectedEventHandler() {
                    @Override
                    public void handleEvent(PassengerRequestRejectedEvent event) {
                        String bookingClass = PreemptiveRejectionOptimizer
                                .getBookingClass(controller.getScenario().getPopulation(), event.getPersonIds());
                        (bookingClass.equals("leisure") ? leisureRejections : businessRejections).incrementAndGet();
                    }
                });
            }
        });

        controller.run();

        // values without preemptive rejection
        assertNotEquals(56, leisureRejections.get());
        assertNotEquals(71, businessRejections.get());

        // values with preemptive rejection
        assertEquals(46, leisureRejections.get());
        assertEquals(108, businessRejections.get());
    }
}
