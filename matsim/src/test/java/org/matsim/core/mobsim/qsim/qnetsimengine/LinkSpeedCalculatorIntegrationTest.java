/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
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
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.Builder;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.EventsLogger;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

/**
 * @author mrieser / Senozon AG
 */
public class LinkSpeedCalculatorIntegrationTest {

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();
	
	@Test
	public void testIntegration_Default() {
		Fixture f = new Fixture();
		EventsCollector collector = new EventsCollector();
		f.events.addHandler(collector);
		f.events.addHandler(new EventsLogger());

		QSim qsim = configureQSim(f, null);
		qsim.run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getVehicleId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getVehicleId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// by default, the link takes 10 seconds to travel along, plus 1 second in the buffer, makes total of 11 seconds
		Assert.assertEquals(11, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void testIntegration_Slow() {
		Fixture f = new Fixture();

		final Scenario scenario = f.scenario ;
		final Config config = scenario.getConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		
		Collection<AbstractModule> defaultsModules = new ArrayList<>() ;
		defaultsModules.add( new ScenarioByInstanceModule( scenario ) ) ;
		defaultsModules.add( new EventsManagerModule() ) ;
		defaultsModules.add( new DefaultMobsimModule() ) ;
		
		AbstractModule overrides = new AbstractModule() {
			final LinkSpeedCalculator linkSpeedCalculator = new CustomLinkSpeedCalculator(5.0) ;
			@Override public void install() {
				bind(QNetworkFactory.class).toInstance( new QNetworkFactory(){
					@Inject QSimConfigGroup qsimConfig ;
					@Inject EventsManager events ;
					@Inject Network network ;
					private AgentCounter agentCounter;
					private NetsimInternalInterface netsimEngine;
					private MobsimTimer mobsimTimer;
					private NetsimEngineContext context;
					@Override void initializeFactory(AgentCounter agentCounter1, MobsimTimer mobsimTimer1, NetsimInternalInterface netsimEngine1) {
						this.agentCounter = agentCounter1 ;
						this.mobsimTimer = mobsimTimer1 ;
						this.netsimEngine = netsimEngine1 ;

						double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;

						SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
						AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
						AbstractAgentSnapshotInfoBuilder positionInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

						this.context = new NetsimEngineContext( events, effectiveCellSize,
								agentCounter, positionInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator );
						
					}
					@Override QNode createNetsimNode(Node node) {
						QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
						return builder.build( node ) ;
					}
					@Override QLinkI createNetsimLink(Link link, QNode queueNode) {
						QueueWithBuffer.Builder laneBuilder = new QueueWithBuffer.Builder(context) ;
						laneBuilder.setLinkSpeedCalculator(linkSpeedCalculator);

						Builder builder = new QLinkImpl.Builder(context, netsimEngine) ;
						builder.setLaneFactory(laneBuilder);

						return builder.build(link, queueNode) ;
					}
				} ) ;
			}
		} ;
		
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), AbstractModule.override( defaultsModules, overrides ) ) ; 

		EventsManager eventsManager = injector.getInstance( EventsManager.class ) ;
		eventsManager.initProcessing(); 

		EventsCollector collector = new EventsCollector();
		eventsManager.addHandler(collector);
		eventsManager.addHandler(new EventsLogger());

		injector.getInstance( Mobsim.class ).run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getVehicleId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getVehicleId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// with 5 per second, the link takes 20 seconds to travel along, plus 1 second in the buffer, makes total of 21 seconds
		Assert.assertEquals(21, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void testIntegration_Fast() {
		Fixture f = new Fixture();

		final Scenario scenario = f.scenario ;
		final Config config = scenario.getConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		
		Collection<AbstractModule> defaultsModules = new ArrayList<>() ;
		defaultsModules.add( new ScenarioByInstanceModule( scenario ) ) ;
		defaultsModules.add( new EventsManagerModule() ) ;
		defaultsModules.add( new DefaultMobsimModule() ) ;
		
		AbstractModule overrides = new AbstractModule() {
			final LinkSpeedCalculator linkSpeedCalculator = new CustomLinkSpeedCalculator(20.0) ;
			@Override public void install() {
				bind(QNetworkFactory.class).toInstance( new QNetworkFactory(){
					@Inject private QSimConfigGroup qsimConfig ;
					@Inject private EventsManager events ;
					@Inject private Network network ;
					private AgentCounter agentCounter;
					private MobsimTimer mobsimTimer;
					private NetsimInternalInterface netsimEngine;
					private NetsimEngineContext context;
					@Override void initializeFactory(AgentCounter agentCounter1, MobsimTimer mobsimTimer1, NetsimInternalInterface netsimEngine1) {
						this.agentCounter = agentCounter1 ;
						this.mobsimTimer = mobsimTimer1 ;
						this.netsimEngine = netsimEngine1 ;

						double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;

						SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
						AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
						AbstractAgentSnapshotInfoBuilder positionInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

						this.context = new NetsimEngineContext( events, effectiveCellSize,
								agentCounter, positionInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator );
					}
					@Override QNode createNetsimNode(Node node) {
						QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
						return builder.build( node ) ;
					}
					@Override QLinkI createNetsimLink(Link link, QNode queueNode) {
						QueueWithBuffer.Builder laneBuilder = new QueueWithBuffer.Builder(context) ;
						laneBuilder.setLinkSpeedCalculator(linkSpeedCalculator);

						Builder builder = new QLinkImpl.Builder(context, netsimEngine) ;
						builder.setLaneFactory(laneBuilder);

						return builder.build(link, queueNode) ;
					}
				} ) ;
			}
		} ;
		
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), AbstractModule.override( defaultsModules, overrides ) ) ; 

		EventsManager eventsManager = injector.getInstance( EventsManager.class ) ;
		eventsManager.initProcessing(); 

		EventsCollector collector = new EventsCollector();
		eventsManager.addHandler(collector);
		eventsManager.addHandler(new EventsLogger());

		injector.getInstance( Mobsim.class ).run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getVehicleId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getVehicleId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// the link should take 5 seconds to travel along, plus 1 second in the buffer, makes total of 6 seconds
		Assert.assertEquals(6, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	private static QSim configureQSim(Fixture f, LinkSpeedCalculator linkSpeedCalculator) {
		QSim qsim = new QSim(f.scenario, f.events);
		
		// handle activities
		ActivityEngine activityEngine = new ActivityEngine(f.events, qsim.getAgentCounter());
		qsim.addMobsimEngine(activityEngine);
		qsim.addActivityHandler(activityEngine);

        QNetsimEngine netsimEngine = new QNetsimEngine(qsim);
		if (linkSpeedCalculator != null) {
			throw new RuntimeException( "does not work like this any more") ;
		}
		qsim.addMobsimEngine(netsimEngine);
		qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
		
		PopulationAgentSource agentSource = new PopulationAgentSource(f.scenario.getPopulation(), new DefaultAgentFactory(qsim), qsim);
		qsim.addAgentSource(agentSource);
		
		return qsim;
	}
	
	static class CustomLinkSpeedCalculator implements LinkSpeedCalculator {

		final double maxSpeed;
		
		public CustomLinkSpeedCalculator(final double maxSpeed) {
			this.maxSpeed = maxSpeed;
		}
		
		@Override
		public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
			return this.maxSpeed;
		}
		
	}
	
	/**
	 * Creates a simple network (3 links in a row) and a single person travelling from the first link to the third.
	 * 
	 * @author mrieser / Senozon AG
	 */
	static class Fixture {
		EventsManager events = new EventsManagerImpl();
		Scenario scenario;

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

			Id<Node>[] nodeIds = new Id[5];
			for (int i = 0; i < nodeIds.length; i++) {
				nodeIds[i] = Id.create(i, Node.class);
			}
			Id<Link>[] linkIds = new Id[4];
			for (int i = 0; i < linkIds.length; i++) {
				linkIds[i] = Id.create(i, Link.class);
			}
	
			/* create Network */
			Network network = this.scenario.getNetwork();
			NetworkFactory nf = network.getFactory();

			Node n1 = nf.createNode(nodeIds[1], new Coord((double) 0, (double) 0));
			Node n2 = nf.createNode(nodeIds[2], new Coord((double) 100, (double) 0));
			Node n3 = nf.createNode(nodeIds[3], new Coord((double) 200, (double) 0));
			Node n4 = nf.createNode(nodeIds[4], new Coord((double) 300, (double) 0));

			network.addNode(n1);
			network.addNode(n2);
			network.addNode(n3);
			network.addNode(n4);

			Link l1 = nf.createLink(linkIds[1], n1, n2);
			Link l2 = nf.createLink(linkIds[2], n2, n3);
			Link l3 = nf.createLink(linkIds[3], n3, n4);
			
			Set<String> modes = new HashSet<String>();
			modes.add("car");
			for (Link l : new Link[] {l1, l2, l3}) {
				l.setLength(100);
				l.setFreespeed(10.0);
				l.setAllowedModes(modes);
				l.setCapacity(1800);
				network.addLink(l);
			}

			/* create Person */
			Population population = this.scenario.getPopulation();
			PopulationFactory pf = population.getFactory();

			Person person = pf.createPerson(Id.create(1, Person.class));
			Plan plan = pf.createPlan();
			Activity homeAct = pf.createActivityFromLinkId("home", linkIds[1]);
			homeAct.setEndTime(7*3600);
			Leg leg = pf.createLeg("car");
			Route route = new LinkNetworkRouteImpl(linkIds[1], new Id[] { linkIds[2] }, linkIds[3]);
			leg.setRoute(route);
			Activity workAct = pf.createActivityFromLinkId("work", linkIds[1]);

			plan.addActivity(homeAct);
			plan.addLeg(leg);
			plan.addActivity(workAct);

			person.addPlan(plan);
			
			population.addPerson(person);
		}
	}

}
