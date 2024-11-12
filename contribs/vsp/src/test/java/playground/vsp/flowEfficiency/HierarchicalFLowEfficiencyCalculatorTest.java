package playground.vsp.flowEfficiency;

import com.google.inject.Provides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * This class tests {@link HierarchicalFlowEfficiencyCalculator} with the application of {@link BunchingFlowEfficencyImpact} and {@code AVFlowEfficencyImpact}.
 *
 * On 3 different routes, agents travel in different mode sequences:
 *
 * 1) all car
 * 2) all drt
 * 3) car - drt - car - drt - ....
 *
 * It is tested that for corresponding configuration, drt vehicles consume less flow capacity and move faster through the network
 * TODO test more details
 */
public class HierarchicalFLowEfficiencyCalculatorTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();
	private FlowEfficiencyHandler handler;

	@Test
	void testThatDrtAVMoveFaster(){

		Assertions.assertTrue(handler.lastArrivalsPerLink.get(Id.createLinkId(258)) > handler.lastArrivalsPerLink.get(Id.createLinkId(259)));
		Assertions.assertTrue(handler.lastArrivalsPerLink.get(Id.createLinkId(258)) > handler.lastArrivalsPerLink.get(Id.createLinkId(260)));
		Assertions.assertTrue(handler.lastArrivalsPerLink.get(Id.createLinkId(260)) > handler.lastArrivalsPerLink.get(Id.createLinkId(259)));

		Assertions.assertEquals(7238, handler.lastArrivalsPerLink.get(Id.createLinkId(258)), MatsimTestUtils.EPSILON); //car drivers
		Assertions.assertEquals(1845, handler.lastArrivalsPerLink.get(Id.createLinkId(259)), MatsimTestUtils.EPSILON); //drt drivers
		Assertions.assertEquals(5440, handler.lastArrivalsPerLink.get(Id.createLinkId(260)), MatsimTestUtils.EPSILON); //mixed (car - drt -car - drt - ....)

		Assertions.assertEquals(1800, handler.nrOfArrivalsPerLink.get(Id.createLinkId(258)), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1800, handler.nrOfArrivalsPerLink.get(Id.createLinkId(259)), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1800, handler.nrOfArrivalsPerLink.get(Id.createLinkId(260)), MatsimTestUtils.EPSILON);

	}

	@BeforeEach
	public void simulate(){
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"),
				"eight_shared_taxi_config.xml");

		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
		ConfigGroup zoneParams = dvrpConfigGroup.getTravelTimeMatrixParams().createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		dvrpConfigGroup.getTravelTimeMatrixParams().addParameterSet(zoneParams);

		Config config = ConfigUtils.loadConfig(configUrl, dvrpConfigGroup, new MultiModeDrtConfigGroup(), new OTFVisConfigGroup());
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.qsim().setEndTime(4*3600);

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DrtConfigGroup drtCfg = multiModeDrtConfigGroup.getModalElements().iterator().next();
		drtCfg.stopDuration = 1;

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		adjustNetwork(scenario);

		scenario.getPopulation().getPersons().clear();
		addPersons(scenario, 1800, Id.createLinkId(226), Id.createLinkId(258), TransportMode.car, 0 );
		addPersons(scenario, 1800, Id.createLinkId(227), Id.createLinkId(259), TransportMode.drt, 0 );
		addPersonsMixedMode(scenario, 1800, Id.createLinkId(228), Id.createLinkId(260), 0 );

		VehiclesFactory fac = scenario.getVehicles().getFactory();
		VehicleType dvrpVehType = fac.createVehicleType(Id.create(TransportMode.drt + "-fancyType", VehicleType.class));

		dvrpVehType.setLength(7.5);
		dvrpVehType.setFlowEfficiencyFactor(1d);
		dvrpVehType.setMaximumVelocity(999);
		scenario.getVehicles().addVehicleType(dvrpVehType);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfigGroup));

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
			}

			@Provides
			QNetworkFactory provideQNetworkFactory(EventsManager eventsManager, Scenario scenario) {
				ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(eventsManager, scenario);

				LinkedList<SituationalFlowEfficiencyImpact> situationalImpacts = new LinkedList<>();
				//the order of impacts matters!
				situationalImpacts.add(new AVFlowEfficiencyImpact(Set.of(dvrpVehType)));
				situationalImpacts.add(new BunchingFlowEfficencyImpact(Set.of(dvrpVehType), 2d, 1.1d));
				factory.setFlowEfficiencyCalculator(new HierarchicalFlowEfficiencyCalculator(situationalImpacts));
				return factory;
			}
		});

		controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
			@Override
			public void install() {
				FleetSpecificationImpl fleet = new FleetSpecificationImpl();
				for (int i = 0; i < 1800 * 1.5; i++) {
					fleet.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
							.capacity(1)
							.serviceBeginTime(0)
							.serviceEndTime(24*3600)
							.startLinkId(Id.createLinkId( (i % 3 == 0) ? 228 : 227))
							.id(Id.create("drt_" + i , DvrpVehicle.class))
							.build());
				}
				bindModal(FleetSpecification.class).toInstance(fleet);
				bindModal(VehicleType.class).toInstance(dvrpVehType);
			}
		});

		handler = new FlowEfficiencyHandler();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(handler);
			}
		});

		controler.run();
	}

	private void adjustNetwork(Scenario scenario) {
		scenario.getNetwork().getLinks().values().forEach(link -> link.setLength(300));
		HashMap<String, String> turns;

		{ //car mode route: 226 -> 237 -> 258
			scenario.getNetwork().getLinks().get(Id.createLinkId(226)).setCapacity(1800);
			turns = new HashMap<>();
			turns.put("237", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(226)).getAttributes().putAttribute("turns", turns);

			scenario.getNetwork().getLinks().get(Id.createLinkId(237)).setCapacity(900);
			turns = new HashMap<>();
			turns.put("258", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(237)).getAttributes().putAttribute("turns", turns);
		}

		{ //drt mode route: 227 -> 238 -> 259
			scenario.getNetwork().getLinks().get(Id.createLinkId(227)).setCapacity(1800);
			turns = new HashMap<>();
			turns.put("238", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(227)).getAttributes().putAttribute("turns", turns);

			scenario.getNetwork().getLinks().get(Id.createLinkId(238)).setCapacity(900);
			turns = new HashMap<>();
			turns.put("259", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(238)).getAttributes().putAttribute("turns", turns);

			turns = new HashMap<>();
			turns.put("459", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.UTURN));
			scenario.getNetwork().getLinks().get(Id.createLinkId(259)).getAttributes().putAttribute("turns", turns);

			turns = new HashMap<>();
			turns.put("438", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(459)).getAttributes().putAttribute("turns", turns);

			turns = new HashMap<>();
			turns.put("427", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(438)).getAttributes().putAttribute("turns", turns);

			turns = new HashMap<>();
			turns.put("227", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.UTURN));
			scenario.getNetwork().getLinks().get(Id.createLinkId(427)).getAttributes().putAttribute("turns", turns);
		}

		{ //mixed mode route: 228 -> 239 -> 260
			scenario.getNetwork().getLinks().get(Id.createLinkId(228)).setCapacity(1800);
			turns = new HashMap<>();
			turns.put("239", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(228)).getAttributes().putAttribute("turns", turns);

			scenario.getNetwork().getLinks().get(Id.createLinkId(239)).setCapacity(900);
			turns = new HashMap<>();
			turns.put("260", String.valueOf(LinkTurnDirectionAttributesFromGraphHopper.TurnDirection.STRAIGHT));
			scenario.getNetwork().getLinks().get(Id.createLinkId(239)).getAttributes().putAttribute("turns", turns);
		}

	}

	private void addPersons(Scenario scenario, int howMany, Id<Link> from, Id<Link> to, String legMode, double startTime){

		PopulationFactory factory = scenario.getPopulation().getFactory();

		for (int i = 0; i < howMany; i++) {
			Person person = factory.createPerson(Id.createPersonId("straight_" + legMode + "_" + i));

			Plan plan = factory.createPlan();

			Activity origin = factory.createActivityFromLinkId("dummy", from);
			origin.setEndTime(startTime);
			plan.addActivity(origin);
			plan.addLeg(factory.createLeg(legMode));

			Activity destination = factory.createActivityFromLinkId("dummy", to);
			plan.addActivity(destination);

			person.addPlan(plan);
			person.setSelectedPlan(plan);

			scenario.getPopulation().addPerson(person);
		}
	}

	private void addPersonsMixedMode(Scenario scenario, int howMany, Id<Link> from, Id<Link> to, double startTime){

		PopulationFactory factory = scenario.getPopulation().getFactory();

		for (int i = 0; i < howMany; i++) {
			String legMode = (i % 2 == 0) ? TransportMode.car : TransportMode.drt;
			Person person = factory.createPerson(Id.createPersonId("mixed_" + legMode + "_" + i));

			Plan plan = factory.createPlan();

			Activity origin = factory.createActivityFromLinkId("dummy", from);
			origin.setEndTime(startTime + i);
			plan.addActivity(origin);
			plan.addLeg(factory.createLeg(legMode));

			Activity destination = factory.createActivityFromLinkId("dummy", to);
			plan.addActivity(destination);

			person.addPlan(plan);
			person.setSelectedPlan(plan);

			scenario.getPopulation().addPerson(person);
		}
	}

	private class FlowEfficiencyHandler implements PersonArrivalEventHandler {

		Map<Id<Link>, Integer> nrOfArrivalsPerLink = new HashMap<>();
		Map<Id<Link>, Double> lastArrivalsPerLink = new HashMap<>();

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if ( (event.getLegMode().equals(TransportMode.car) || event.getLegMode().equals(TransportMode.drt) ) &&
					(event.getPersonId().toString().contains("straight_") || event.getPersonId().toString().contains("mixed_"))) {
				this.nrOfArrivalsPerLink.compute(event.getLinkId(), (k,v) -> (v==null) ? 1 : v+1);
				this.lastArrivalsPerLink.put(event.getLinkId(), event.getTime());
			}
		}

		@Override
		public void reset(int iteration) {
			this.lastArrivalsPerLink.clear();
			this.nrOfArrivalsPerLink.clear();
		}

	}

}
