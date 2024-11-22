package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;

public record Teleportation(Class<? extends DistributedMobsimAgent> type, Message agent, double exitTime) {
}
