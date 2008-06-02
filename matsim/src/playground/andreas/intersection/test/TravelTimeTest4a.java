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
 * @author aneumann
 *
 */
public class TravelTimeTest4a extends MatsimTestCase implements	EventHandlerLinkLeaveI, EventHandlerLinkEnterI {

  private Map<Id, Double> agentTravelTimes;
  
	public void testTrafficLightIntersection4arms() {
		this.agentTravelTimes = new HashMap<Id, Double>();
		Config conf = loadConfig("src/playground/andreas/intersection/test/config.xml");
		String popFileName = "src/playground/andreas/intersection/test/data/plans_4a_16.xml.gz";
		String netFileName = "src/playground/andreas/intersection/test/data/net_4a.xml.gz";
		
		String signalSystems = "./src/playground/andreas/intersection/test/data/signalSystemConfig_4a.xml";
		String groupDefinitions = "./src/playground/andreas/intersection/test/data/signalGroupDefinition_4a.xml";

		conf.plans().setInputFile(popFileName);
		conf.network().setInputFile(netFileName);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		new QSim(events, data.getPopulation(), data.getNetwork(), signalSystems, groupDefinitions, false).run();

		assertEquals(118.0, this.agentTravelTimes.get(new IdImpl(1)).intValue(), EPSILON);
		assertEquals(118.0, this.agentTravelTimes.get(new IdImpl(2)).intValue(), EPSILON);
		assertEquals(129.0, this.agentTravelTimes.get(new IdImpl(3)).intValue(), EPSILON);
		assertEquals( 80.0, this.agentTravelTimes.get(new IdImpl(4)).intValue(), EPSILON);
		assertEquals(167.0, this.agentTravelTimes.get(new IdImpl(5)).intValue(), EPSILON);
		assertEquals(111.0, this.agentTravelTimes.get(new IdImpl(6)).intValue(), EPSILON);
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(7)).intValue(), EPSILON);
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(8)).intValue(), EPSILON);
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(9)).intValue(), EPSILON);
		assertEquals(167.0, this.agentTravelTimes.get(new IdImpl(10)).intValue(), EPSILON);
		assertEquals(111.0, this.agentTravelTimes.get(new IdImpl(11)).intValue(), EPSILON);
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(12)).intValue(), EPSILON);
		assertEquals(167.0, this.agentTravelTimes.get(new IdImpl(13)).intValue(), EPSILON);	
		assertEquals(111.0, this.agentTravelTimes.get(new IdImpl(14)).intValue(), EPSILON);	
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(15)).intValue(), EPSILON);	
		assertEquals(110.0, this.agentTravelTimes.get(new IdImpl(16)).intValue(), EPSILON);	
		
	}

	public void handleEvent(EventLinkEnter event) {
		
		if (event.linkId.equalsIgnoreCase("101") || event.linkId.equalsIgnoreCase("201") ||
				event.linkId.equalsIgnoreCase("301") || event.linkId.equalsIgnoreCase("401")) {
			
//			System.out.println(event);
					
			if (this.agentTravelTimes.get(event.agent.getId()) != null) {
				Double startTime = this.agentTravelTimes.get(event.agent.getId());
				this.agentTravelTimes.put(event.agent.getId(), Double.valueOf(event.time - startTime.doubleValue()));
				System.out.println(event.agent.getId() + " : " + this.agentTravelTimes.get(event.agent.getId()));
			}
		}
		
	}

	public void handleEvent(EventLinkLeave event) {

		if (event.linkId.equalsIgnoreCase("20") || event.linkId.equalsIgnoreCase("40") ||
				event.linkId.equalsIgnoreCase("60") || event.linkId.equalsIgnoreCase("80")) {
			
//			System.out.println(event);
					
			if (this.agentTravelTimes.get(event.agent.getId()) == null) {
				this.agentTravelTimes.put(event.agent.getId(), Double.valueOf(event.time));
			}
		}
	}

	public void reset(int iteration) {
	}

}
