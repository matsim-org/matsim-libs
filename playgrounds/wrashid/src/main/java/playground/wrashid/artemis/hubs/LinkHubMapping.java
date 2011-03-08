package playground.wrashid.artemis.hubs;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;

public class LinkHubMapping {
	StringMatrix matrix;
	LinkedListValueHashMap<Id, Id> hubIdLinkIdMapping;
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

	public Id getLinkIdForHubId(Id hubId) {
		return hubIdLinkIdMapping.getValue(hubId);

	}
}
