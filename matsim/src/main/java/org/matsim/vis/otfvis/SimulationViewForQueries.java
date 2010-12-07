package org.matsim.vis.otfvis;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

public interface SimulationViewForQueries {

	Collection<AgentSnapshotInfo> getSnapshot();
	
	Map<Id, Plan> getPlans();
	
	Network getNetwork();

}
