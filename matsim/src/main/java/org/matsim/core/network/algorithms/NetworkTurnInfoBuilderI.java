package org.matsim.core.network.algorithms;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;

public interface NetworkTurnInfoBuilderI {

	Map<Id<Link>, List<TurnInfo>> createAllowedTurnInfos();

}