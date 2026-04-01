package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;

public record CapacityUpdate(Id<Link> linkId, double released, double consumed) implements Message {
}
