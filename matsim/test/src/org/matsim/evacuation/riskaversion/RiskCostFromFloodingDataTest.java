package org.matsim.evacuation.riskaversion;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentMoneyEventHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigReaderMatsimV1;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.testcases.MatsimTestCase;


public class RiskCostFromFloodingDataTest extends MatsimTestCase {

	public void testRiskCostFromFloodingData() {
		String config = getInputDirectory() + "config.xml";
		Scenario sc = new ScenarioImpl();
		new ConfigReaderMatsimV1(sc.getConfig()).readFile(config);
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(sc.getConfig().network().getInputFile());
			
		FloodingReader fr = new FloodingReader(sc.getConfig().evacuation().getFloodingDataFile(),true);
		
		Events events = new Events();
		RiskCostFromFloodingData rcf = new RiskCostFromFloodingData(sc.getNetwork(),fr,events);
		
		double delta = Math.pow(10, -6);
		
		//// travel costs
		//test 2 links within flooding area
		//Link 11288 cost = 33703318.3116002
		Link l0 = sc.getNetwork().getLink(new IdImpl("11288"));
		double l0Cost = rcf.getLinkRisk(l0);
		assertEquals(33703318.3116002,l0Cost,delta);
		
		//Link 111288 cost = 0. (opposite direction)
		Link l0Inverse = sc.getNetwork().getLink(new IdImpl("111288"));
		double l0InverseCost = rcf.getLinkRisk(l0Inverse);
		assertEquals(0,l0InverseCost,delta);
		
		//test 2 links within buffer
		//Link 9204 cost = 124442.470964684
		Link l1 = sc.getNetwork().getLink(new IdImpl("9204"));
		double l1Cost = rcf.getLinkRisk(l1);
		assertEquals(124442.470964684,l1Cost,delta);
		
		//Link 109204 cost = 0. (opposite direction)
		Link l1Inverse = sc.getNetwork().getLink(new IdImpl("109204"));
		double l1InverseCost = rcf.getLinkRisk(l1Inverse);
		assertEquals(0,l1InverseCost,delta);
		
		//test 2 links at the edge of the buffer
		//Link 6798 cost = 497605.266434762
		Link l2 = sc.getNetwork().getLink(new IdImpl("6798"));
		double l2Cost = rcf.getLinkRisk(l2);
		assertEquals(497605.266434762,l2Cost,delta);
		
		//Link 106798 cost = 0. (opposite direction)
		Link l2Inverse = sc.getNetwork().getLink(new IdImpl("106798"));
		double l2InverseCost = rcf.getLinkRisk(l2Inverse);
		assertEquals(0,l2InverseCost,delta);
		
		//// agent penalties
		Id id = sc.createId("agent");
		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();
		events.addHandler(apc);
		events.addHandler(rcf);
		double refCost = 0.;
		
		events.processEvent(new LinkEnterEvent(0.,id,l0.getId()));
		refCost += 33703318.3116002 / -600.;
		events.processEvent(new LinkEnterEvent(0.,id,l0Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEvent(0.,id,l1.getId()));
		refCost += 124442.470964684 / -600.;
		events.processEvent(new LinkEnterEvent(0.,id,l1Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEvent(0.,id,l2.getId()));
		refCost += 497605.266434762 / -600.;
		events.processEvent(new LinkEnterEvent(0.,id,l2Inverse.getId()));
		refCost += 0.;
		
		
		assertEquals(refCost, apc.penalty,delta);
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
}
