package org.matsim.contrib.wagonSim.schedule.mapping;

import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser / senozon
 */
public class ReplaceLink implements NetworkEdit {

	private final Id linkId;
	private final List<Id> replacementLinkIds;
	
	public ReplaceLink(final Id linkId, final List<Id> replacementLinkIds) {
		this.linkId = linkId;
		this.replacementLinkIds = replacementLinkIds;
	}
	
	public Id getLinkId() {
		return linkId;
	}
	
	public List<Id> getReplacementLinkIds() {
		return replacementLinkIds;
	}
}
