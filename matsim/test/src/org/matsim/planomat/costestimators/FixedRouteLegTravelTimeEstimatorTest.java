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
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class FixedRouteLegTravelTimeEstimatorTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private final String LINK_ID = "1";
	private final String PERSON_ID = "1";
	
	private TravelTimeCalculator linkTravelTimeEstimator = null;
	private FixedRouteLegTravelTimeEstimator testee = null;
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	
	private static Logger log = Logger.getLogger(FixedRouteLegTravelTimeEstimatorTest.class);
	
	protected void setUp() throws Exception {

		Config config = super.loadConfig(FixedRouteLegTravelTimeEstimatorTest.CONFIGFILE);
		
		log.info("Creating network layer...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		log.info("Creating network layer...done.");

		log.info("Reading network xml file...");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		linkTravelTimeEstimator = new TravelTimeCalculator(network, 900);
		
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
//		fail("Not yet implemented");
	}

	public void testProcessRouteTravelTime() {
//		fail("Not yet implemented");
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
