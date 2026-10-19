package org.matsim.core.mobsim.qsim;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.network.Link;

public class NoopMessageCollector implements MobsimMessageCollector {
	@Override
	public void collect(Message message, Id<Link> targetLink) {

	}

	@Override
	public void collect(Message message, int targetRank) {

	}

	@Override
	public void send(double now, IntSet neighborPartitions) {

	}

	@Override
	public boolean isLocal(Id<Link> linkId) {
		return true;
	}

	@Override
	public int getPartitionIndex(Id<Link> linkId) {
		return 0;
	}
}
