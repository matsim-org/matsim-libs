package playground.wrashid.PSF.data;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper.GenerateTableHubLinkMapping;

public class DiverseSubPackageTests extends TestCase {

	public void testEliminateDuplicates(){
		LinkedList<Id<Link>> list=new LinkedList<>();
		
		list.add(Id.create("1", Link.class));
		list.add(Id.create("1", Link.class));
		list.add(Id.create("a", Link.class));
		list.add(Id.create("a", Link.class));
		
		assertEquals(4, list.size());
		
		list=GenerateTableHubLinkMapping.eliminateDuplicates(list);

		assertEquals(2, list.size());
	}
	
}
