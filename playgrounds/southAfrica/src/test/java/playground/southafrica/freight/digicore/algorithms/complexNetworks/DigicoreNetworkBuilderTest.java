package playground.southafrica.freight.digicore.algorithms.complexNetworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkBuilder;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;

public class DigicoreNetworkBuilderTest extends MatsimTestCase{

	
	
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
	public void testCheckChain(){
		DigicoreNetworkBuilder dfg = new DigicoreNetworkBuilder();

		/*----------------- Create the chain. ----------------------*/
		DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));		
		da1.setCoord(new CoordImpl(0.0, 0.0));						
		/* No facility Id. */	
		
		DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da2.setCoord(new CoordImpl(0.0, 1.0));
		da2.setFacilityId(new IdImpl(2));
		
		DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da3.setCoord(new CoordImpl(1.0, 1.0));
		/* No facility Id. */
		
		DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
		da4.setCoord(new CoordImpl(1.0, 0.0));
		da4.setFacilityId(new IdImpl(4));

		
		DigicoreChain chain = new DigicoreChain();
		chain.add(da1);
		chain.add(da2);
		chain.add(da3);
		chain.add(da4);
		/*-----------------------------------------------------------*/

		List<Id> filter1 = null;
		assertTrue("Should accept the chain.", dfg.checkChain(chain, filter1));
		
		List<Id> filter2 = new ArrayList<Id>();
		filter2.add(new IdImpl(3));
		assertFalse("Should not accept the chain.", dfg.checkChain(chain, filter2));
		
		List<Id> filter3 = new ArrayList<Id>();
		filter3.add(new IdImpl(4));
		assertTrue("Should accept the chain.", dfg.checkChain(chain, filter3));

		List<Id> list1 = new ArrayList<Id>(2);
		list1.add(new IdImpl(1));
		list1.add(new IdImpl(2));
		List<Id> filter4 = list1;
		assertTrue("Should accept the chain.", dfg.checkChain(chain, filter4));

		List<Id> list2 = new ArrayList<Id>(2);
		list2.add(new IdImpl(1));
		list2.add(new IdImpl(3));
		List<Id> filter5 = list2;
		assertFalse("Should not accept the chain.", dfg.checkChain(chain, filter5));
	}


	/** 
	 * Nothing can really fail here...
	 */
	public void testWriteGraphStatistics(){
		try{
			DigicoreNetworkBuilder dfg = new DigicoreNetworkBuilder();

			/*----------------- Create the graph. ----------------------*/
			DigicoreActivity da1 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));		
			da1.setCoord(new CoordImpl(0.0, 0.0));						
			da1.setFacilityId(new IdImpl(1));	
			
			DigicoreActivity da2 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da2.setCoord(new CoordImpl(0.0, 1.0));
			da2.setFacilityId(new IdImpl(2));
			
			DigicoreActivity da3 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da3.setCoord(new CoordImpl(1.0, 1.0));
			da3.setFacilityId(new IdImpl(3));
			
			DigicoreActivity da4 = new DigicoreActivity("test", TimeZone.getTimeZone("GMT+2"), new Locale("en"));
			da4.setCoord(new CoordImpl(1.0, 0.0));
			da4.setFacilityId(new IdImpl(4));

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
			fail("Should not have any exceptions.");
		}
	}
	
	

}
