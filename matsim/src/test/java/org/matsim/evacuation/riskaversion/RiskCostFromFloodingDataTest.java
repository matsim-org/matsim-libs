package org.matsim.evacuation.riskaversion;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigReaderMatsimV1;
import org.matsim.core.config.Module;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.evacuation.config.EvacuationConfigGroup;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.testcases.MatsimTestCase;

public class RiskCostFromFloodingDataTest extends MatsimTestCase {

	public void testRiskCostFromFloodingData() {
		String config = getInputDirectory() + "config.xml";
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new ConfigReaderMatsimV1(sc.getConfig()).readFile(config);
		Module m = sc.getConfig().getModule("evacuation");
		EvacuationConfigGroup ec = new EvacuationConfigGroup(m);
		sc.getConfig().getModules().put("evacuation", ec);

		new MatsimNetworkReader(sc).readFile(sc.getConfig().network().getInputFile());

		double offsetEast = ec.getSWWOffsetEast();
		double offsetNorth = ec.getSWWOffsetNorth();
		List<FloodingReader> frs = new ArrayList<FloodingReader>();
		for (int i = 0; i < ec.getSWWFileCount(); i++) {
			String netcdf = ec.getSWWRoot() + "/" + ec.getSWWFilePrefix() + i + ec.getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setOffset(offsetEast, offsetNorth);
			// fr.setMaxTimeStep(60);
			frs.add(fr);
		}

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		RiskCostFromFloodingData rcf = new RiskCostFromFloodingData(sc.getNetwork(), frs, events, ec.getBufferSize());

		double delta = Math.pow(10, -6);

		// // travel costs

		// baseCost = 3 *3600

		// test 2 links within flooding area
		// nodeCost = baseCost-nodeFloodTime

		// Link 11288 cost = 28044.9329790229
		// fromNodeTime = 35.54864794603423333333
		// fromNodeCost = baseCost - 60 * fromNodeTime = 8667.081123237946
		// toNodeTime = 33.04903456966766666666
		// tNodeCost = baseCost - 60 * fromNodeTime = 8817.05792581994
		// linkLength = 318.075861755381
		// linkCost = 8667.081123237946 * linkLength / 100 = 28044.9329790229
		Link l0 = sc.getNetwork().getLinks().get(new IdImpl("11288"));
		double l0Cost = rcf.getLinkGeneralizedTravelCost(l0, Time.UNDEFINED_TIME);
		assertEquals(28044.9329790229, l0Cost, delta);

		// Link 111288 cost = 0. (opposite direction)
		// fromNodeTime = 33.04903456966766666666
		// toNodeTime = 35.54864794603423333333
		// fromNodeTime < toNodeTime --> linkCost = 0
		Link l0Inverse = sc.getNetwork().getLinks().get(new IdImpl("111288"));
		double l0InverseCost = rcf.getLinkGeneralizedTravelCost(l0Inverse, Time.UNDEFINED_TIME);
		assertEquals(0, l0InverseCost, delta);

		// bufferSize = 250

		// test 2 links within buffer
		// nodeCost = baseCost/2 * (1 - (nodeFloodDist/bufferSize))

		// Link 9204 cost = 124.44247096468449
		// fromNodeDist = 223.12500852607877
		// fromNodeCost = 3 * 3600 / 2 * (1 -(223.12500852607877/250)) =
		// 580.4998158366984
		// toNodeDist = 212.98510568617348
		// toNodeCost = 3 * 3600 / 2 * (1 -(212.98510568617348 /250)) =
		// 799.5217171786527
		// fromNodeCost < toNodeCost
		// linkLength = 15.5646142300945
		// linkCost = toNodeCost * linkLength / 100 = 124.44247096468449
		Link l1 = sc.getNetwork().getLinks().get(new IdImpl("9204"));
		double l1Cost = rcf.getLinkGeneralizedTravelCost(l1, Time.UNDEFINED_TIME);
		assertEquals(124.44247096468449, l1Cost, delta);

		// Link 109204 cost = 0. (opposite direction)
		// toNodeDist = 223.125
		// toNodeCost = 3 * 3600 / 2 * (1 -(223.125 /250)) --> 580.4998158366984
		// fromNodeDist = 212.985
		// ffromNodeCost = 3 * 3600 / 2 * (1 -(223.125 /250)) --> ca. 799.5
		// toNodeCost < fromNodeCost --> linkCost = 0
		Link l1Inverse = sc.getNetwork().getLinks().get(new IdImpl("109204"));
		double l1InverseCost = rcf.getLinkGeneralizedTravelCost(l1Inverse, Time.UNDEFINED_TIME);
		assertEquals(0, l1InverseCost, delta);

		// test 2 links at the edge of the buffer
		// Link 6798 cost = 497.60526643476226
		// toNodeDist = 223.125
		// toNodeCost = 3 * 3600 / 2 * (1 -(223.125 /250)) --> 580.4998158366984
		// fromNode = null
		// linkLength = 85.7201419982439
		// linkCost = toNodeCost * linkLength / 100 = 497.60526643476226
		Link l2 = sc.getNetwork().getLinks().get(new IdImpl("6798"));
		double l2Cost = rcf.getLinkGeneralizedTravelCost(l2, Time.UNDEFINED_TIME);
		assertEquals(497.60526643476226, l2Cost, delta);

		// Link 106798 cost = 0. (opposite direction)
		Link l2Inverse = sc.getNetwork().getLinks().get(new IdImpl("106798"));
		double l2InverseCost = rcf.getLinkGeneralizedTravelCost(l2Inverse, Time.UNDEFINED_TIME);
		assertEquals(0, l2InverseCost, delta);

		// // agent penalties
		Id id = sc.createId("agent");
		AgentPenaltyCalculator apc = new AgentPenaltyCalculator();
		events.addHandler(apc);
		events.addHandler(rcf);
		double refCost = 0.;

		events.processEvent(new LinkEnterEventImpl(0., id, l0.getId()));
		refCost += 28044.9329790229 / -600.;
		events.processEvent(new LinkEnterEventImpl(0., id, l0Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEventImpl(0., id, l1.getId()));
		refCost += 124.44247096468449 / -600.;
		events.processEvent(new LinkEnterEventImpl(0., id, l1Inverse.getId()));
		refCost += 0.;
		events.processEvent(new LinkEnterEventImpl(0., id, l2.getId()));
		refCost += 497.60526643476226 / -600.;
		events.processEvent(new LinkEnterEventImpl(0., id, l2Inverse.getId()));
		refCost += 0.;

		assertEquals(refCost, apc.penalty, delta);
	}

	private static class AgentPenaltyCalculator implements AgentMoneyEventHandler {
		double penalty = 0.;

		public void handleEvent(AgentMoneyEvent event) {
			this.penalty += event.getAmount();
		}

		public void reset(int iteration) {
		}

	}
}
