package playground.andreas.intersection.test;

import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.testcases.MatsimTestCase;

import playground.andreas.intersection.sim.QSim;

/**
 * @author dgrether
 *
 */
public class TravelTimeTest4a extends MatsimTestCase implements	EventHandlerLinkLeaveI, EventHandlerLinkEnterI {

  private Map<Id, Double> agentTravelTimes;

	public void testEquilTwoAgents() {
		this.agentTravelTimes = new HashMap<Id, Double>();
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/plans_12agents.xml";
		String netFileName = "src/playground/andreas/intersection/test/net_crossing_4arms.xml";
		
		String signalSystems = "./src/playground/andreas/intersection/test/signalSystemConfig_4arms.xml";
		String groupDefinitions = "./src/playground/andreas/intersection/test/signalGroupDefinition_4arms.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		new QSim(events, data.getPopulation(), data.getNetwork(), signalSystems, groupDefinitions).run();

		assertEquals(139.0, agentTravelTimes.get(new IdImpl(1)).intValue(), EPSILON);
		assertEquals(140.0, agentTravelTimes.get(new IdImpl(2)).intValue(), EPSILON);
		assertEquals(141.0, agentTravelTimes.get(new IdImpl(3)).intValue(), EPSILON);
		assertEquals(101.0, agentTravelTimes.get(new IdImpl(4)).intValue(), EPSILON);
		assertEquals(100.0, agentTravelTimes.get(new IdImpl(5)).intValue(), EPSILON);
		assertEquals(99.0, agentTravelTimes.get(new IdImpl(6)).intValue(), EPSILON);
		assertEquals(109.0, agentTravelTimes.get(new IdImpl(7)).intValue(), EPSILON);
		assertEquals(110.0, agentTravelTimes.get(new IdImpl(8)).intValue(), EPSILON);
		assertEquals(111.0, agentTravelTimes.get(new IdImpl(9)).intValue(), EPSILON);
		assertEquals(124.0, agentTravelTimes.get(new IdImpl(10)).intValue(), EPSILON);
		assertEquals(125.0, agentTravelTimes.get(new IdImpl(11)).intValue(), EPSILON);
		assertEquals(126.0, agentTravelTimes.get(new IdImpl(12)).intValue(), EPSILON);
		
	}

	public void handleEvent(EventLinkEnter event) {
		
		if (event.linkId.equalsIgnoreCase("20") || event.linkId.equalsIgnoreCase("40") ||
				event.linkId.equalsIgnoreCase("60") || event.linkId.equalsIgnoreCase("80")) {
			
//			System.out.println(event);
					
			if (this.agentTravelTimes.get(event.agent.getId()) != null) {
				double startTime = this.agentTravelTimes.get(event.agent.getId());
				agentTravelTimes.put(event.agent.getId(), Double.valueOf(event.time) - startTime);
				System.out.println(event.agent.getId() + " : " + agentTravelTimes.get(event.agent.getId()));
			}
		}
		
	}

	public void handleEvent(EventLinkLeave event) {

		if (event.linkId.equalsIgnoreCase("20") || event.linkId.equalsIgnoreCase("40") ||
				event.linkId.equalsIgnoreCase("60") || event.linkId.equalsIgnoreCase("80")) {
			
//			System.out.println(event);
					
			if (this.agentTravelTimes.get(event.agent.getId()) == null) {
				agentTravelTimes.put(event.agent.getId(), Double.valueOf(event.time));
			}
		}
	}

	public void reset(int iteration) {
	}

}
