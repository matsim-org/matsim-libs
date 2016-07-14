package playground.wrashid.artemis.hubs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class Main {

	public static void main(String[] args) {
		String linkHubMappingFile="H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
	
		LinkHubMapping linkHubMapping=new LinkHubMapping(linkHubMappingFile); 
		
		if (!linkHubMapping.getHubIdForLinkId(Id.create("17560001380400TF", Link.class)).toString().equalsIgnoreCase("2")){
			throw new RuntimeException("bad");
		}
		
		//linkHubMapping.getLinkIdsForHubId("2");
		
		//TODO: make tests cases for this.
	}
	
}
