package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity;

import java.util.Locale;
import java.util.TimeZone;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.binary.ChainComparator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class ChainComparatorTest extends MatsimTestCase {

	public void testSetup() {
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		
		assertEquals("Wrong number of chains.", 2, vehicle.getChains().size());
		
		assertEquals("Wrong umber of activities in chain 1.", 4, vehicle.getChains().get(0).size());
		assertTrue("Wrong activity: 'a' should be in there.", vehicle.getChains().get(0).get(0).getType().equalsIgnoreCase("a"));
		assertTrue("Wrong activity: 'b' should be in there.", vehicle.getChains().get(0).get(1).getType().equalsIgnoreCase("b"));
		assertTrue("Wrong activity: 'c' should be in there.", vehicle.getChains().get(0).get(2).getType().equalsIgnoreCase("c"));
		assertTrue("Wrong activity: 'd' should be in there.", vehicle.getChains().get(0).get(3).getType().equalsIgnoreCase("d"));
		
		assertEquals("Wrong umber of activities in chain 2.", 4, vehicle.getChains().get(1).size());
		assertTrue("Wrong activity: 'a' should be in there.", vehicle.getChains().get(1).get(0).getType().equalsIgnoreCase("a"));
		assertTrue("Wrong activity: 'c' should be in there.", vehicle.getChains().get(1).get(1).getType().equalsIgnoreCase("c"));
		assertTrue("Wrong activity: 'd' should be in there.", vehicle.getChains().get(1).get(2).getType().equalsIgnoreCase("d"));
		assertTrue("Wrong activity: 'e' should be in there.", vehicle.getChains().get(1).get(3).getType().equalsIgnoreCase("e"));
	}
	
	public void testConstructor(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		
		assertEquals("Wrong number of Ids in list. Should be empty.", 0, cc.getFacilityIds().size());
		assertNull("Quadtree should still be null.", cc.getQuadTree());
	}
	
	
	public void testBuildActivityFacilityList(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		/* Check list of facilities. */
		assertEquals("Wrong number of Ids in list.", 5, cc.getFacilityIds().size());
		assertTrue("Facility 'f1' not in list", cc.getFacilityIds().contains(new IdImpl("f1")));
		assertTrue("Facility 'f2' not in list", cc.getFacilityIds().contains(new IdImpl("f2")));
		assertTrue("Facility 'f3' not in list", cc.getFacilityIds().contains(new IdImpl("f3")));
		assertTrue("Facility 'a0' not in list", cc.getFacilityIds().contains(new IdImpl("a0")));
		assertTrue("Facility 'a1' not in list", cc.getFacilityIds().contains(new IdImpl("a1")));
		
		/* Check QuadTree. */
		assertEquals("Wrong number of Ids in QuadTree.", 2, cc.getQuadTree().size());
		assertTrue("Facility 'a0' not in the QuadTree where its supposed to be.", cc.getQuadTree().get(2, 0).equals(new IdImpl("a0")));
		assertTrue("Facility 'a1' not in the QuadTree where its supposed to be.", cc.getQuadTree().get(8, 0).equals(new IdImpl("a1")));
	}

	
	public void testGetActivityPosition(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		DigicoreChain chain = vehicle.getChains().get(0);
		
		assertEquals("Wrong position for facility f1", 0, cc.getActivityPosition(chain, new IdImpl("f1")).intValue());
		assertEquals("Wrong position for facility a0", 1, cc.getActivityPosition(chain, new IdImpl("a0")).intValue());
		assertEquals("Wrong position for facility f2", 2, cc.getActivityPosition(chain, new IdImpl("f2")).intValue());
		assertEquals("Wrong position for facility f3", 3, cc.getActivityPosition(chain, new IdImpl("f3")).intValue());
		assertNull("Wrong position for facility a1", cc.getActivityPosition(chain, new IdImpl("a1")) );
	}

	
	public void testCompareChains(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		DigicoreChain chain1 = vehicle.getChains().get(0);
		DigicoreChain chain2 = vehicle.getChains().get(1);
		assertEquals("Wrong comparison value for penalty 10.", 22.0, cc.compareChains(chain1, chain2, 10));
		assertEquals("Wrong comparison value for penalty 10.", 202.0, cc.compareChains(chain1, chain2, 100));
	}
	
	
	/**
	 * Sets up the following two activity chains for comparison:
	 *   f1          f2    f3
	 * (0,0) (2,0) (4,0) (6,0)
	 *   a --> b --> c --> d
	 *   
	 *   a --> c --> d --> e
	 * (0,0) (4,0) (6,0) (8,0)
	 *   f1    f2    f3   
	 */
	private DigicoreVehicle setupVehicleWithTwoChains(){
		DigicoreVehicle vehicle = new DigicoreVehicle(new IdImpl("1"));
		
		/* Set up all the activities. */
		DigicoreActivity a = new DigicoreActivity("a", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH); 
		a.setCoord(new CoordImpl(0.0, 0.0));
		a.setFacilityId(new IdImpl("f1"));
		DigicoreActivity b = new DigicoreActivity("b", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH); 
		b.setCoord(new CoordImpl(2.0, 0.0));
		DigicoreActivity c = new DigicoreActivity("c", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH); 
		c.setCoord(new CoordImpl(4.0, 0.0));
		c.setFacilityId(new IdImpl("f2"));
		DigicoreActivity d = new DigicoreActivity("d", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH); 
		d.setCoord(new CoordImpl(6.0, 0.0));
		d.setFacilityId(new IdImpl("f3"));
		DigicoreActivity e = new DigicoreActivity("e", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH); 
		e.setCoord(new CoordImpl(8.0, 0.0));
		
		/* Set up chain 1. */
		DigicoreChain chain1 = new DigicoreChain();
		chain1.add(a);
		chain1.add(b);
		chain1.add(c);
		chain1.add(d);
		vehicle.getChains().add(chain1);
		
		/* Set up chain 2. */
		DigicoreChain chain2 = new DigicoreChain();
		chain2.add(a);
		chain2.add(c);
		chain2.add(d);
		chain2.add(e);
		vehicle.getChains().add(chain2);		
		
		return vehicle;
	}
	
	
}
