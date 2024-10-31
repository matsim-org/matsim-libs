package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

@Builder(setterPrefix = "set")
@Data
public class CapacityUpdate {

    private final Id<Link> linkId;
    private final double released;
    private final double consumed;

}
