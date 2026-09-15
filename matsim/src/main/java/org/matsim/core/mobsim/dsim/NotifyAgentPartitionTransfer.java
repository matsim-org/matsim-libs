package org.matsim.core.mobsim.dsim;

public interface NotifyAgentPartitionTransfer {

	void onAgentLeavesPartition(DistributedMobsimAgent agent, int toPartition);

	void onAgentEntersPartition(DistributedMobsimAgent agent);
}
