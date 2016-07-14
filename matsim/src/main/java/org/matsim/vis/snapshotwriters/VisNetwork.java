package org.matsim.vis.snapshotwriters;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.Link;

public interface VisNetwork {

	Map<Id<Link>,? extends VisLink> getVisLinks() ;
	Network getNetwork() ;
}
