package playground.wrashid.artemis.hubs;

import org.matsim.core.basic.v01.IdImpl;

public class Main {

	public static void main(String[] args) {
		String linkHubMappingFile="H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
	
		LinkHubMapping linkHubMapping=new LinkHubMapping(linkHubMappingFile); 
		
		if (!linkHubMapping.getHubIdForLinkId(new IdImpl("17560001380400TF")).toString().equalsIgnoreCase("2")){
			throw new RuntimeException("bad");
		}
		
		//linkHubMapping.getLinkIdsForHubId("2");
		
		//TODO: make tests cases for this.
	}
	
}
