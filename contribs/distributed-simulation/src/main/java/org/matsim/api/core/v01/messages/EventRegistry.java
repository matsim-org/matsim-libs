package org.matsim.api.core.v01.messages;


import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Message;

@Data
@Builder
public class EventRegistry implements Message {

    private final int type;
    private final int rank;
    private final IntSet eventTypes;

    private final double syncStep;
}
