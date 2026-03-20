package org.matsim.core.mobsim.dsim;

@FunctionalInterface
public interface NotifyAgentEntersPartition {

	void onAgentEntersPartition(DistributedMobsimAgent agent);
}
