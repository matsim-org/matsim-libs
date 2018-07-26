package org.matsim.core.mobsim.qsim;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.jdeqsim.Message;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;


public class AgentNotificationTest {

	private static class MyAgentFactory implements AgentFactory {

		@Inject
		Netsim simulation;

		@Inject
		MessageQueue messageQueue;

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			return new MyAgent(p.getSelectedPlan());
		}

		private class MyAgent implements MobsimDriverAgent, PlanAgent {

			PersonDriverAgentImpl delegate;

			MyAgent(Plan selectedPlan) {
				delegate = new PersonDriverAgentImpl(selectedPlan, simulation);
			}

			@Override
			public Id<Person> getId() {
				return delegate.getId();
			}

			@Override
			public Id<Link> getCurrentLinkId() {
				return delegate.getCurrentLinkId();
			}

			@Override
			public Id<Link> getDestinationLinkId() {
				return delegate.getDestinationLinkId();
			}

			@Override
			public String getMode() {
				return delegate.getMode();
			}

			@Override
			public Id<Link> chooseNextLinkId() {
				return delegate.chooseNextLinkId();
			}

			@Override
			public void notifyMoveOverNode(Id<Link> newLinkId) {
				delegate.notifyMoveOverNode(newLinkId);
			}

			@Override
			public boolean isWantingToArriveOnCurrentLink() {
				return delegate.isWantingToArriveOnCurrentLink();
			}

			@Override
			public void setVehicle(MobsimVehicle veh) {
				delegate.setVehicle(veh);
			}

			@Override
			public MobsimVehicle getVehicle() {
				return delegate.getVehicle();
			}

			@Override
			public Id<Vehicle> getPlannedVehicleId() {
				return delegate.getPlannedVehicleId();
			}

			@Override
			public PlanElement getCurrentPlanElement() {
				return delegate.getCurrentPlanElement();
			}

			@Override
			public PlanElement getNextPlanElement() {
				return delegate.getNextPlanElement();
			}

			@Override
			public Plan getCurrentPlan() {
				return delegate.getCurrentPlan();
			}

			@Override
			public State getState() {
				return delegate.getState();
			}

			@Override
			public double getActivityEndTime() {
				return delegate.getActivityEndTime();
			}

			@Override
			public void endActivityAndComputeNextState(double now) {
				delegate.endActivityAndComputeNextState(now);
				Message m = new Message() {
					@Override
					public void processEvent() {

					}

					@Override
					public void handleMessage() {
						onTenMinutesAfterDeparting();
					}
				};
				m.setMessageArrivalTime(now + 10.0 * 60.0);
				messageQueue.putMessage(m);
			}

			private void onTenMinutesAfterDeparting() {
				simulation.getEventsManager().processEvent(new HomesicknessEvent());
			}

			@Override
			public void endLegAndComputeNextState(double now) {
				delegate.endLegAndComputeNextState(now);
			}

			@Override
			public void setStateToAbort(double now) {
				delegate.setStateToAbort(now);
			}

			@Override
			public Double getExpectedTravelTime() {
				return delegate.getExpectedTravelTime();
			}

			@Override
			public Double getExpectedTravelDistance() {
				return delegate.getExpectedTravelDistance();
			}

			@Override
			public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
				delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
			}

			class HomesicknessEvent extends Event {
				public HomesicknessEvent() {
					super(simulation.getSimTimer().getTimeOfDay());
				}

				@Override
				public String getEventType() {
					return "homeSickness";
				}

				@Override
				public Map<String, String> getAttributes() {
					Map<String, String> attributes = super.getAttributes();
					attributes.put("person", delegate.getPerson().getId().toString());
					return attributes;
				}
			}

			@Override
			public Facility<? extends Facility<?>> getCurrentFacility() {
				return delegate.getCurrentFacility() ;
			}

			@Override
			public Facility<? extends Facility<?>> getDestinationFacility() {
				return delegate.getDestinationFacility() ;
			}

			@Override
			public PlanElement getPreviousPlanElement() {
				return delegate.getPreviousPlanElement() ;
			}
		}

	}


	@SuppressWarnings("static-method")
	@Test
	public void testAgentNotification() {
		Scenario scenario = createSimpleScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsCollector handler = new EventsCollector();
		eventsManager.addHandler(handler);
		
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.removePlugin(PopulationPlugin.class) //
			.addPlugin(new PopulationPlugin(scenario.getConfig()) {
				@Override
				public Collection<? extends AbstractModule> modules() {
					return Collections.singletonList(new AbstractModule() {
						@Override
						public void configure() {
							bind(PopulationAgentSource.class).asEagerSingleton();
							bind(AgentFactory.class).to(MyAgentFactory.class).asEagerSingleton();
						}
					});
				}
			}) //
			.configureComponents(components -> {
				components.activeMobsimEngines.remove("NetsimEngine");
				components.activeDepartureHandlers.remove("NetsimEngine");
			}) //
			.build(scenario, eventsManager) //
			.run();
		
		// yyyyyy I can comment out the above line and the test still passes (will say: "skipped"). ?????? kai, feb'16
		
		assumeThat(handler.getEvents(), hasItem(
				is(both(eventWithTime(25200.0)).and(instanceOf(PersonDepartureEvent.class)))));
		assertThat(handler.getEvents(), hasItem(
				is(both(eventWithTime(25800.0)).and(instanceOf(MyAgentFactory.MyAgent.HomesicknessEvent.class)))));
	}

	private static Matcher<Event> eventWithTime(double time) {
		return new FeatureMatcher<Event, Double>(is(time), "time", "time") {
			@Override
			protected Double featureValueOf(Event event) {
				return event.getTime();
			}
		};
	}

	private static Scenario createSimpleScenario() {
		final Config config = ConfigUtils.createConfig();

		ModeRoutingParams params = new ModeRoutingParams( TransportMode.walk ) ;
		params.setBeelineDistanceFactor(1.3);
		params.setTeleportedModeSpeed(1.);
		config.plansCalcRoute().addModeRoutingParams( params );

		Scenario scenario = ScenarioUtils.createScenario(config);

		// build simple network with 1 link
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000.0, 0.0));
		network.addNode(node1);
		network.addNode(node2);
		Link link = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
		link.setFreespeed(10.0);
		link.setCapacity(2000.0);
		network.addLink(link);

		// build simple population with 1 person with 1 plan with 1 leg
		Population population = scenario.getPopulation();
		PopulationFactory pb = population.getFactory();
		{
			Person person = pb.createPerson(Id.create("1", Person.class));
			Plan plan = pb.createPlan();
			{
				Activity act1 = pb.createActivityFromLinkId("h", link.getId());
				act1.setEndTime(7.0*3600);
				plan.addActivity(act1);
			}
			{
				Leg leg = pb.createLeg(TransportMode.walk);
				Route route = RouteUtils.createGenericRouteImpl(link.getId(), link.getId());
				route.setTravelTime(5.0*3600);
				route.setDistance(100.0);
				leg.setRoute(route);
				plan.addLeg(leg);
			}
			{
				Activity act2 = pb.createActivityFromLinkId("w", link.getId());
				plan.addActivity(act2);
			}
			person.addPlan(plan);
			population.addPerson(person);
		}
		return scenario;
	}

}
