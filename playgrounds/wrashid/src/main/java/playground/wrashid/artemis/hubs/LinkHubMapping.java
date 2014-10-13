package playground.wrashid.artemis.hubs;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class LinkHubMapping {
	Matrix matrix;
	LinkedListValueHashMap<Id<Link>, Id<Link>> hubIdLinkIdMapping;
	
	public Collection<Id<Link>> getHubs(){
		return hubIdLinkIdMapping.getKeySet();
	}
	
	public LinkHubMapping(String linkHubMappingTable) {
		matrix = GeneralLib.readStringMatrix(linkHubMappingTable);

		hubIdLinkIdMapping = new LinkedListValueHashMap<>();

		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			String hubId = matrix.getString(i, 0);
			String linkId = matrix.getString(i, 1);
			hubIdLinkIdMapping.putAndSetBackPointer(Id.create(hubId, Link.class), Id.create(linkId, Link.class));
		}
	}

	public Id<Link> getHubIdForLinkId(Id<Link> linkId) {
		return hubIdLinkIdMapping.getKey(linkId);
	}

	public LinkedList<Id<Link>> getLinkIdsForHubId(Id<Link> hubId) {
		return hubIdLinkIdMapping.get(hubId);

	}
}
