package org.matsim.core.trafficmonitoring;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

public class TravelTimeDataArrayTest{
	private static final Logger log = Logger.getLogger( TravelTimeDataArrayTest.class );
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {
		Network network = NetworkUtils.createTimeInvariantNetwork();
		Node from = NetworkUtils.createNode( Id.createNodeId( "1" ) );
		Node to = NetworkUtils.createNode( Id.createNodeId( "2" ) );
		Link link = NetworkUtils.createLink( Id.createLinkId( "1-2" ), from, to, network, 10, 10, 10, 1 );
		final int numSlots = 24;
		TravelTimeDataArray abc = new TravelTimeDataArray( link, numSlots );
		log.info( abc.ttToString() );
		log.info( abc.cntToString() );
		abc.resetTravelTimes();
		log.info( abc.ttToString() );
		log.info( abc.cntToString() );
		abc.addTravelTime( 12,123. );
		abc.addTravelTime( 12,123. );
		abc.addTravelTime( 12,123. );
		abc.addTravelTime( 12,123. );
		abc.addTravelTime( 12,123. );
		log.info( abc.ttToString() );
		log.info( abc.cntToString() );
		abc.setTravelTime( 13,111 );
		abc.setTravelTime( 13,111 );
		log.info( abc.ttToString() );
		log.info( abc.cntToString() );
	}

}
