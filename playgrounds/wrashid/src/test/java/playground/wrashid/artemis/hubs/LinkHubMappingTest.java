package playground.wrashid.artemis.hubs;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;


public class LinkHubMappingTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    @Test
    public final void basicTests() {
        String linkHubMappingFile = utils.getInputDirectory()+ "linkHubMapping.txt";
        
		LinkHubMapping linkHubMapping=new LinkHubMapping(linkHubMappingFile); 
		
		Assert.assertEquals("2",linkHubMapping.getHubIdForLinkId(new IdImpl("17561380400TF")).toString());
		
		Assert.assertEquals(4,linkHubMapping.getLinkIdsForHubId(new IdImpl("2")).size());
		
		Assert.assertEquals(null,linkHubMapping.getHubIdForLinkId(new IdImpl("non-existing-linkId")));
    }

	
}
