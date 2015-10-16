package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.binary.ChainComparator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

public class ChainComparatorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testSetup() {
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		
		Assert.assertEquals("Wrong number of chains.", 2, vehicle.getChains().size());
		
		Assert.assertEquals("Wrong umber of activities in chain 1.", 4, vehicle.getChains().get(0).size());
		Assert.assertTrue("Wrong activity: 'a' should be in there.", vehicle.getChains().get(0).get(0).getType().equalsIgnoreCase("a"));
		Assert.assertTrue("Wrong activity: 'b' should be in there.", vehicle.getChains().get(0).get(1).getType().equalsIgnoreCase("b"));
		Assert.assertTrue("Wrong activity: 'c' should be in there.", vehicle.getChains().get(0).get(2).getType().equalsIgnoreCase("c"));
		Assert.assertTrue("Wrong activity: 'd' should be in there.", vehicle.getChains().get(0).get(3).getType().equalsIgnoreCase("d"));
		
		Assert.assertEquals("Wrong umber of activities in chain 2.", 4, vehicle.getChains().get(1).size());
		Assert.assertTrue("Wrong activity: 'a' should be in there.", vehicle.getChains().get(1).get(0).getType().equalsIgnoreCase("a"));
		Assert.assertTrue("Wrong activity: 'c' should be in there.", vehicle.getChains().get(1).get(1).getType().equalsIgnoreCase("c"));
		Assert.assertTrue("Wrong activity: 'd' should be in there.", vehicle.getChains().get(1).get(2).getType().equalsIgnoreCase("d"));
		Assert.assertTrue("Wrong activity: 'e' should be in there.", vehicle.getChains().get(1).get(3).getType().equalsIgnoreCase("e"));
	}
	
	@Test
	public void testConstructor(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		
		Assert.assertEquals("Wrong number of Ids in list. Should be empty.", 0, cc.getFacilityIds().size());
		Assert.assertNull("Quadtree should still be null.", cc.getQuadTree());
	}
	
	
	@Test
	public void testBuildActivityFacilityList(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		/* Check list of facilities. */
		Assert.assertEquals("Wrong number of Ids in list.", 5, cc.getFacilityIds().size());
		Assert.assertTrue("Facility 'f1' not in list", cc.getFacilityIds().contains(Id.create("f1", ActivityFacility.class)));
		Assert.assertTrue("Facility 'f2' not in list", cc.getFacilityIds().contains(Id.create("f2", ActivityFacility.class)));
		Assert.assertTrue("Facility 'f3' not in list", cc.getFacilityIds().contains(Id.create("f3", ActivityFacility.class)));
		Assert.assertTrue("Facility 'a0' not in list", cc.getFacilityIds().contains(Id.create("a0", ActivityFacility.class)));
		Assert.assertTrue("Facility 'a1' not in list", cc.getFacilityIds().contains(Id.create("a1", ActivityFacility.class)));
		
		/* Check QuadTree. */
		Assert.assertEquals("Wrong number of Ids in QuadTree.", 2, cc.getQuadTree().size());
		Assert.assertTrue("Facility 'a0' not in the QuadTree where its supposed to be.", cc.getQuadTree().getClosest(2, 0).equals(Id.create("a0", Facility.class)));
		Assert.assertTrue("Facility 'a1' not in the QuadTree where its supposed to be.", cc.getQuadTree().getClosest(8, 0).equals(Id.create("a1", Facility.class)));
	}

	
	@Test
	public void testGetActivityPosition(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		DigicoreChain chain = vehicle.getChains().get(0);
		
		Assert.assertEquals("Wrong position for facility f1", 
				0, 
				cc.getActivityPosition(chain, Id.create("f1", ActivityFacility.class)).intValue());
		Assert.assertEquals("Wrong position for facility a0", 
				1, 
				cc.getActivityPosition(chain, Id.create("a0", ActivityFacility.class)).intValue());
		Assert.assertEquals("Wrong position for facility f2", 
				2, 
				cc.getActivityPosition(chain, Id.create("f2", ActivityFacility.class)).intValue());
		Assert.assertEquals("Wrong position for facility f3", 
				3, 
				cc.getActivityPosition(chain, Id.create("f3", ActivityFacility.class)).intValue());
		Assert.assertNull("Wrong position for facility a1", 
				cc.getActivityPosition(chain, Id.create("a1", ActivityFacility.class)) );
	}

	
	@Test
	public void testCompareChains(){
		DigicoreVehicle vehicle = setupVehicleWithTwoChains();
		ChainComparator cc = new ChainComparator(vehicle);
		cc.buildActivityFacilityList();
		
		DigicoreChain chain1 = vehicle.getChains().get(0);
		DigicoreChain chain2 = vehicle.getChains().get(1);
		Assert.assertEquals("Wrong comparison value for penalty 10.", 22.0, cc.compareChains(chain1, chain2, 10), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong comparison value for penalty 10.", 202.0, cc.compareChains(chain1, chain2, 100), MatsimTestUtils.EPSILON);
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
		DigicoreVehicle vehicle = new DigicoreVehicle(Id.create("1", Vehicle.class));
		
		/* Set up all the activities. */
		DigicoreActivity a = new DigicoreActivity("a", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		a.setCoord(new Coord(0.0, 0.0));
		a.setFacilityId(Id.create("f1", ActivityFacility.class));
		DigicoreActivity b = new DigicoreActivity("b", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		b.setCoord(new Coord(2.0, 0.0));
		DigicoreActivity c = new DigicoreActivity("c", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		c.setCoord(new Coord(4.0, 0.0));
		c.setFacilityId(Id.create("f2", ActivityFacility.class));
		DigicoreActivity d = new DigicoreActivity("d", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		d.setCoord(new Coord(6.0, 0.0));
		d.setFacilityId(Id.create("f3", ActivityFacility.class));
		DigicoreActivity e = new DigicoreActivity("e", TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		e.setCoord(new Coord(8.0, 0.0));
		
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
