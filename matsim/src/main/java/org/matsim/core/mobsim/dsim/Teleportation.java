package org.matsim.core.mobsim.dsim;

public record Teleportation(Class<? extends DistributedMobsimAgent> type, Message agent, double exitTime) {
}
