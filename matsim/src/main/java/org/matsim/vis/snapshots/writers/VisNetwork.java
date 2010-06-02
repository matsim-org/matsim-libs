package org.matsim.vis.snapshots.writers;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

public interface VisNetwork {

	Map<Id,? extends VisLink> getVisLinks() ;
	Map<Id,? extends VisNode> getVisNodes() ;
	Network getNetwork() ;
}
