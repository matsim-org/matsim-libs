package org.matsim.contrib.wagonSim.schedule.mapping;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author mrieser / senozon
 */
public class ReplaceLink implements NetworkEdit {

	private final Id<Link> linkId;
	private final List<Id<Link>> replacementLinkIds;
	
	public ReplaceLink(final Id<Link> linkId, final List<Id<Link>> replacementLinkIds) {
		this.linkId = linkId;
		this.replacementLinkIds = replacementLinkIds;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public List<Id<Link>> getReplacementLinkIds() {
		return replacementLinkIds;
	}
}
