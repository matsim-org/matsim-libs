package org.matsim.vis.otfvis;

import java.util.Collection;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

public interface SimulationViewForQueries {

	Collection<AgentSnapshotInfo> getSnapshot();

}
