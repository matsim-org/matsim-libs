package org.matsim.api.core.v01;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.network.Link;

public interface MobsimMessageCollector {

	void collect(Message message, Id<Link> targetLink);

	void collect(Message message, int targetRank);

	void send(double now, IntSet neighborPartitions);

	boolean isLocal(Id<Link> linkId);

	int getPartitionIndex(Id<Link> linkId);
}
