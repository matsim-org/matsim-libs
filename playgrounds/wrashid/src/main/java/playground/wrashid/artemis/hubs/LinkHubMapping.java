package playground.wrashid.artemis.hubs;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.basic.v01.IdImpl;


public class LinkHubMapping {
	Matrix matrix;
	LinkedListValueHashMap<Id, Id> hubIdLinkIdMapping;
	
	public Collection<Id> getHubs(){
		return hubIdLinkIdMapping.getKeySet();
	}
	
	public LinkHubMapping(String linkHubMappingTable) {
		matrix = GeneralLib.readStringMatrix(linkHubMappingTable);

		hubIdLinkIdMapping = new LinkedListValueHashMap<Id, Id>();

		for (int i = 1; i < matrix.getNumberOfRows(); i++) {
			String hubId = matrix.getString(i, 0);
			String linkId = matrix.getString(i, 1);
			hubIdLinkIdMapping.putAndSetBackPointer(new IdImpl(hubId), new IdImpl(linkId));
		}
	}

	public Id getHubIdForLinkId(Id linkId) {
		return hubIdLinkIdMapping.getKey(linkId);
	}

	public LinkedList<Id> getLinkIdsForHubId(Id hubId) {
		return hubIdLinkIdMapping.get(hubId);

	}
}
