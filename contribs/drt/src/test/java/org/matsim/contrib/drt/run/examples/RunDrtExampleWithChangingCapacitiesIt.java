package org.matsim.contrib.drt.run.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.DvrpLoadFromPassengers;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class RunDrtExampleWithChangingCapacitiesIt {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	static class PersonsDvrpLoadType extends IntegerLoadType {
		public PersonsDvrpLoadType() {
			super("personsLoad", "persons");
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
		public GoodsDvrpLoadType() {
			super("goodsLoad", "goods");
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

	private static final PersonsDvrpLoadType PERSONS_LOAD_FACTORY = new PersonsDvrpLoadType();
	private static final GoodsDvrpLoadType GOODS_LOAD_FACTORY = new GoodsDvrpLoadType();

	static class CapacityChangeSchedulerEngine implements MobsimEngine {
		private final Fleet fleet;
		private final Link link;
		private final LeastCostPathCalculator router;
		private final TravelTime travelTime;
		private static final DrtTaskType taskType = new DrtTaskType("TO_CAPACITY_CHANGE", DrtTaskBaseType.DRIVE);

		public CapacityChangeSchedulerEngine(Fleet fleet, Network network, Link link, TravelDisutility travelDisutility, TravelTime travelTime) {
			this.fleet = fleet;
			this.link = link;
			this.travelTime = travelTime;
			this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
		}

		@Override
		public void doSimStep(double time) {

		}

		@Override
		public void onPrepareSim() {
			this.fleet.getVehicles().values().stream().forEach(dvrpVehicle -> {
				Schedule schedule = dvrpVehicle.getSchedule();
				DvrpLoad newVehicleLoad;

				// To be more generic in case other pats of the code change, we just switch the capacity type
				if(dvrpVehicle.getCapacity() instanceof PersonsDvrpLoad personsDvrpVehicleLoad) {
					newVehicleLoad = GOODS_LOAD_FACTORY.fromInt(personsDvrpVehicleLoad.getLoad());
				} else if(dvrpVehicle.getCapacity() instanceof GoodsDvrpLoad goodsDvrpVehicleLoad) {
					newVehicleLoad = PERSONS_LOAD_FACTORY.fromInt(goodsDvrpVehicleLoad.getLoad());
				} else {
					throw new IllegalStateException();
				}
				assert schedule.getTasks().size() == 1;
				assert schedule.getStatus().equals(Schedule.ScheduleStatus.PLANNED);
				assert schedule.getTasks().get(0) instanceof DrtStayTask;

				// We leave the initial stay task at the middle of the initially allowed slot
				DrtStayTask drtStayTask = (DrtStayTask) schedule.getTasks().get(0);
				if(drtStayTask.getEndTime() - drtStayTask.getBeginTime() < 3600 * 10) {
					return;
				}
				double departureTime = drtStayTask.getBeginTime() + 3600; //(drtStayTask.getEndTime() - drtStayTask.getBeginTime()) / 4;
				double initialEndTime = drtStayTask.getEndTime();
				drtStayTask.setEndTime(departureTime);
				VrpPathWithTravelData vrpPathWithTravelData = VrpPaths.calcAndCreatePath(drtStayTask.getLink(), link, departureTime, router, travelTime);
				DrtDriveTask drtDriveTask = new DrtDriveTask(vrpPathWithTravelData, taskType);

				double capacityChangeBeginTime = 12 * 3600;

				Task stayBeforeCapacityChangeTask = new DrtStayTask(vrpPathWithTravelData.getArrivalTime(), capacityChangeBeginTime, link);

				//Then we insert a capacity change with a duration of one minute
				Task capacityChangeTask = new DefaultDrtStopTaskWithVehicleCapacityChange(capacityChangeBeginTime,
					capacityChangeBeginTime, link, newVehicleLoad);

				//Then we insert a stay task there
				DrtStayTask stayAfterCapacityChangeTask = new DrtStayTask(capacityChangeTask.getEndTime(), Math.max(initialEndTime, capacityChangeTask.getEndTime()+60), link);
				schedule.addTask(drtDriveTask);
				schedule.addTask(stayBeforeCapacityChangeTask);
				schedule.addTask(capacityChangeTask);
				schedule.addTask(stayAfterCapacityChangeTask);
			});
		}

		@Override
		public void afterSim() {

		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {

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

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				// All vehicles start with a compatibility with persons
				bindModal(DvrpLoadFromFleet.class).toInstance((capacity, vehicleId) -> PERSONS_LOAD_FACTORY.fromInt(capacity));
			}
		});

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				// This implementation converts the first passenger's id to a number and decides whether it's a good or a person depending on the parity
				bindModal(DvrpLoadFromPassengers.class).toInstance(personIds -> {
					Id<Person> personId = personIds.stream().findFirst().get();
					int personParity=0;
					try {
						personParity = Integer.parseInt(personId.toString());
					} catch (NumberFormatException e) {

					}
					if(personParity%2 == 0) {
						return PERSONS_LOAD_FACTORY.fromInt(personIds.size());
					} else {
						return GOODS_LOAD_FACTORY.fromInt(personIds.size());
					}
				});

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
					return new CapacityChangeSchedulerEngine(fleet, network, network.getLinks().values().stream().findFirst().get(),
						travelDisutility, travelTime);
				}));
			}
		});

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

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				// All vehicles start with a compatibility with persons
				bindModal(DvrpLoadFromFleet.class).toInstance((capacity, vehicleId) -> PERSONS_LOAD_FACTORY.fromInt(capacity));
			}
		});

		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule("drt") {
			@Override
			protected void configureQSim() {
				// This implementation converts the first passenger's id to a number and decides whether it's a good or a person depending on the parity
				bindModal(DvrpLoadFromPassengers.class).toInstance(personIds -> {
					Id<Person> personId = personIds.stream().findFirst().get();
					int personParity=0;
					try {
						personParity = Integer.parseInt(personId.toString());
					} catch (NumberFormatException e) {

					}
					if(personParity%2 == 0) {
						return PERSONS_LOAD_FACTORY.fromInt(personIds.size());
					} else {
						return GOODS_LOAD_FACTORY.fromInt(personIds.size());
					}
				});

				bindModal(Fleet.class).toProvider(modalProvider(
					getter -> {
						Network network = getter.getModal(Network.class);
						return Fleets.createCustomFleet(getter.getModal(FleetSpecification.class),
							dvrpVehicleSpecification -> new DvrpVehicleWithChangeableCapacityImpl(dvrpVehicleSpecification,
								network.getLinks().get(dvrpVehicleSpecification.getStartLinkId())));
					})).asEagerSingleton();

				// This engine will plan the vehicles to change their capacities during the simulation
				addModalComponent(CapacityChangeSchedulerEngine.class, modalProvider(getter -> {
					Fleet fleet = getter.getModal(Fleet.class);
					Network network = getter.getModal(Network.class);
					TravelTime travelTime = getter.getModal(TravelTime.class);
					TravelDisutility travelDisutility = getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(travelTime);
					return new CapacityChangeSchedulerEngine(fleet, network, network.getLinks().values().stream().findFirst().get(),
						travelDisutility, travelTime);
				}));
			}
		});

		controller.run();
	}
}
