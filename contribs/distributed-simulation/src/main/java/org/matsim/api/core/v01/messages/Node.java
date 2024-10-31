package org.matsim.api.core.v01.messages;

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Message;

@Data
@Builder(toBuilder = true)
public class Node implements Message {

    private final int rank;
    private final int cores;
    private final IntList parts;
    private final String hostname;

}
