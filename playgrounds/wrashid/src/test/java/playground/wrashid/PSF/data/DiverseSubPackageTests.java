package playground.wrashid.PSF.data;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper.GenerateTableHubLinkMapping;

import junit.framework.TestCase;

public class DiverseSubPackageTests extends TestCase {

	public void testEliminateDuplicates(){
		LinkedList<Id> list=new LinkedList<Id>();
		
		list.add(new IdImpl("1"));
		list.add(new IdImpl("1"));
		list.add(new IdImpl("a"));
		list.add(new IdImpl("a"));
		
		assertEquals(4, list.size());
		
		list=GenerateTableHubLinkMapping.eliminateDuplicates(list);

		assertEquals(2, list.size());
	}
	
}
