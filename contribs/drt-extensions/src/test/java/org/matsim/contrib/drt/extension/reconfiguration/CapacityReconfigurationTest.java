package org.matsim.contrib.drt.extension.reconfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.reconfiguration.logic.DefaultCapacityReconfigurationLogic;
import org.matsim.contrib.drt.extension.reconfiguration.run.CapacityReconfigurationModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.DefaultDvrpLoadFromTrip;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class CapacityReconfigurationTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testWithoutReconfiguration() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		configureDrt(config, true);

		Controler controller = DrtControlerCreator.createControler(config, false);
		prepareLoads(controller.getScenario().getPopulation());

		ReconfigurationTracker tracker = ReconfigurationTracker.install(controller);

		controller.run();

		assertEquals(198, tracker.pickedUpPassengers);
		assertEquals(0, tracker.pickedUpGoods);
	}
	
	@Test
	void testSimpleReconfiguration() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		DrtConfigGroup drtConfig = configureDrt(config, true);

		Controler controller = DrtControlerCreator.createControler(config, false);
		prepareLoads(controller.getScenario().getPopulation());

		controller.addOverridingModule(new CapacityReconfigurationModule(drtConfig.mode, 300));
		SimpleReconfigurationLogic.install(controller, drtConfig.getMode());

		ReconfigurationTracker tracker = ReconfigurationTracker.install(controller);

		controller.run();

		assertEquals(88, tracker.pickedUpPassengers);
		assertEquals(106, tracker.pickedUpGoods);
	}

	@Test
	void testDefaultReconfiguration() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		DrtConfigGroup drtConfig = configureDrt(config, true);

		Controler controller = DrtControlerCreator.createControler(config, false);
		prepareLoads(controller.getScenario().getPopulation());

		controller.addOverridingModule(new CapacityReconfigurationModule(drtConfig.mode, 300));

		DefaultCapacityReconfigurationLogic.install(controller, drtConfig.getMode(), loadType -> {
			return Set.of( //
					DvrpLoadType.fromArray(loadType, 4, 0),
					DvrpLoadType.fromArray(loadType, 0, 4));
		});


		ReconfigurationTracker tracker = ReconfigurationTracker.install(controller);

		controller.run();

		assertEquals(186, tracker.pickedUpPassengers);
		assertEquals(167, tracker.pickedUpGoods);
	}

	@Test
	void testDefaultReconfigurationWithoutRejections() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		DrtConfigGroup drtConfig = configureDrt(config, false);

		Controler controller = DrtControlerCreator.createControler(config, false);
		prepareLoads(controller.getScenario().getPopulation());

		controller.addOverridingModule(new CapacityReconfigurationModule(drtConfig.mode, 300));

		DefaultCapacityReconfigurationLogic.install(controller, drtConfig.getMode(), loadType -> {
			return Set.of( //
					DvrpLoadType.fromArray(loadType, 4, 0),
					DvrpLoadType.fromArray(loadType, 0, 4));
		});


		ReconfigurationTracker tracker = ReconfigurationTracker.install(controller);

		controller.run();

		assertEquals(199, tracker.pickedUpPassengers);
		assertEquals(189, tracker.pickedUpGoods);
	}

	private static DrtConfigGroup configureDrt(Config config, boolean useRejections) {
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);

		// rejections?
		drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet().rejectRequestIfMaxWaitOrTravelTimeViolated = useRejections;

		// produce analysis output on capacities and loads
		drtConfig.loadParams.analysisInterval = 1;

		// set up two dimensions
		drtConfig.loadParams.dimensions = List.of("passengers", "goods");

		return drtConfig;
	}

	private static void prepareLoads(Population population) {
		for (Person person : population.getPersons().values()) {
			Id<Person> personId = person.getId();
			int personParity = Integer.parseInt(personId.toString());

			if (personParity % 2 == 0) {
				person.getAttributes().putAttribute(DefaultDvrpLoadFromTrip.LOAD_ATTRIBUTE, "passengers=1");
			} else {
				person.getAttributes().putAttribute(DefaultDvrpLoadFromTrip.LOAD_ATTRIBUTE, "goods=1");
			}
		}
	}
}
