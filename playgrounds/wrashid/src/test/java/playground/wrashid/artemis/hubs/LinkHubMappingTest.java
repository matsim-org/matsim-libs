package playground.wrashid.artemis.hubs;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestUtils;


public class LinkHubMappingTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    @Test
    public final void basicTests() {
        String linkHubMappingFile = utils.getInputDirectory()+ "linkHubMapping.txt";
        
		LinkHubMapping linkHubMapping=new LinkHubMapping(linkHubMappingFile); 
		
		Assert.assertEquals("2",linkHubMapping.getHubIdForLinkId(Id.create("17561380400TF", Link.class)).toString());
		
		Assert.assertEquals(4,linkHubMapping.getLinkIdsForHubId(Id.create("2", Link.class)).size());
		
		Assert.assertEquals(null,linkHubMapping.getHubIdForLinkId(Id.create("non-existing-linkId", Link.class)));
    }

	
}
