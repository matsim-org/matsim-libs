package org.matsim.planomat.costestimators;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.plans.Route;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class CharyparEtAlCompatibleLegTravelTimeEstimatorTest extends FixedRouteLegTravelTimeEstimatorTest {

	private CharyparEtAlCompatibleLegTravelTimeEstimator testee = null;

//	private static Logger log = Logger.getLogger(CharyparEtAlCompatibleLegTravelTimeEstimatorTest.class);

	@Override
	protected void setUp() throws Exception {

		super.setUp();
	
	}

	public void testGetLegTravelTimeEstimation() {
		DepartureDelayAverageCalculator tDepDelayCalc = super.getTDepDelayCalc();
		TravelTimeCalculator linkTravelTimeEstimator = super.getLinkTravelTimeEstimator();
		
		testee = new CharyparEtAlCompatibleLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		
		Events events = new Events();
		events.addHandler(tDepDelayCalc);
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		Route route = testLeg.getRoute();
		Link[] links = route.getLinkRoute();

		// let's test a route without events first
		// should result in free speed travel time, without departure delay
		double departureTime = Time.parseTime("06:03:00");
		double legTravelTime = testee.getLegTravelTimeEstimation(
				new IdImpl(TEST_PERSON_ID), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());

		double expectedLegEndTime = departureTime;
		expectedLegEndTime += originAct.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		for (Link link : links) {
			expectedLegEndTime += link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
		}
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

		// next, a departure delay of 5s at the origin link is added
		departureTime = Time.parseTime("06:05:00");
		double depDelay = Time.parseTime("00:00:05");
		EventAgentDeparture depEvent = new EventAgentDeparture(departureTime, TEST_PERSON_ID, 0, originAct.getLink().getId().toString());
		EventLinkLeave leaveEvent = new EventLinkLeave(departureTime + depDelay, TEST_PERSON_ID, 0, originAct.getLink().getId().toString());

		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		legTravelTime = testee.getLegTravelTimeEstimation(
				new IdImpl(TEST_PERSON_ID), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());

		expectedLegEndTime = departureTime;
		expectedLegEndTime += depDelay;
		expectedLegEndTime += originAct.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		for (Link link : links) {
			expectedLegEndTime += link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
		}
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

		// now let's add some travel events
		String[][] eventTimes = new String[][]{
				new String[]{"06:05:00", "06:07:00", "06:09:00"},
				new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};

		BasicEvent event = null;
		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < links.length; linkCnt++) {
				event = new EventLinkEnter(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]), 
						TEST_PERSON_ID, 
						TEST_LEG_NR, 
						links[linkCnt].getId().toString());
				events.processEvent(event);
				event = new EventLinkLeave(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]), 
						TEST_PERSON_ID, 
						TEST_LEG_NR, 
						links[linkCnt].getId().toString());
				events.processEvent(event);
			}
		}

		// test a start time where all link departures will be in the first time bin
		departureTime = Time.parseTime("06:10:00");
		legTravelTime = testee.getLegTravelTimeEstimation(
				new IdImpl(TEST_PERSON_ID), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());
		expectedLegEndTime = departureTime;
		expectedLegEndTime += depDelay;
		expectedLegEndTime = testee.processLink(originAct.getLink(), expectedLegEndTime);
		expectedLegEndTime = testee.processRouteTravelTime(route, expectedLegEndTime);
		
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

	}

}
