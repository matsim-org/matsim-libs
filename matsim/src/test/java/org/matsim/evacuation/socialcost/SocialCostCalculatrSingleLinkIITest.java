/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculatrSingleLinkIITest.java
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
package org.matsim.evacuation.socialcost;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLinkTest.Vehicle;

import junit.framework.TestCase;

/**
 * @author laemmel
 * 
 */
public class SocialCostCalculatrSingleLinkIITest extends TestCase {

	public void testSocialCostCalculatorSingleLinkIIZeroCost() {
		Fixture f = new Fixture();
		Controler c = new Controler(f.sc);

		EventsManagerImpl events = new EventsManagerImpl();

		SocialCostCalculatorSingleLinkII scalc = new SocialCostCalculatorSingleLinkII(f.network, 60, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c, 1));

		events.addHandler(scalc);

		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : f.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEventImpl lee = new LinkEnterEventImpl(time, v.id, f.l0);
			events.processEvent(lee);

			while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= f.link0.getFreespeedTravelTime(time)) {
				Vehicle tmp = vehQueue.poll();
				LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
				events.processEvent(lle);

			}

			time++;
		}
		IterationStartsEvent iss = new IterationStartsEvent(c, 1);
		scalc.notifyIterationStarts(iss);

		double costs = 0.;
		for (; time >= 0; time--) {
			costs += scalc.getLinkTravelCost(f.link0, time);
		}

		assertEquals(0., costs);

	}

	public void testSocialCostCalculatorSingleIILinkCost() {
		Fixture f = new Fixture();
		Controler c = new Controler(f.sc);

		EventsManagerImpl events = new EventsManagerImpl();

		SocialCostCalculatorSingleLinkII scalc = new SocialCostCalculatorSingleLinkII(f.network, 5, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c, 1));

		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();

		events.addHandler(scalc);
		events.addHandler(apc);

		IterationStartsEvent iss = new IterationStartsEvent(c, 1);
		scalc.notifyIterationStarts(iss);

		BeforeMobsimEvent bme = new BeforeMobsimEvent(c, 1);
		scalc.notifyBeforeMobsim(bme);

		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : f.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEventImpl lee = new LinkEnterEventImpl(time, v.id, f.l0);
			events.processEvent(lee);

			if (time <= 18 || time > 27) {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= f.link0.getFreespeedTravelTime(time)) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			} else {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= (1 + f.link0.getFreespeedTravelTime(time))) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			}

			time += 1;
		}
		while (vehQueue.size() > 0) {
			if (time <= 18 || time > 27) {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= f.link0.getFreespeedTravelTime(time)) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			} else {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= (1 + f.link0.getFreespeedTravelTime(time))) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			}
			time += 1;
		}

		AfterMobsimEvent ame = new AfterMobsimEvent(c, 1);
		scalc.notifyAfterMobsim(ame);

		// congestion end time 640
		// soc cost = T*tbinsize - fstt;
		// 6*(60-10) + 6*(120-10) + ... + 6*(480-10) = 12480
		double costs = 0.;
		for (; time >= 0; time -= 1) {
			costs += scalc.getLinkTravelCost(f.link0, time);
		}
		assertEquals(25., costs);

		double penalty = (0 + 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8) / -600.;
		assertEquals(penalty, apc.penalty, 0.000000001);
	}

	// test to make sure that there are no social costs on empty links
	public void testSocialCostCalculatorSingleLinkIICostZeroCost() {
		Fixture f = new Fixture();
		Controler c = new Controler(f.sc);

		EventsManagerImpl events = new EventsManagerImpl();

		SocialCostCalculatorSingleLinkII scalc = new SocialCostCalculatorSingleLinkII(f.network, 5, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c, 1));

		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();

		events.addHandler(scalc);
		events.addHandler(apc);

		IterationStartsEvent iss = new IterationStartsEvent(c, 1);
		scalc.notifyIterationStarts(iss);

		BeforeMobsimEvent bme = new BeforeMobsimEvent(c, 1);
		scalc.notifyBeforeMobsim(bme);

		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : f.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEventImpl lee = new LinkEnterEventImpl(time, v.id, f.l0);
			events.processEvent(lee);

			if (time <= 18) {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= f.link0.getFreespeedTravelTime(time)) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			} else {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= (1 + f.link0.getFreespeedTravelTime(time))) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			}

			time += 1;
		}
		while (vehQueue.size() > 0) {
			if (time <= 18) {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= f.link0.getFreespeedTravelTime(time)) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			} else {
				while (vehQueue.size() > 0 && (time - vehQueue.peek().enterTime) >= (1 + f.link0.getFreespeedTravelTime(time))) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
					events.processEvent(lle);

				}
			}
			time += 1;
		}

		AfterMobsimEvent ame = new AfterMobsimEvent(c, 1);
		scalc.notifyAfterMobsim(ame);

		// congestion end time 640
		// soc cost = T*tbinsize - fstt;
		// 6*(60-10) + 6*(120-10) + ... + 6*(480-10) = 12480
		double costs = 0.;
		for (; time >= 0; time -= 1) {
			costs += scalc.getLinkTravelCost(f.link0, time);
		}
		assertEquals(5 * 7 + 5 * 12. + 5 * 2.5, costs);

		double penalty = (15 + 14 + 13 + 12 + 11 + 10 + 9 + 8 + 7 + 6 + 5 + 4 + 3 + 2 + 1) / -600.;
		assertEquals(penalty, apc.penalty, 0.000000001);
	}

	// test to make sure that there social costs until sim ends if agents do not
	// leave link
	public void testSocialCostCalculatorSingleLinkIICostStuckAgent() {
		Fixture f = new Fixture();
		Controler c = new Controler(f.sc);

		EventsManagerImpl events = new EventsManagerImpl();

		SocialCostCalculatorSingleLinkII scalc = new SocialCostCalculatorSingleLinkII(f.network, 180, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c, 1));

		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();

		events.addHandler(scalc);
		events.addHandler(apc);

		IterationStartsEvent iss = new IterationStartsEvent(c, 1);
		scalc.notifyIterationStarts(iss);

		BeforeMobsimEvent bme = new BeforeMobsimEvent(c, 1);
		scalc.notifyBeforeMobsim(bme);

		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : f.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEventImpl lee = new LinkEnterEventImpl(time, v.id, f.l0);
			events.processEvent(lee);
			time += 60;
		}

		time = 0;
		while (vehQueue.size() > 0) {
			time += 3456;
			Vehicle tmp = vehQueue.poll();
			LinkLeaveEventImpl lle = new LinkLeaveEventImpl(time, tmp.id, f.l0);
			events.processEvent(lle);
		}

		AfterMobsimEvent ame = new AfterMobsimEvent(c, 1);
		scalc.notifyAfterMobsim(ame);
		double costs = 0;
		for (time = 0; time <= 24 * 3600; time += 180) {
			costs += scalc.getLinkTravelCost(f.link0, time);
		}

		assertEquals(266112., costs);
		assertEquals(-1728., apc.penalty, 0.000000001);
	}

	/* package */static class AgentPenaltyCalculator implements AgentMoneyEventHandler {
		double penalty = 0.;

		@Override
		public void handleEvent(AgentMoneyEvent event) {
			this.penalty += event.getAmount();
		}

		@Override
		public void reset(int iteration) {
		}

	}

	/* package */static class Vehicle {
		Id id;
		double enterTime;
	}

	private static class Fixture {
		/* package */ArrayList<Vehicle> agents;
		/* package */ScenarioImpl sc;
		/* package */NetworkImpl network;
		/* package */Id l0;
		/* package */LinkImpl link0;

		public Fixture() {
			this.sc = new ScenarioImpl();
			this.network = this.sc.getNetwork();

			this.agents = new ArrayList<Vehicle>();
			for (int i = 0; i < 25; i++) {
				Vehicle v = new Vehicle();
				v.id = this.sc.createId(Integer.toString(i));
				this.agents.add(v);
			}

			this.l0 = this.sc.createId("0");
			Id n0 = this.sc.createId("0");
			Id n1 = this.sc.createId("1");

			Node node0 = (this.network).createAndAddNode(n0, this.sc.createCoord(0, 0));
			Node node1 = (this.network).createAndAddNode(n1, this.sc.createCoord(10, 0));

			this.link0 = (LinkImpl) (this.network).createAndAddLink(this.l0, node0, node1, 100, 10, 1, 1);
		}
	}
}
