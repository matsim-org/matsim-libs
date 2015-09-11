package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
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
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;

import junit.framework.Assert;

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

			public MyAgent(Plan selectedPlan) {
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

			void onTenMinutesAfterDeparting() {
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
		}

	}


	@Test
	public void testAgentNotification() {
		Scenario scenario = createSimpleScenario();
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin());
		plugins.add(new ActivityEnginePlugin());
		plugins.add(new TeleportationPlugin());
		plugins.add(new AbstractQSimPlugin() {
			@Override
			public Collection<? extends AbstractModule> modules() {
				Collection<AbstractModule> result = new ArrayList<>();
				result.add(new AbstractModule() {
					@Override
					public void install() {
						bind(PopulationAgentSource.class).asEagerSingleton();
						bind(AgentFactory.class).to(MyAgentFactory.class).asEagerSingleton();
					}
				});
				return result;
			}

			@Override
			public Collection<Class<? extends AgentSource>> agentSources() {
				Collection<Class<? extends AgentSource>> result = new ArrayList<>();
				result.add(PopulationAgentSource.class);
				return result;
			}
		});
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsCollector handler = new EventsCollector();
		eventsManager.addHandler(handler);
		QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);
		qSim.run();
		for (Event event : handler.getEvents()) {
			if (event instanceof PersonDepartureEvent) {
				Assert.assertEquals(25200.0, event.getTime());
			}
			if (event instanceof MyAgentFactory.MyAgent.HomesicknessEvent) {
				// Agent becomes homesick 10 minutes after departure
				Assert.assertEquals(25800.0, event.getTime());
			}
			System.out.println(event);
		}
	}

	private Scenario createSimpleScenario() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

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
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		Activity act1 = pb.createActivityFromLinkId("h", link.getId());
		act1.setEndTime(7.0*3600);
		Leg leg = pb.createLeg(TransportMode.walk);
		Route route = new GenericRouteImpl(link.getId(), link.getId());
		route.setTravelTime(5.0*3600);
		route.setDistance(100.0);
		leg.setRoute(route);
		Activity act2 = pb.createActivityFromLinkId("w", link.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);
		person.addPlan(plan);
		population.addPerson(person);
		return scenario;
	}

}
