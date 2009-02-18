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
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;
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
	private CarRoute route1 = null;
	private CarRoute route2 = null;
	private CarRoute agentRoute = null;
	private Plan plan = null;
	private Leg leg = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.network = this.loadNetwork(networkFile);
		this.createRoutes();
	}

	@Override
	protected void tearDown() throws Exception {
		this.agentRoute = null;
		this.leg = null;
		this.network = null;
		this.plan = null;
		this.route1 = null;
		this.route2 = null;
		super.tearDown();
	}

	private NetworkLayer loadNetwork(final String filename) {
		NetworkLayer network = new NetworkLayer();
		Gbl.createConfig(null);
		MatsimNetworkReader parser = new MatsimNetworkReader(network);
		parser.readFile(filename);
		return network;
	}

	private void createRoutes() {
		Link startLink = this.network.getLink(new IdImpl("2"));
		Link endLink = this.network.getLink(new IdImpl("7"));
		this.route1 = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car, startLink, endLink);
		this.route2 = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car, startLink, endLink);
		this.agentRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car, startLink, endLink);
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

	private WithindayAgent createAgent(final Link homeLink, final Link workLink) {
		Person p = new PersonImpl(new IdImpl("1"));
		this.plan = new Plan(p);
		p.addPlan(this.plan);
		this.leg = null;
		try {
			this.plan.createAct("h", homeLink);
			this.leg = this.plan.createLeg(Mode.car);
			this.plan.createAct("work", workLink);
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
		QueueVehicle v = new QueueVehicle();
		
		//create the agent
		int sightDistance = 1;
		
		WithindayAgent pa = new WithindayAgent(p, sightDistance, factory);
		pa.setVehicle(v);
		v.setDriver(pa);
		pa.initialize();
		pa.setCurrentLink(this.network.getLink("2"));
		
	  return pa;
	}

	/**
	 * Test method for
	 * {@link org.matsim.withinday.WithindayAgent#replan()}.
	 */
	public void testReplan() {
		Events events = new Events();
		new QueueSimulation(this.network, null, events); // needed to initialize static QueueSimulation.events...
		
		WithindayAgent agent = createAgent(this.network.getLink(new IdImpl("2")), this.network.getLink(new IdImpl("7")));
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		Leg newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		List<Node> newPlansRoute = ((CarRoute) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		List<Node> newLegsRoute = ((CarRoute) agent.getVehicle().getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(1));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));		
		//enlarge scenario
		Link link1 = this.network.getLink("1");
		Link link7 = this.network.getLink("7");
		List<Node> list = new ArrayList<Node>();
		list.add(this.network.getNode("2"));
		list.addAll(this.agentRoute.getNodes());
		this.agentRoute.setNodes(link1, list, link7);
		agent = createAgent(link1, link7);
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		newPlansRoute = ((CarRoute) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		newLegsRoute = ((CarRoute) agent.getVehicle().getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("4"), newLegsRoute.get(newLegsRoute.size()-1));

		//again enlarge scenario
		Link link8 = this.network.getLink("8");
		list = new ArrayList<Node>();
		list.addAll(this.agentRoute.getNodes());
		list.add(this.network.getNode("5"));
		this.agentRoute.setNodes(link1, list, link8);
		agent = createAgent(link1, link8);
		agent.replan();
		assertNotSame("The selected plan should be exchanged by a new one", this.plan, agent.getPerson().getSelectedPlan());
		//going into the details
	  // first testing the plan of the person
		newPlansLeg = (Leg) agent.getPerson().getSelectedPlan().getActsLegs().get(1);
		newPlansRoute = ((CarRoute) newPlansLeg.getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newPlansRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newPlansRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newPlansRoute.get(newPlansRoute.size()-1));
		//second testing the vehicle
		assertNotSame("The current leg should be exchanged by a new one", this.leg, agent.getVehicle().getCurrentLeg());
		newLegsRoute = ((CarRoute) agent.getVehicle().getCurrentLeg().getRoute()).getNodes();
		assertEquals("the agent's new route should have the same size as the old one", this.agentRoute.getNodes().size(), newLegsRoute.size());
		assertEquals("agent should be rerouted via node 31", this.network.getNode("31"), newLegsRoute.get(2));
		assertEquals("check the last node of rerouting!", this.network.getNode("5"), newLegsRoute.get(newLegsRoute.size()-1));
		Logger.getLogger(WithindayAgentTest.class).info("WithindayAgentTest ran successfully.");
	}

}
