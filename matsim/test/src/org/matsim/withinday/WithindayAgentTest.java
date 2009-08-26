/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayAgentTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueVehicleImpl;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.withinday.coopers.CoopersAgentLogicFactory;
import org.matsim.withinday.trafficmanagement.EmptyControlInputImpl;
import org.matsim.withinday.trafficmanagement.VDSSign;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.ConstantControler;

/**
 * @author dgrether
 */
public class WithindayAgentTest extends MatsimTestCase {

	private static final String networkFile = "./test/input/org/matsim/withinday/network.xml";

	private NetworkLayer network = null;
	private NetworkRouteWRefs route1 = null;
	private NetworkRouteWRefs route2 = null;
	private NetworkRouteWRefs agentRoute = null;
	private PlanImpl plan = null;
	private LegImpl leg = null;
	private QueueSimulation simulation = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = new NetworkLayer();
		Gbl.createConfig(null);
		MatsimNetworkReader parser = new MatsimNetworkReader(this.network);
		parser.readFile(networkFile);
		this.createRoutes();
		this.simulation = new QueueSimulation(this.network, null, new EventsImpl());
	}

	@Override
	protected void tearDown() throws Exception {
		this.agentRoute = null;
		this.leg = null;
		this.network = null;
		this.plan = null;
		this.route1 = null;
		this.route2 = null;
		this.simulation = null;
		super.tearDown();
	}

	private void createRoutes() {
		LinkImpl startLink = this.network.getLink(new IdImpl("2"));
		LinkImpl endLink = this.network.getLink(new IdImpl("7"));
		this.route1 = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, startLink, endLink);
		this.route2 = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, startLink, endLink);
		this.agentRoute = (NetworkRouteWRefs) this.network.getFactory().createRoute(TransportMode.car, startLink, endLink);
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("31"));
		list.add(this.network.getNode("4"));
		this.route1.setNodes(startLink, list, endLink);
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.route2.setNodes(startLink, list, endLink);
		list = new ArrayList<Node>();
		list.add(this.network.getNode("3"));
		list.add(this.network.getNode("32"));
		list.add(this.network.getNode("4"));
		this.agentRoute.setNodes(startLink, list, endLink);
	}

	private VDSSign createSign() {
		VDSSign sign = new VDSSign();
		sign.setSignLink(this.network.getLink("2"));
		sign.setDirectionLink(this.network.getLink("7"));
		sign.setBenefitControl(false);
		sign.setMessageHoldTime(1);
		sign.setControlEvents(1);
		sign.setDeadZoneSystemInput(0.0);
		sign.setDeadZoneSystemOutput(0.0);
		sign.setNominalSplitting(0.5);
		sign.setControler(new ConstantControler(1.0));
		sign.setCompliance(1.0);
		// create control input
		EmptyControlInputImpl controlInput = new EmptyControlInputImpl();
		controlInput.setNashTime(0);
		controlInput.setMainRoute(this.route1);
		controlInput.setAlternativeRoute(this.route2);
		// set control input
		sign.setControlInput(controlInput);
		sign.setupIteration();
		sign.simulationPrepared();
		sign.calculateOutput(SimulationTimer.getTime());
		return sign;
	}

	private WithindayAgent createAgent(final LinkImpl homeLink, final LinkImpl workLink) {
		PersonImpl p = new PersonImpl(new IdImpl("1"));
		this.plan = new PlanImpl(p);
		p.addPlan(this.plan);
		this.leg = null;
		try {
			this.plan.createActivity("h", homeLink);
			this.leg = this.plan.createLeg(TransportMode.car);
			this.plan.createActivity("work", workLink);
		} catch (Exception e) {
			e.printStackTrace();
		}
		p.setSelectedPlan(this.plan);
		this.leg.setRoute(this.agentRoute);
	  //create the agentlogicfactory with
		//...the vdssigns
		List<VDSSign> signs = new LinkedList<VDSSign>();
		signs.add(createSign());
		//... the scoring function
		CharyparNagelScoringConfigGroup scoringFunctionConfig = Gbl.getConfig().charyparNagelScoring();
		scoringFunctionConfig.addParam("activityType_0", "h");
		scoringFunctionConfig.addParam("activityPriority_0", "1");
		scoringFunctionConfig.addParam("activityTypicalDuration_0", "01:00");
		scoringFunctionConfig.addParam("activityType_1", "work");
		scoringFunctionConfig.addParam("activityPriority_1", "1");
		scoringFunctionConfig.addParam("activityTypicalDuration_1", "01:00");
		CoopersAgentLogicFactory factory = new CoopersAgentLogicFactory(
				this.network, scoringFunctionConfig, signs);
		//create the vehicle
		BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("testVehType"));
		QueueVehicleImpl v = new QueueVehicleImpl(new BasicVehicleImpl(p.getId(), vehicleType));

		//create the agent
		int sightDistance = 1;

		WithindayAgent pa = new WithindayAgent(p, this.simulation, sightDistance, factory);
		pa.setVehicle(v);
		v.setDriver(pa);
		pa.initialize();
		pa.activityEnds(7.0*3600);
		LinkImpl link2 = this.network.getLink("2");
		v.setCurrentLink(link2);
		pa.teleportToLink(link2);

	  return pa;
	}

	/**
	 * Test method for
	 * {@link org.matsim.withinday.WithindayAgent#replan()}.
	 */
	public void testReplan() {
		LinkImpl link1 = this.network.getLink("1");
		LinkImpl link2 = this.network.getLink("2");
		LinkImpl link7 = this.network.getLink("7");
		WithindayAgent agent = createAgent(link2, link7);
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		LegImpl newPlansLeg = (LegImpl) agent.getPerson().getSelectedPlan().getPlanElements().get(1);
		List<Node> newPlansRoute = ((NetworkRouteWRefs) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getCurrentLeg());
		List<Node> newLegsRoute = ((NetworkRouteWRefs) agent.getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));

		//enlarge scenario
		List<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("2"));
		list.addAll(this.agentRoute.getNodes());
		this.agentRoute.setNodes(link1, list, link7);
		agent = createAgent(link1, link7);
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (LegImpl) agent.getPerson().getSelectedPlan().getPlanElements().get(1);
		newPlansRoute = ((NetworkRouteWRefs) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getCurrentLeg());
		newLegsRoute = ((NetworkRouteWRefs) agent.getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));

		//again enlarge scenario
		LinkImpl link8 = this.network.getLink("8");
		list = new ArrayList<Node>();
		list.addAll(this.agentRoute.getNodes());
		list.add(this.network.getNode("5"));
		this.agentRoute.setNodes(link1, list, link8);
		agent = createAgent(link1, link8);
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (LegImpl) agent.getPerson().getSelectedPlan().getPlanElements().get(1);
		newPlansRoute = ((NetworkRouteWRefs) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getCurrentLeg());
		newLegsRoute = ((NetworkRouteWRefs) agent.getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newLegsRoute.get(newLegsRoute.size()-1));
		Logger.getLogger(WithindayAgentTest.class).info("WithindayAgentTest ran successfully.");
	}

}
