package org.matsim.dsim.messages;

import lombok.Builder;
import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;

@Builder(setterPrefix = "set")
public record Teleportation(Class<? extends DistributedMobsimAgent> type, Message agent, double exitTime) {
}
