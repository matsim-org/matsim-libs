package playground.wrashid.parkingSearch.planLevel;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class ClosestParkingMatrixTest extends MatsimTestCase {
	
	public void testGetClosestLinks()  {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = LinkFacilityAssociationTest.loadNetwork(sc);
		
		ClosestParkingMatrix cpm = new ClosestParkingMatrix(sc.getActivityFacilities(), net);
		
		LinkedList<Link> links=null;
		
		assertEquals(0, cpm.getClosestLinks(new CoordImpl(0.0, 0.0), 100).size());
		
		links=cpm.getClosestLinks(new CoordImpl(0.0, 0.0), 500);
		assertEquals("1", links.get(0).getId().toString());
		assertEquals("91", links.get(1).getId().toString());
		assertEquals(2, links.size());
	}
	
	public void testGetClosestParkings()  {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = LinkFacilityAssociationTest.loadNetwork(sc);
		
		ClosestParkingMatrix cpm = new ClosestParkingMatrix(sc.getActivityFacilities(), net);
		
		ArrayList<ActivityFacilityImpl> resultFacilities=null;
		
		resultFacilities=cpm.getClosestParkings(new CoordImpl(0.0, 0.0), 100);
		assertEquals(0, resultFacilities.size());
		
		resultFacilities=cpm.getClosestParkings(new CoordImpl(0.0, 0.0), 500);
		assertEquals("19", resultFacilities.get(0).getId().toString());
		assertEquals("1", resultFacilities.get(1).getId().toString());
		assertEquals(2, resultFacilities.size());
		
		
		resultFacilities=cpm.getClosestParkings(new CoordImpl(0.0, 0.0), 2,2);
		assertEquals("19", resultFacilities.get(0).getId().toString());
		assertEquals("1", resultFacilities.get(1).getId().toString());
		assertEquals(2, resultFacilities.size());
		
		// con't here (following tests not running).
		
		resultFacilities = cpm.getClosestParkings(new CoordImpl(0.0, 0.0), 4, 0);
		assertEquals("19", resultFacilities.get(0).getId().toString());
		assertEquals("20", resultFacilities.get(1).getId().toString());
		assertEquals("1", resultFacilities.get(2).getId().toString());
		assertEquals("2", resultFacilities.get(3).getId().toString());
		assertEquals(4, resultFacilities.size());
		
		
	}
	
	
	

}
