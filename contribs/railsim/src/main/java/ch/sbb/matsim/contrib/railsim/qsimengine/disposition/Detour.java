package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;

import java.util.List;

/**
 * Instruction to detour from original route.
 */
public record Detour(int startIdx, int endIdx, List<RailLink> newRoute) {
}
