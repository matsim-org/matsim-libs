package org.matsim.contrib.drt.run.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.passenger.DefaultDvrpLoadFromDrtPassengers;
import org.matsim.contrib.drt.taas.capacities.CapacityChangeSchedulerEngine;
import org.matsim.contrib.drt.taas.capacities.CapacityReconfigurationLogic;
import org.matsim.contrib.drt.passenger.DvrpLoadFromDrtPassengers;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.taas.capacities.DefaultCapacityConfigurationLogic;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunDrtExampleWithChangingCapacitiesTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private record SimpleCapacityConfiguration(Id<Link> linkId) implements CapacityReconfigurationLogic {

		@Override
			public IdMap<DvrpVehicle, DvrpLoad> getOverriddenStartingCapacities() {
				return new IdMap<>(DvrpVehicle.class);
			}

			@Override
			public List<CapacityChangeItem> getPreScheduledCapacityChanges(DvrpVehicle dvrpVehicle) {
				DvrpLoad newVehicleLoad;
				if (dvrpVehicle.getCapacity() instanceof PersonsDvrpLoad personsDvrpVehicleLoad) {
					newVehicleLoad = new GoodsDvrpLoadType().fromInt(personsDvrpVehicleLoad.getLoad());
				} else if (dvrpVehicle.getCapacity() instanceof GoodsDvrpLoad goodsDvrpVehicleLoad) {
					newVehicleLoad = new PersonsDvrpLoadType().fromInt(goodsDvrpVehicleLoad.getLoad());
				} else {
					throw new IllegalStateException();
				}
				return List.of(new CapacityChangeItem(12 * 3600, this.linkId, newVehicleLoad));
			}
		}


	static class PersonsDvrpLoadType extends IntegerLoadType {

		public static final String TYPE_NAME = "personsLoad";

		public PersonsDvrpLoadType() {
			super(Id.create(TYPE_NAME, DvrpLoadType.class), "persons");
		}

		@Override
		public PersonsDvrpLoad fromInt(int load) {
			return new PersonsDvrpLoad(load, this);
		}
	}

	static class PersonsDvrpLoad extends IntegerLoad {

		public PersonsDvrpLoad(int capacity, PersonsDvrpLoadType factory) {
			super(capacity, factory);
		}
	}

	static class GoodsDvrpLoadType extends IntegerLoadType {

		public static final String TYPE_NAME = "goodsLoad";

		public GoodsDvrpLoadType() {
			super(Id.create(TYPE_NAME, DvrpLoadType.class), "goods");
		}

		@Override
		public GoodsDvrpLoad fromInt(int load) {
			return new GoodsDvrpLoad(load, this);
		}
	}

	static class GoodsDvrpLoad extends IntegerLoad {

		public GoodsDvrpLoad(int capacity, GoodsDvrpLoadType factory) {
			super(capacity, factory);
		}
	}

	static class ParityBasedDvrpLoadFromDrtPassengers implements DvrpLoadFromDrtPassengers {

		private final IntegerLoadType first;
		private final IntegerLoadType second;
		public ParityBasedDvrpLoadFromDrtPassengers(IntegerLoadType first, IntegerLoadType second) {
			this.first = first;
			this.second = second;
		}
		@Override
		public DvrpLoad getLoad(Collection<Id<Person>> personIds) {
			assert personIds.size() > 0;
			Id<Person> personId = personIds.stream().findFirst().get();
			int personParity=0;
			try {
				personParity = Integer.parseInt(personId.toString());
			} catch (NumberFormatException ignored) {

			}
			if(personParity%2 == 0) {
				return first.fromInt(personIds.size());
			} else {
				return second.fromInt(personIds.size());
			}
		}
	}

	static class CustomLoadsModule extends AbstractDvrpModeModule {

		private final boolean useSimpleCapacityConfigurationLogic;
		protected CustomLoadsModule(boolean useSimpleCapacityConfigurationLogic) {
			super("drt");
			this.useSimpleCapacityConfigurationLogic = useSimpleCapacityConfigurationLogic;
		}

		@Override
		public void install() {
			bindModal(PersonsDvrpLoadType.class).toInstance(new PersonsDvrpLoadType());
			bindModal(GoodsDvrpLoadType.class).toInstance(new GoodsDvrpLoadType());
			// All vehicles start with a compatibility with persons
			bindModal(IntegerLoadType.class).to(PersonsDvrpLoadType.class);

			bindModal(DvrpLoadSerializer.class).toProvider(modalProvider(getter -> {
				PersonsDvrpLoadType personsDvrpLoadType = getter.getModal(PersonsDvrpLoadType.class);
				GoodsDvrpLoadType goodsDvrpLoadType = getter.getModal(GoodsDvrpLoadType.class);
				return new DefaultDvrpLoadSerializer(personsDvrpLoadType, goodsDvrpLoadType);
			})).asEagerSingleton();

			//bindModal(DvrpLoadFromDrtPassengers.class).toProvider(modalProvider(getter -> new ParityBasedDvrpLoadFromDrtPassengers(getter.getModal(PersonsDvrpLoadType.class), getter.getModal(GoodsDvrpLoadType.class))));

			if(useSimpleCapacityConfigurationLogic) {
				bindModal(CapacityReconfigurationLogic.class).toProvider(modalProvider(getter -> {
					Set<Id<Link>> linkIds = getter.getModal(Network.class).getLinks().keySet();
					assert linkIds.size() > 0;
					Id<Link> linkId = linkIds.stream().findFirst().get();
					return new SimpleCapacityConfiguration(linkId);
				}));
			} else {
				bindModal(CapacityReconfigurationLogic.class).toProvider(modalProvider(getter -> {
					FleetSpecification fleetSpecification = getter.getModal(FleetSpecification.class);
					PersonsDvrpLoadType personsDvrpLoadType = getter.getModal(PersonsDvrpLoadType.class);
					GoodsDvrpLoadType goodsDvrpLoadType = getter.getModal(GoodsDvrpLoadType.class);
					Set<DvrpLoad> possibleCapacities = Set.of(personsDvrpLoadType.fromInt(4), goodsDvrpLoadType.fromInt(4));
					DvrpLoadFromDrtPassengers dvrpLoadFromDrtPassengers = getter.getModal(DvrpLoadFromDrtPassengers.class);
					Network network = getter.getModal(Network.class);
					return new DefaultCapacityConfigurationLogic(fleetSpecification,
						possibleCapacities, dvrpLoadFromDrtPassengers, network, network.getLinks().values(), 7200, true,
						DefaultCapacityConfigurationLogic.CapacityChangeLinkSelection.RANDOM);
				}));
			}

			installOverridingQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
				@Override
				protected void configureQSim() {
					bindModal(Fleet.class).toProvider(modalProvider(
						getter -> {
							Network network = getter.getModal(Network.class);
							return Fleets.createCustomFleet(getter.getModal(FleetSpecification.class),
								dvrpVehicleSpecification -> new DvrpVehicleWithChangeableCapacityImpl(dvrpVehicleSpecification, network.getLinks().get(dvrpVehicleSpecification.getStartLinkId())));
						})).asEagerSingleton();

					// This engine will plan the vehicles to change their capacities during the simulation
					addModalComponent(CapacityChangeSchedulerEngine.class, modalProvider(getter -> {
						Fleet fleet = getter.getModal(Fleet.class);
						Network network = getter.getModal(Network.class);
						TravelTime travelTime = getter.getModal(TravelTime.class);
						TravelDisutility travelDisutility = getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(travelTime);
						CapacityReconfigurationLogic capacityReconfigurationLogic = getter.getModal(CapacityReconfigurationLogic.class);
						return new CapacityChangeSchedulerEngine(fleet, network,
							travelDisutility, travelTime, capacityReconfigurationLogic);
					}));
				}
			});
		}
	}

	private static void preparePopulationLoads(Population population) {
		for(Map.Entry<Id<Person>, ? extends Person> personEntry: population.getPersons().entrySet()) {
			Id<Person> personId = personEntry.getKey();
			Person person = personEntry.getValue();
			int personParity=0;
			try {
				personParity = Integer.parseInt(personId.toString());
			} catch (NumberFormatException ignored) {

			}
			if(personParity % 2 == 0) {
				person.getAttributes().putAttribute(DefaultDvrpLoadFromDrtPassengers.ATTRIBUTE_LOAD_TYPE, PersonsDvrpLoadType.TYPE_NAME);
			} else {
				person.getAttributes().putAttribute(DefaultDvrpLoadFromDrtPassengers.ATTRIBUTE_LOAD_TYPE, GoodsDvrpLoadType.TYPE_NAME);
			}
		}
	}

	@Test
	void testRunDrtWithHeterogeneousVehicleCapacitiesWithoutRejections() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		Controler controller = DrtControlerCreator.createControler(config, false);
		preparePopulationLoads(controller.getScenario().getPopulation());
		controller.addOverridingModule(new CustomLoadsModule(true));
		controller.run();
	}


	@Test
	void testRunDrtWithHeterogeneousVehicleCapacitiesWithoutRejectionsWithDefaultReconfiguration() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		Controler controller = DrtControlerCreator.createControler(config, false);
		preparePopulationLoads(controller.getScenario().getPopulation());
		controller.addOverridingModule(new CustomLoadsModule(false));
		controller.run();
	}

	@Test
	void testRunDrtWithHeterogeneousVehicleCapacitiesWithoutRejectionsWithDefaultReconfigurationExcludingBeforeDayStarts() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		for (var drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			//disable rejections
			drtCfg.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		}

		Controler controller = DrtControlerCreator.createControler(config, false);
		preparePopulationLoads(controller.getScenario().getPopulation());
		controller.addOverridingModule(new CustomLoadsModule(false));

		controller.run();
	}


	@Test
	void testRunDrtWithHeterogeneousVehicleCapacitiesWithRejections() {
		Id.resetCaches();

		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Controler controller = DrtControlerCreator.createControler(config, false);
		preparePopulationLoads(controller.getScenario().getPopulation());
		controller.addOverridingModule(new CustomLoadsModule(true));

		controller.run();
	}
}
