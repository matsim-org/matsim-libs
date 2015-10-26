package playground.southafrica.freight.digicore.algorithms.complexNetworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkBuilder;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;

public class DigicoreNetworkBuilderTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	/**
	 * The following little graph is used:
	 * 
	 *  2      -------> 3
	 *  ^     /       /
	 *  |   w:2      /
	 *  |   /      w:1
	 * w:1 /       /
	 *  | /       /
	 *  |/<-------
	 *  1 <--- w:3 ---- 4
	 */
	@Test
	public void testCheckChain(){
		DigicoreNetworkBuilder dfg = new DigicoreNetworkBuilder();

		/*----------------- Create the chain. ----------------------*/
		DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da1.setCoord(new Coord(0.0, 0.0));
		/* No facility Id. */	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new Coord(0.0, 1.0));
		da2.setFacilityId(Id.create(1, ActivityFacility.class));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new Coord(1.0, 1.0));
		/* No facility Id. */
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new Coord(1.0, 0.0));
		da4.setFacilityId(Id.create(4, ActivityFacility.class));

		
		DigicoreChain chain = new DigicoreChain();
		chain.add(da1);
		chain.add(da2);
		chain.add(da3);
		chain.add(da4);
		/*-----------------------------------------------------------*/

		List<Id<ActivityFacility>> filter1 = null;
		Assert.assertTrue("Should accept the chain.", dfg.checkChain(chain, filter1));
		
		List<Id<ActivityFacility>> filter2 = new ArrayList<Id<ActivityFacility>>();
		filter2.add(Id.create(3, ActivityFacility.class));
		Assert.assertFalse("Should not accept the chain.", dfg.checkChain(chain, filter2));
		
		List<Id<ActivityFacility>> filter3 = new ArrayList<Id<ActivityFacility>>();
		filter3.add(Id.create(4, ActivityFacility.class));
		Assert.assertTrue("Should accept the chain.", dfg.checkChain(chain, filter3));

		List<Id<ActivityFacility>> list1 = new ArrayList<Id<ActivityFacility>>(2);
		list1.add(Id.create(1, ActivityFacility.class));
		list1.add(Id.create(2, ActivityFacility.class));
		List<Id<ActivityFacility>> filter4 = list1;
		Assert.assertTrue("Should accept the chain.", dfg.checkChain(chain, filter4));

		List<Id<ActivityFacility>> list2 = new ArrayList<Id<ActivityFacility>>(2);
		list2.add(Id.create(2, ActivityFacility.class));
		list2.add(Id.create(3, ActivityFacility.class));
		List<Id<ActivityFacility>> filter5 = list2;
		Assert.assertFalse("Should not accept the chain.", dfg.checkChain(chain, filter5));
	}


	/** 
	 * Nothing can really fail here...
	 */
	@Test
	public void testWriteGraphStatistics(){
		try{
			DigicoreNetworkBuilder dfg = new DigicoreNetworkBuilder();

			/*----------------- Create the graph. ----------------------*/
			DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da1.setCoord(new Coord(0.0, 0.0));
			da1.setFacilityId(Id.create(1, ActivityFacility.class));	
			
			DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da2.setCoord(new Coord(0.0, 1.0));
			da2.setFacilityId(Id.create(2, ActivityFacility.class));
			
			DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da3.setCoord(new Coord(1.0, 1.0));
			da3.setFacilityId(Id.create(3, ActivityFacility.class));
			
			DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da4.setCoord(new Coord(1.0, 0.0));
			da4.setFacilityId(Id.create(4, ActivityFacility.class));

			dfg.getNetwork().addArc(da1, da2);
			dfg.getNetwork().addArc(da1, da3);
			dfg.getNetwork().addArc(da1, da3);
			dfg.getNetwork().addArc(da3, da1);
			dfg.getNetwork().addArc(da4, da1);
			dfg.getNetwork().addArc(da4, da1);
			dfg.getNetwork().addArc(da4, da1);
			/*-----------------------------------------------------------*/
			
			dfg.writeGraphStatistics();
			
		} catch(Exception e){
			Assert.fail("Should not have any exceptions.");
		}
	}
	
	

}
