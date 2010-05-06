package org.matsim.vis.otfvis.opengl.queries;

import java.util.Collection;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

public interface QueryQueueModel {

	Collection<AgentSnapshotInfo> getSnapshot();

}
