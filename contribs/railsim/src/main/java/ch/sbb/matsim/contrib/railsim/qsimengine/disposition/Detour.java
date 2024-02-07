package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.List;

/**
 * Instruction to detour from original route.
 */
public record Detour(int startIdx, int endIdx, Id<Link> startLink, Id<Link> endLink, List<RailLink> newRoute) {
}
