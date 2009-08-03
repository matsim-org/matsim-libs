package org.matsim.evacuation.socialcost;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentMoneyEventHandler;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.testcases.MatsimTestCase;

public class SocialCostCalculatorSingleLinkTest extends MatsimTestCase {

	private ArrayList<Vehicle> agents;
	private final boolean setup = false;
	private ScenarioImpl sc;
	private NetworkLayer network;
	private Id l0;
	private LinkImpl link0;
	
	
	public void testSocialCostCalculatorSingleLinkZeroCost() {
	
		if (!this.setup) {
			setup();
		}
		
		Controler c = new Controler(this.sc);
		
		Events events = new Events();
		
		
		SocialCostCalculatorSingleLink scalc = new SocialCostCalculatorSingleLink((NetworkLayer) this.network,60, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c,1));
		
		events.addHandler(scalc);
		
		
		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : this.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEvent lee = new LinkEnterEvent(time,v.id,this.l0);
			events.processEvent(lee);
			
			while (vehQueue.size() > 0 && (time-vehQueue.peek().enterTime) >= this.link0.getFreespeedTravelTime(time) ) {
				Vehicle tmp = vehQueue.poll();
				LinkLeaveEvent lle = new LinkLeaveEvent(time,tmp.id,this.l0);
				events.processEvent(lle);
				
			}
			
			time++;
		}
		IterationStartsEvent iss = new IterationStartsEvent(c,1);
		scalc.notifyIterationStarts(iss);
		
		double costs = 0.;
		for (; time >= 0; time--) {
			costs += scalc.getSocialCost(this.link0, time);
		}
		
		assertEquals(0., costs);
		
		
	}
	
	public void testSocialCostCalculatorSingleLinkCost() {
		
		if (!this.setup) {
			setup();
		}
		
		Controler c = new Controler(this.sc);
		
		Events events = new Events();
		
		
		SocialCostCalculatorSingleLink scalc = new SocialCostCalculatorSingleLink((NetworkLayer) this.network,60, events);
		scalc.notifyIterationStarts(new IterationStartsEvent(c,1));
		
		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();
		
		events.addHandler(scalc);
		events.addHandler(apc);
		
		
		double time = 0;
		Queue<Vehicle> vehQueue = new ConcurrentLinkedQueue<Vehicle>();
		for (Vehicle v : this.agents) {
			v.enterTime = time;
			vehQueue.add(v);
			LinkEnterEvent lee = new LinkEnterEvent(time,v.id,this.l0);
			events.processEvent(lee);
			
			
			//first 9 vehicle travel with fs tt
			if (time <= 90 || time > 630) {
				while (vehQueue.size() > 0 && (time-vehQueue.peek().enterTime) >= this.link0.getFreespeedTravelTime(time) ) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEvent lle = new LinkLeaveEvent(time,tmp.id,this.l0);
					events.processEvent(lle);
					
				}
				
			//55 vehicles congested
			// 8 tt bin congestion 48 vehicles // 6 per time bin 
			} else if (time <= 630) {
				while (vehQueue.size() > 0 && (time-vehQueue.peek().enterTime) >= (20+this.link0.getFreespeedTravelTime(time)) ) {
					Vehicle tmp = vehQueue.poll();
					LinkLeaveEvent lle = new LinkLeaveEvent(time,tmp.id,this.l0);
					events.processEvent(lle);
					
				}				
			} 
			
			time += 10;
		}
		IterationStartsEvent iss = new IterationStartsEvent(c,1);
		scalc.notifyIterationStarts(iss);

		//soc cost = T*tbinsize - fstt;
		// 6*(60-10) + 6*(120-10) +  ... + 6*(480-10) = 12480
		double costs = 0.;
		for (; time >= 0; time -= 10) {
			costs += scalc.getSocialCost(this.link0, time);
		}
		assertEquals(12480., costs);
		
		//agent penalty (6 * 12480/6) / -600 = -20.8
		assertEquals(-20.8,apc.penalty);
	}
	
	
	private void setup() {
		this.sc = new ScenarioImpl();
		this.network = this.sc.getNetwork();
		
		this.agents = new ArrayList<Vehicle>();
		for (int i = 0; i < 100; i++) {
			Vehicle v = new Vehicle();
			v.id = this.sc.createId(Integer.toString(i));
			this.agents.add(v);
		}
		
		this.l0 = this.sc.createId("0");
		Id n0 = this.sc.createId("0");
		Id n1 = this.sc.createId("1");

		
		NodeImpl node0 = ((NetworkLayer)this.network).createNode(n0, this.sc.createCoord(0,0));
		NodeImpl node1 = ((NetworkLayer)this.network).createNode(n1, this.sc.createCoord(10,0));
		
		
		
		this.link0 = ((NetworkLayer)this.network).createLink(this.l0,node0,node1,100,10,1,1);
		
		
		
	}


	private static class AgentPenaltyCalculator implements BasicAgentMoneyEventHandler {
		double penalty = 0.;
		
		public void handleEvent(BasicAgentMoneyEvent event) {
			this.penalty += event.getAmount();
		}

		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
	}

	private static class Vehicle {
		Id id;
		double enterTime;
	}
}
