package org.matsim.planomat.costestimators;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.events.BasicEvent;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.Route;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class FixedRouteLegTravelTimeEstimatorTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private Plans population = null;
	private final String LINK_ID = "1";
	private final String PERSON_ID = "1";
	private static final int TIME_BIN_SIZE = 900;
	
	private TravelTimeCalculator linkTravelTimeEstimator = null;
	private DepartureDelayAverageCalculator tDepDelayCalc = null;
	private FixedRouteLegTravelTimeEstimator testee = null;
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	
	private static Logger log = Logger.getLogger(FixedRouteLegTravelTimeEstimatorTest.class);
	
	protected void setUp() throws Exception {

		Config config = super.loadConfig(FixedRouteLegTravelTimeEstimatorTest.CONFIGFILE);

		log.info("Reading network xml file...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		population = new Plans(Plans.NO_STREAMING);
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");
		
		tDepDelayCalc = new DepartureDelayAverageCalculator(network, TIME_BIN_SIZE);
		linkTravelTimeEstimator = new TravelTimeCalculator(network, TIME_BIN_SIZE);
		
		testee = new FixedRouteLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Gbl.reset();
	}

	public void testGetLegTravelTimeEstimation() {
		
		double legTravelTimeEstimation = -1.0;
		
		// this method does not do something useful, therefore the meaningless parameters
		legTravelTimeEstimation = testee.getLegTravelTimeEstimation(new IdImpl("1"), 0.0, null, null, null, "dummy mode");
		
		assertEquals(legTravelTimeEstimation, 0.0);
	}

	public void testProcessDeparture() {
		
		Events events = new Events();
		events.addHandler(tDepDelayCalc);
		events.printEventHandlers();
		
		// this gives a delay of 36s (1/100th of an hour)
		EventAgentDeparture depEvent = new EventAgentDeparture(6.03 * 3600, PERSON_ID, 0, LINK_ID);
		EventLinkLeave leaveEvent = new EventLinkLeave(6.04 * 3600, PERSON_ID, 0, LINK_ID);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}
		
		double startTime = 6.00 * 3600;
		double delayEndTime = testee.processDeparture(network.getLink(new IdImpl("1")), startTime);
		assertEquals(delayEndTime, startTime + 36.0);
		
		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new EventAgentDeparture(6.02 * 3600, PERSON_ID, 0, LINK_ID);
		leaveEvent = new EventLinkLeave(6.04 * 3600, PERSON_ID, 0, LINK_ID);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		startTime = 6.00 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(new IdImpl("1")), startTime);
		assertEquals(delayEndTime, startTime + (36.0 + 72.0) / 2);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s
		
		startTime = 5.9 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(new IdImpl("1")), startTime);
		assertEquals(delayEndTime, startTime);

		startTime = 6.26 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(new IdImpl("1")), 6.26 * 3600);
		assertEquals(delayEndTime, startTime);

	}

	public void testProcessRouteTravelTime() {
		
		Events events = new Events();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		// central route through equil-net
		// first person
		Person person = population.getPerson("1");
		// only plan of that person
		Plan plan = person.getPlans().get(0);
		// first leg
		Leg leg = (Leg) plan.getIteratorLeg().next();
		Route route = leg.getRoute();
		log.info(route.toString());
		
		// generate some travel times
		BasicEvent event = null;
		
		Link[] links = route.getLinkRoute();
		System.out.println(links.length);
		
		String[][] eventTimes = new String[][]{
			new String[]{"06:05:00", "06:07:00", "06:09:00"},
			new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};
		
		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < links.length; linkCnt++) {
				event = new EventLinkEnter(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]), 
						person.getId().toString(), 
						leg.getNum(), 
						links[linkCnt].getId().toString());
				events.processEvent(event);
				event = new EventLinkLeave(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]), 
						person.getId().toString(), 
						leg.getNum(), 
						links[linkCnt].getId().toString());
				events.processEvent(event);
			}
		}
		
		// test a start time where all link departures will be in the first time bin
		double startTime = Time.parseTime("06:10:00");
		double routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:14:00"), routeEndTime);
		
		// test a start time where all link departures will be in the second time bin
		startTime = Time.parseTime("06:20:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:30:00"), routeEndTime);
		
		// test a start time in the first bin where one link departure is in the first bin, one in the second bin
		startTime = Time.parseTime("06:13:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:20:00"), routeEndTime);
		
		// test a start time in a free speed bin, having second departure in the first bin
		startTime = Time.parseTime("05:59:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(
				testee.processLink(links[1], startTime + network.getLink(links[0].getId()).getFreespeedTravelTime(-1.0)), 
				routeEndTime);

		// test a start time in the second bin, having second departure in the free speed bin
		startTime = Time.parseTime("06:28:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(
				testee.processLink(links[0], startTime) + network.getLink(links[1].getId()).getFreespeedTravelTime(-1.0),
				routeEndTime
				);

	}

	public void testProcessLink() {

		Events events = new Events();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		// we have one agent on this link, taking 1 minute and 48 seconds
		EventLinkEnter enterEvent = new EventLinkEnter(Time.parseTime("06:05:00"), PERSON_ID, 0, LINK_ID);
		EventLinkLeave leaveEvent = new EventLinkLeave(Time.parseTime("06:06:48"), PERSON_ID, 0, LINK_ID);

		for (BasicEvent event : new BasicEvent[]{enterEvent, leaveEvent}) {
			events.processEvent(event);
		}

		// for start times inside the time bin, the predicted travel time is always the same
		double startTime = Time.parseTime("06:10:00");
		double linkEndTime = testee.processLink(network.getLink(new IdImpl(LINK_ID)), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:11:48"));

		startTime = Time.parseTime("06:01:00");
		linkEndTime = testee.processLink(network.getLink(new IdImpl(LINK_ID)), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:02:48"));
		
		// for start times outside the time bin, the free speed travel time is returned
		double freeSpeedTravelTime = network.getLink(LINK_ID).getFreespeedTravelTime(-1.0);
		
		startTime = Time.parseTime("05:59:00");
		linkEndTime = testee.processLink(network.getLink(new IdImpl(LINK_ID)), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime);
		
		startTime = Time.parseTime("08:12:00");
		linkEndTime = testee.processLink(network.getLink(new IdImpl(LINK_ID)), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime);
		
	}

}
