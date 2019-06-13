package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Provides;

public class VehicleHandlerTest {
	@Test
	public void testVehicleHandler() {
		// This is a test where there is a link with a certain parking capacity. As soon as
		// it is reached the link is blocking, until a vehicle is leaving the link again. In 
		// this case there are three agents which do a longer stop on the capacitated link,
		// but they all have the same route and plan. This means that if the capacity is 
		// above 3, all agents will perform their plans without any distraction. If the
		// capacity is set to 2, the third agent needs to wait until the first of the 
		// previous ones is leaving, and so on ...
		
		Assert.assertEquals(20203.0, runTestScenario(4), 1e-3);
		Assert.assertEquals(20203.0, runTestScenario(3), 1e-3);
		Assert.assertEquals(23003.0, runTestScenario(2), 1e-3);
		Assert.assertEquals(33003.0, runTestScenario(1), 1e-3);
	}

	public double runTestScenario(long capacity) {
		Scenario scenario = createScenario();
		Controler controler = new Controler(scenario);

		LatestArrivalHandler arrivalHandler = new LatestArrivalHandler();
		BlockingVehicleHandler vehicleHandler = new BlockingVehicleHandler(capacity);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(arrivalHandler);
			}
		});

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
			}

			@Provides
			QNetworkFactory provideQNetworkFactory(EventsManager eventsManager, Scenario scenario) {
				ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(eventsManager, scenario);
				factory.setVehicleHandler(vehicleHandler);
				return factory;
			}
		});

		controler.run();
		return arrivalHandler.latestArrivalTime;
	}

	private class BlockingVehicleHandler implements VehicleHandler {
		private final Id<Link> linkId = Id.createLinkId("CD");
		private final long capacity;

		long count = 0;

		public BlockingVehicleHandler(long capacity) {
			this.capacity = capacity;
		}

		@Override
		public void handleVehicleDeparture(QVehicle vehicle, Link link) {
			if (link.getId().equals(linkId)) {
				count--;
			}
		}

		@Override
		public boolean handleVehicleArrival(QVehicle vehicle, Link link) {
			if (link.getId().equals(linkId)) {
				if (count >= capacity) {
					return false;
				}

				count++;
			}

			return true;
		}

		@Override
		public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {

		}
	}

	private class LatestArrivalHandler implements PersonArrivalEventHandler {
		double latestArrivalTime = Time.getUndefinedTime();

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLinkId().equals(Id.createLinkId("DE"))) {
				latestArrivalTime = event.getTime();
			}
		}
	}

	private Scenario createScenario() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setTypicalDuration(1.0);

		config.planCalcScore().addActivityParams(genericParams);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node nodeA = networkFactory.createNode(Id.createNodeId("A"), new Coord(0.0, 0.0));
		Node nodeB = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000.0, 2000.0));
		Node nodeC = networkFactory.createNode(Id.createNodeId("C"), new Coord(1000.0, 3000.0));
		Node nodeD = networkFactory.createNode(Id.createNodeId("D"), new Coord(1000.0, 4000.0));
		Node nodeE = networkFactory.createNode(Id.createNodeId("E"), new Coord(1000.0, 5000.0));

		Link linkAB = networkFactory.createLink(Id.createLinkId("AB"), nodeA, nodeB);
		Link linkBC = networkFactory.createLink(Id.createLinkId("BC"), nodeB, nodeC);
		Link linkCD = networkFactory.createLink(Id.createLinkId("CD"), nodeC, nodeD);
		Link linkDE = networkFactory.createLink(Id.createLinkId("DE"), nodeD, nodeE);

		Arrays.asList(nodeA, nodeB, nodeC, nodeD, nodeE).forEach(network::addNode);
		Arrays.asList(linkAB, linkBC, linkCD, linkDE).forEach(network::addLink);

		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Person person1 = populationFactory.createPerson(Id.createPersonId("person1"));
		Person person2 = populationFactory.createPerson(Id.createPersonId("person2"));
		Person person3 = populationFactory.createPerson(Id.createPersonId("person3"));

		for (Person person : Arrays.asList(person1, person2, person3)) {
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			Activity activity;
			Leg leg;

			activity = populationFactory.createActivityFromLinkId("generic", linkAB.getId());
			activity.setEndTime(0.0);
			plan.addActivity(activity);

			leg = populationFactory.createLeg("car");
			plan.addLeg(leg);

			activity = populationFactory.createActivityFromLinkId("generic", linkCD.getId());
			activity.setMaximumDuration(10000.0);
			plan.addActivity(activity);

			leg = populationFactory.createLeg("car");
			plan.addLeg(leg);

			activity = populationFactory.createActivityFromLinkId("generic", linkDE.getId());
			plan.addActivity(activity);
		}

		return scenario;
	}
}
