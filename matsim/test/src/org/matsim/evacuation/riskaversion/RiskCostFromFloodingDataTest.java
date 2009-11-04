package org.matsim.evacuation.riskaversion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentMoneyEventHandler;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigReaderMatsimV1;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.testcases.MatsimTestCase;


public class RiskCostFromFloodingDataTest extends MatsimTestCase {

	public void testRiskCostFromFloodingData() {
		String config = getInputDirectory() + "config.xml";
		ScenarioImpl sc = new ScenarioImpl();
		new ConfigReaderMatsimV1(sc.getConfig()).readFile(config);
		
		new MatsimNetworkReader(sc.getNetwork()).readFile(sc.getConfig().network().getInputFile());
			
		double offsetEast = sc.getConfig().evacuation().getSWWOffsetEast();
		double offsetNorth = sc.getConfig().evacuation().getSWWOffsetNorth();
		List<FloodingReader> frs = new ArrayList<FloodingReader>();
		for (int i = 0; i < sc.getConfig().evacuation().getSWWFileCount(); i++) {
			String netcdf = sc.getConfig().evacuation().getSWWRoot() + "/" + sc.getConfig().evacuation().getSWWFilePrefix() + i + sc.getConfig().evacuation().getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setOffset(offsetEast, offsetNorth);
//			fr.setMaxTimeStep(60);
			frs .add(fr);
		}


		
		
		
		
		EventsManagerImpl events = new EventsManagerImpl();
		RiskCostFromFloodingData rcf = new RiskCostFromFloodingData(sc.getNetwork(),frs,events,sc.getConfig().evacuation().getBufferSize());
		
		double delta = Math.pow(10, -6);
		
		//// travel costs
		//test 2 links within flooding area
		//Link 11288 cost = 33703318.3116002
		LinkImpl l0 = sc.getNetwork().getLink(new IdImpl("11288"));
		double l0Cost = rcf.getLinkTravelCost(l0,Time.UNDEFINED_TIME);
		assertEquals(33703318.3116002,l0Cost,delta);
		
		//Link 111288 cost = 0. (opposite direction)
		LinkImpl l0Inverse = sc.getNetwork().getLink(new IdImpl("111288"));
		double l0InverseCost = rcf.getLinkTravelCost(l0Inverse,Time.UNDEFINED_TIME);
		assertEquals(0,l0InverseCost,delta);
		
		//test 2 links within buffer
		//Link 9204 cost = 124442.470964684
		LinkImpl l1 = sc.getNetwork().getLink(new IdImpl("9204"));
		double l1Cost = rcf.getLinkTravelCost(l1,Time.UNDEFINED_TIME);
		assertEquals(124442.470964684,l1Cost,delta);
		
		//Link 109204 cost = 0. (opposite direction)
		LinkImpl l1Inverse = sc.getNetwork().getLink(new IdImpl("109204"));
		double l1InverseCost = rcf.getLinkTravelCost(l1Inverse,Time.UNDEFINED_TIME);
		assertEquals(0,l1InverseCost,delta);
		
		//test 2 links at the edge of the buffer
		//Link 6798 cost = 497605.266434762
		LinkImpl l2 = sc.getNetwork().getLink(new IdImpl("6798"));
		double l2Cost = rcf.getLinkTravelCost(l2,Time.UNDEFINED_TIME);
		assertEquals(497605.266434762,l2Cost,delta);
		
		//Link 106798 cost = 0. (opposite direction)
		LinkImpl l2Inverse = sc.getNetwork().getLink(new IdImpl("106798"));
		double l2InverseCost = rcf.getLinkTravelCost(l2Inverse,Time.UNDEFINED_TIME);
		assertEquals(0,l2InverseCost,delta);
		
		//// agent penalties
		Id id = sc.createId("agent");
		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();
		events.addHandler(apc);
		events.addHandler(rcf);
		double refCost = 0.;
		
		events.processEvent(new LinkEnterEventImpl(0.,id,l0.getId()));
		refCost += 33703318.3116002 / -600.;
		events.processEvent(new LinkEnterEventImpl(0.,id,l0Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEventImpl(0.,id,l1.getId()));
		refCost += 124442.470964684 / -600.;
		events.processEvent(new LinkEnterEventImpl(0.,id,l1Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEventImpl(0.,id,l2.getId()));
		refCost += 497605.266434762 / -600.;
		events.processEvent(new LinkEnterEventImpl(0.,id,l2Inverse.getId()));
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
